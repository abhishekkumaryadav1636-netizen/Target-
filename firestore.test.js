const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_abhishek";
const ADMIN_EMAIL = "abhishekkumaryadav1636@gmail.com";
const ALICE_UID = "student_alice";
const BOB_UID = "student_bob";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validCoursePayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    title: "Class 10 UP Board",
    subtitle: "Board Toppers 95+ Batch • Complete Syllabus",
    board: "UP Board",
    className: "Class 10",
    batchYear: "2026-27",
    category: "UP Board",
    priceInr: 499,
    originalPriceInr: 1999,
    freeBatch: false,
    thumbnailUrl: "preset://thumb_up_class_10",
    fallbackKey: "up_class_10",
    instructorName: "Target Bootcamp Faculty",
    subjectsSummary: "Science, Maths, SST, English, Hindi",
    lessonCount: 180,
    durationLabel: "12 Months",
    sortOrder: 2,
    catalogActive: true,
    updatedBy: uid,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write courses or user data", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("courses").where("catalogActive", "==", true).get()
  );
  await assertFails(unauthDb.collection("courses").doc("up_class_10").get());
  await assertFails(
    unauthDb
      .collection("courses")
      .doc("up_class_10")
      .set(validCoursePayload(ADMIN_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Verified admin can create, update thumbnailUrl, and delete courses; spoofed email is rejected", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();

  const spoofedDb = testEnv
    .authenticatedContext("spoof_user", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  // Spoofed unverified email cannot create course
  await assertFails(
    spoofedDb
      .collection("courses")
      .doc("up_class_10")
      .set(validCoursePayload("spoof_user"))
  );

  // Verified admin creates course (no @DocumentId 'id' field)
  await assertSucceeds(
    adminDb
      .collection("courses")
      .doc("up_class_10")
      .set(validCoursePayload(ADMIN_UID))
  );

  // Admin replaces thumbnailUrl with compact data URI / HTTPS URL while preserving createdAt
  await assertSucceeds(
    adminDb
      .collection("courses")
      .doc("up_class_10")
      .update({
        thumbnailUrl: "https://example.com/posters/up_class_10_v2.jpg",
        updatedBy: ADMIN_UID,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Shadow update with ghost field or invalid @DocumentId 'id' fails
  await assertFails(
    adminDb
      .collection("courses")
      .doc("up_class_10")
      .update({
        thumbnailUrl: "https://example.com/ok.jpg",
        ghostField: true,
        updatedBy: ADMIN_UID,
        updatedAt: new Date(Date.now() - 500),
      })
  );
  await assertFails(
    adminDb
      .collection("courses")
      .doc("up_class_9")
      .set(validCoursePayload(ADMIN_UID, { id: "up_class_9" }))
  );

  // Admin can delete course
  await assertSucceeds(adminDb.collection("courses").doc("up_class_10").delete());
});

test("Students can list active courses with matching query, cannot run unfiltered list, and cannot modify courses", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("courses")
      .doc("up_class_10")
      .set(validCoursePayload(ADMIN_UID, { catalogActive: true }));
    await db
      .collection("courses")
      .doc("hidden_draft")
      .set(validCoursePayload(ADMIN_UID, { catalogActive: false }));
  });

  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  // Aligned client query succeeds
  await assertSucceeds(
    aliceDb.collection("courses").where("catalogActive", "==", true).get()
  );

  // Unfiltered query rejected
  await assertFails(aliceDb.collection("courses").get());

  // Inactive course rejected for student
  await assertFails(aliceDb.collection("courses").doc("hidden_draft").get());

  // Student cannot update or create courses or promote self in /admins
  await assertFails(
    aliceDb
      .collection("courses")
      .doc("up_class_10")
      .update({
        thumbnailUrl: "https://evil.example/hijack.jpg",
        updatedBy: ALICE_UID,
        updatedAt: new Date(Date.now() - 500),
      })
  );
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({ role: "admin" })
  );
});

test("Per-user profile, favorites, and enrollments enforce strict owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Alice creates profile (with phoneNumber), favorite, enrollment
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "+919876543210",
      phoneNumber: "+919876543210",
      preferredBoard: "UP Board",
      preferredClass: "Class 10",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("up_class_10")
      .set({
        courseId: "up_class_10",
        addedAt: now,
      })
  );
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("enrollments")
      .doc("up_class_10")
      .set({
        courseId: "up_class_10",
        courseTitle: "Class 10 UP Board",
        progressPercent: 10,
        enrolledAt: now,
        updatedAt: now,
      })
  );

  // Alice lists her own favorites & enrollments
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("enrollments").get()
  );

  // Bob cannot read or write Alice's profile, favorites, or enrollments
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("favorites").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("favorites")
      .doc("up_class_10")
      .delete()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("enrollments")
      .doc("up_class_10")
      .get()
  );
});
