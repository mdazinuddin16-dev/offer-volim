package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.auth.PendingRegistration
import com.example.auth.PendingRegistrationStore
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.LocalOfferSemanticPalette

private enum class AuthEntryMode(val label: String) {
    LOGIN("লগইন"),
    REGISTER("রেজিস্ট্রেশন")
}

private val BangladeshDivisions = listOf(
    "All BD",
    "Dhaka",
    "Barishal",
    "Chattogram",
    "Rajshahi",
    "Khulna",
    "Sylhet",
    "Rangpur",
    "Mymensingh"
)

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val semantics = LocalOfferSemanticPalette.current

    var entryMode by remember { mutableStateOf(AuthEntryMode.LOGIN) }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var division by remember { mutableStateOf("All BD") }

    var isSigningIn by remember { mutableStateOf(false) }
    var isCheckingExisting by remember { mutableStateOf(true) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingExisting = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingExisting = false
            },
            scope = scope
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.extraLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_deals_banner),
                            contentDescription = "OfferVault Drive Deals Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(132.dp)
                                .clip(MaterialTheme.shapes.large)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            AuthEntryMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = entryMode == mode,
                                    onClick = {
                                        entryMode = mode
                                        authError = null
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = AuthEntryMode.entries.size
                                    ),
                                    modifier = Modifier.testTag("auth_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(mode.label, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (entryMode == AuthEntryMode.LOGIN) {
                            Surface(
                                color = semantics.savingsContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = semantics.onSavingsContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "আগে থেকে আইডি থাকলে লগইন করুন",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = semantics.onSavingsContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "আপনার অ্যাকাউন্ট আগে থেকেই রেজিস্টার করা থাকলে সরাসরি লগইন করে অফার ও অ্যাডমিন প্যানেলে প্রবেশ করুন। নতুন হলে রেজিস্ট্রেশন ট্যাব ব্যবহার করুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "নতুন ইউজার রেজিস্ট্রেশন",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "প্রথমবার প্রবেশের জন্য আপনার নাম, মোবাইল ও বিভাগ দিয়ে আইডি তৈরি করুন।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = {
                                        fullName = it.take(80)
                                        authError = null
                                    },
                                    label = { Text("আপনার পুরো নাম") },
                                    placeholder = { Text("যেমন: Md Azinuddin") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_name_input")
                                )

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { input ->
                                        phone = input.filter { it.isDigit() || it == '+' }.take(20)
                                        authError = null
                                    },
                                    label = { Text("মোবাইল নম্বর") },
                                    placeholder = { Text("017XXXXXXXX") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("register_phone_input")
                                )

                                Text(
                                    text = "আপনার বিভাগ",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(BangladeshDivisions) { item ->
                                        FilterChip(
                                            selected = division == item,
                                            onClick = { division = item },
                                            label = { Text(item) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        )
                                    }
                                }
                            }
                        }

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = authError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                authError = null
                                if (entryMode == AuthEntryMode.REGISTER) {
                                    val cleanName = fullName.trim()
                                    val cleanPhone = phone.trim()
                                    if (cleanName.isEmpty() || cleanPhone.isEmpty()) {
                                        authError = "সবগুলো ঘর পূরণ করুন!"
                                        return@Button
                                    }
                                    if (cleanName.length < 2) {
                                        authError = "নাম কমপক্ষে ২ অক্ষরের হতে হবে।"
                                        return@Button
                                    }
                                    if (cleanPhone.length < 6) {
                                        authError = "সঠিক মোবাইল নম্বর লিখুন।"
                                        return@Button
                                    }
                                    PendingRegistrationStore.set(
                                        PendingRegistration(
                                            fullName = cleanName,
                                            phone = cleanPhone,
                                            division = division
                                        )
                                    )
                                } else {
                                    PendingRegistrationStore.clear()
                                }

                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isSigningIn = false
                                        authError = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    }
                                )
                            },
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isSigningIn || isCheckingExisting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isSigningIn) "যাচাই হচ্ছে…" else "অ্যাকাউন্ট চেক হচ্ছে…",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            } else {
                                Icon(
                                    imageVector = if (entryMode == AuthEntryMode.LOGIN) {
                                        Icons.AutoMirrored.Rounded.Login
                                    } else {
                                        Icons.Rounded.PersonAdd
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (entryMode == AuthEntryMode.LOGIN) {
                                        "Sign in with Google (লগইন)"
                                    } else {
                                        "রেজিস্ট্রেশন ও অ্যাকাউন্ট তৈরি করুন"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                entryMode = if (entryMode == AuthEntryMode.LOGIN) {
                                    AuthEntryMode.REGISTER
                                } else {
                                    AuthEntryMode.LOGIN
                                }
                                authError = null
                            }
                        ) {
                            Text(
                                text = if (entryMode == AuthEntryMode.LOGIN) {
                                    "নতুন ইউজার? এখানে রেজিস্ট্রেশন করুন"
                                } else {
                                    "আগে থেকে আইডি আছে? লগইন করুন"
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Secured by Firebase Auth & Firestore Rules",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CompleteRegistrationScreen(
    initialName: String,
    email: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmitRegistration: (fullName: String, phone: String, division: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantics = LocalOfferSemanticPalette.current
    var fullName by remember(initialName) { mutableStateOf(initialName) }
    var phone by remember { mutableStateOf("") }
    var division by remember { mutableStateOf("All BD") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .border(1.dp, semantics.cardBorder, MaterialTheme.shapes.extraLarge)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = semantics.savingsContainer,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HowToReg,
                                contentDescription = null,
                                tint = semantics.onSavingsContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "নতুন ইউজার রেজিস্ট্রেশন",
                                style = MaterialTheme.typography.labelMedium,
                                color = semantics.onSavingsContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "আপনার অ্যাকাউন্ট তৈরি করুন",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (email.isNotBlank()) {
                            "এই অ্যাকাউন্ট ($email) এখনো রেজিস্টার করা হয়নি। নিচের তথ্য দিয়ে রেজিস্ট্রেশন সম্পন্ন করুন।"
                        } else {
                            "নতুন অ্যাকাউন্ট তৈরির জন্য নিচের তথ্য পূরণ করুন।"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it.take(80) },
                        label = { Text("আপনার পুরো নাম") },
                        placeholder = { Text("যেমন: Md Azinuddin") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complete_reg_name_input")
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { input -> phone = input.filter { it.isDigit() || it == '+' }.take(20) },
                        label = { Text("মোবাইল নম্বর") },
                        placeholder = { Text("017XXXXXXXX") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("complete_reg_phone_input")
                    )

                    Text(
                        text = "আপনার বিভাগ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BangladeshDivisions) { item ->
                            FilterChip(
                                selected = division == item,
                                onClick = { division = item },
                                label = { Text(item) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { onSubmitRegistration(fullName, phone, division) },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("complete_reg_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("রেজিস্ট্রেশন হচ্ছে…")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "রেজিস্ট্রেশন সম্পন্ন করুন",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("complete_reg_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("অন্য অ্যাকাউন্ট ব্যবহার করুন")
                    }
                }
            }
        }
    }
}
