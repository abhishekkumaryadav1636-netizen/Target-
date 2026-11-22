package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.auth.PhoneAuthHelper
import com.example.ui.theme.SaffronAccent
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var phoneInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var sentPhoneDisplay by remember { mutableStateOf("") }
    var resendSecondsLeft by remember { mutableIntStateOf(0) }

    var isOtpSending by remember { mutableStateOf(false) }
    var isOtpVerifying by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    // Silent auto sign-in check on startup for previously authorized Google sessions
    LaunchedEffect(Unit) {
        GoogleAuthHelper.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    // Countdown timer for Resend OTP
    LaunchedEffect(resendSecondsLeft) {
        if (resendSecondsLeft > 0) {
            delay(1000L)
            resendSecondsLeft -= 1
        }
    }

    val anyBusy = isOtpSending || isOtpVerifying || isGoogleLoading

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E1742),
                        Color(0xFF1A237E),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = "Target Bootcamp learning banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x330B1120), Color(0xCC0B1120))
                            )
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SaffronAccent
                        ) {
                            Text(
                                text = "BATCHES 2026-27 LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.auth_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.mobile_login_title),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.mobile_login_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("UP Board 9-12", "Bihar Board 9-12", "CUET UG 2026").forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 1: Mobile Number Input
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '+' }.take(14)
                            phoneInput = filtered
                            errorMessage = null
                        },
                        label = { Text(stringResource(R.string.mobile_number_label)) },
                        placeholder = { Text("9876543210") },
                        prefix = {
                            if (!phoneInput.startsWith("+")) {
                                Text("+91 ", fontWeight = FontWeight.Bold)
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = "Mobile Number"
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        enabled = !anyBusy && verificationId == null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mobile_number_input")
                    )

                    if (verificationId == null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                val e164 = PhoneAuthHelper.formatIndianOrE164Phone(phoneInput)
                                val activity = context as? Activity
                                if (e164 == null) {
                                    errorMessage = "Please enter a valid 10-digit mobile number."
                                    return@Button
                                }
                                if (activity == null) {
                                    errorMessage = "Unable to start OTP verification."
                                    return@Button
                                }
                                isOtpSending = true
                                sentPhoneDisplay = e164
                                PhoneAuthHelper.sendOtp(
                                    activity = activity,
                                    e164Phone = e164,
                                    resendToken = null,
                                    onAutoVerified = { cred ->
                                        scope.launch {
                                            val res = PhoneAuthHelper.signInWithPhoneCredential(cred)
                                            isOtpSending = false
                                            res.onSuccess { onAuthSuccess() }
                                                .onFailure { err ->
                                                    errorMessage = err.localizedMessage ?: "OTP login failed"
                                                }
                                        }
                                    },
                                    onCodeSent = { verId, token ->
                                        isOtpSending = false
                                        verificationId = verId
                                        resendToken = token
                                        resendSecondsLeft = 30
                                        infoMessage = "OTP sent to $e164"
                                    },
                                    onError = { msg ->
                                        isOtpSending = false
                                        errorMessage = msg
                                    }
                                )
                            },
                            enabled = !anyBusy,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("send_otp_button")
                        ) {
                            if (isOtpSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = stringResource(R.string.send_otp),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Step 2: 6-Digit OTP Verification
                    AnimatedVisibility(visible = verificationId != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { raw ->
                                    otpInput = raw.filter(Char::isDigit).take(6)
                                    errorMessage = null
                                },
                                label = { Text(stringResource(R.string.enter_otp_label)) },
                                placeholder = { Text("6-digit OTP") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = "OTP Code"
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                enabled = !anyBusy,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("otp_code_input")
                            )

                            Button(
                                onClick = {
                                    val verId = verificationId ?: return@Button
                                    if (otpInput.length < 6) {
                                        errorMessage = "Please enter the 6-digit OTP."
                                        return@Button
                                    }
                                    errorMessage = null
                                    isOtpVerifying = true
                                    scope.launch {
                                        val res = PhoneAuthHelper.signInWithOtp(verId, otpInput)
                                        isOtpVerifying = false
                                        res.onSuccess { onAuthSuccess() }
                                            .onFailure { err ->
                                                errorMessage = err.localizedMessage ?: "Invalid OTP"
                                            }
                                    }
                                },
                                enabled = !anyBusy && otpInput.length == 6,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("verify_otp_button")
                            ) {
                                if (isOtpVerifying) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.verify_and_login),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        verificationId = null
                                        otpInput = ""
                                        infoMessage = null
                                        errorMessage = null
                                    },
                                    enabled = !anyBusy
                                ) {
                                    Text("Change Number")
                                }

                                TextButton(
                                    onClick = {
                                        val activity = context as? Activity ?: return@TextButton
                                        if (sentPhoneDisplay.isBlank()) return@TextButton
                                        errorMessage = null
                                        isOtpSending = true
                                        PhoneAuthHelper.sendOtp(
                                            activity = activity,
                                            e164Phone = sentPhoneDisplay,
                                            resendToken = resendToken,
                                            onAutoVerified = { cred ->
                                                scope.launch {
                                                    val res = PhoneAuthHelper.signInWithPhoneCredential(cred)
                                                    isOtpSending = false
                                                    res.onSuccess { onAuthSuccess() }
                                                        .onFailure { err ->
                                                            errorMessage = err.localizedMessage ?: "OTP login failed"
                                                        }
                                                }
                                            },
                                            onCodeSent = { verId, token ->
                                                isOtpSending = false
                                                verificationId = verId
                                                resendToken = token
                                                resendSecondsLeft = 30
                                                infoMessage = "New OTP sent to $sentPhoneDisplay"
                                            },
                                            onError = { msg ->
                                                isOtpSending = false
                                                errorMessage = msg
                                            }
                                        )
                                    },
                                    enabled = !anyBusy && resendSecondsLeft == 0
                                ) {
                                    Text(
                                        if (resendSecondsLeft > 0) "Resend in ${resendSecondsLeft}s"
                                        else "Resend OTP"
                                    )
                                }
                            }
                        }
                    }

                    if (!infoMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = infoMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                TextButton(
                                    onClick = {
                                        errorMessage = null
                                        isGoogleLoading = true
                                        GoogleAuthHelper.onGoogleSignInClicked(
                                            context = context,
                                            credentialManager = credentialManager,
                                            onAuthSuccess = {
                                                isGoogleLoading = false
                                                onAuthSuccess()
                                            },
                                            onAuthError = { msg ->
                                                isGoogleLoading = false
                                                errorMessage = msg
                                            },
                                            scope = scope,
                                            onAuthCancelled = {
                                                isGoogleLoading = false
                                            }
                                        )
                                    },
                                    enabled = !anyBusy
                                ) {
                                    Text("Login with Google Instead")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            isGoogleLoading = true
                            GoogleAuthHelper.onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isGoogleLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isGoogleLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isGoogleLoading = false
                                }
                            )
                        },
                        enabled = !anyBusy,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.sign_in_with_google),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = SaffronAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.auth_sync_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
