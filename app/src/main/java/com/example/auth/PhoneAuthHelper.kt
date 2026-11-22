package com.example.auth

import android.app.Activity
import android.os.Build
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object PhoneAuthHelper {

    private const val TAG = "PhoneAuth"

    fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)
    }

    fun formatIndianOrE164Phone(rawDigitsOrPlus: String): String? {
        val cleaned = rawDigitsOrPlus.trim().replace(Regex("[\\s\\-()]"), "")
        if (cleaned.isEmpty()) return null
        return when {
            cleaned.startsWith("+") && cleaned.length in 10..16 && cleaned.drop(1).all(Char::isDigit) -> cleaned
            cleaned.length == 10 && cleaned.all(Char::isDigit) -> "+91$cleaned"
            cleaned.length == 12 && cleaned.startsWith("91") && cleaned.all(Char::isDigit) -> "+$cleaned"
            else -> null
        }
    }

    fun sendOtp(
        activity: Activity,
        e164Phone: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onAutoVerified: (PhoneAuthCredential) -> Unit,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = Firebase.auth
        // On emulators without Play Store Play Integrity binding (-9), skip Play Integrity
        // and use reCAPTCHA / test-number flow directly.
        if (isProbablyEmulator()) {
            try {
                auth.firebaseAuthSettings.forceRecaptchaFlowForTesting(true)
            } catch (e: Exception) {
                Log.w(TAG, "Could not configure forceRecaptchaFlowForTesting: ${e.message}")
            }
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Phone verification auto-completed")
                onAutoVerified(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "Phone verification failed: ${e.message}", e)
                val msg = e.message.orEmpty()
                val errorCode = (e as? FirebaseAuthException)?.errorCode.orEmpty()
                val friendly = when {
                    e is FirebaseAuthInvalidCredentialsException ->
                        "Please enter a valid 10-digit mobile number (e.g. +91 9876543210)."
                    e is FirebaseTooManyRequestsException ->
                        "Too many OTP attempts from this device. Please wait a few minutes and try again."
                    e is FirebaseAuthMissingActivityForRecaptchaException ||
                        errorCode in setOf(
                            "ERROR_OPERATION_NOT_ALLOWED",
                            "ERROR_APP_NOT_AUTHORIZED",
                            "ERROR_CAPTCHA_CHECK_FAILED",
                            "ERROR_MISSING_CLIENT_IDENTIFIER"
                        ) ||
                        msg.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                        msg.contains("not enabled", ignoreCase = true) ||
                        msg.contains("reCAPTCHA", ignoreCase = true) ||
                        msg.contains("Recaptcha", ignoreCase = true) ||
                        msg.contains("Integrity", ignoreCase = true) ||
                        msg.contains("CANNOT_BIND_TO_SERVICE", ignoreCase = true) ||
                        msg.contains("not authorized", ignoreCase = true) ->
                        "Mobile OTP requires enabling Phone Auth (and Test Numbers or reCAPTCHA/Play Integrity) in Firebase Console → Authentication → Sign-in method → Phone. You can also log in instantly with Google below."
                    else -> e.localizedMessage ?: "Failed to send OTP. Please try again or use Google Login below."
                }
                onError(friendly)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "OTP code sent successfully")
                onCodeSent(verificationId, token)
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(e164Phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (resendToken != null) {
            builder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    suspend fun signInWithOtp(verificationId: String, otpCode: String): Result<Unit> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, otpCode.trim())
            Firebase.auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(IllegalArgumentException("Incorrect OTP. Please check the 6-digit code and try again."))
        } catch (e: Exception) {
            Log.w(TAG, "OTP verification failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<Unit> {
        return try {
            Firebase.auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Auto phone sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
