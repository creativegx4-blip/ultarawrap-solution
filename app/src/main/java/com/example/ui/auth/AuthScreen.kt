package com.example.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.BuildConfig
import com.example.data.AuthManager
import com.example.ui.components.GoldPrimaryButton
import com.example.ui.components.UltraWrapLogoBadge
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.CrimsonSurface
import com.example.ui.theme.GoldTintSurface
import com.example.ui.theme.IvoryText
import com.example.ui.theme.LightGold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RichGold
import com.google.firebase.auth.FirebaseUser

enum class AuthMode {
    CUSTOMER_LOGIN,
    CUSTOMER_SIGN_UP,
    ADMIN_LOGIN
}

@Composable
fun AuthScreen(
    initialMode: AuthMode = AuthMode.CUSTOMER_LOGIN,
    adminDeniedMessage: String? = null,
    onClearAdminDenied: () -> Unit = {},
    onCustomerSignInSuccess: (user: FirebaseUser, pendingFullName: String, pendingMobile: String, pendingCompany: String) -> Unit,
    onAdminSignInSuccess: (user: FirebaseUser) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var mode by remember { mutableStateOf(initialMode) }
    var fullName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var adminEmailHint by remember { mutableStateOf(BuildConfig.ADMIN_EMAIL.trim()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val activeError = adminDeniedMessage ?: errorMessage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianBlack,
                        Color(0xFF15120A),
                        ObsidianBlack
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (mode == AuthMode.ADMIN_LOGIN) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                mode = AuthMode.CUSTOMER_LOGIN
                                errorMessage = null
                                onClearAdminDenied()
                            },
                            modifier = Modifier.testTag("back_to_customer_auth")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Customer Login",
                                tint = LightGold
                            )
                        }
                        Text(
                            text = "Customer Portal",
                            style = MaterialTheme.typography.labelLarge,
                            color = LightGold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                UltraWrapLogoBadge(size = 88.dp)
                Spacer(Modifier.height(16.dp))

                Text(
                    text = "ULTRAWRAP SOLUTION",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Wrap Smarter. Grow Stronger.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                    color = MetallicGold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                if (mode != AuthMode.ADMIN_LOGIN) {
                    // Login / Sign Up segmented toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ObsidianCard)
                            .border(1.dp, ObsidianOutline, RoundedCornerShape(14.dp))
                            .padding(4.dp)
                    ) {
                        val isLogin = mode == AuthMode.CUSTOMER_LOGIN
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isLogin) MetallicGold else Color.Transparent)
                                .clickable {
                                    mode = AuthMode.CUSTOMER_LOGIN
                                    errorMessage = null
                                    onClearAdminDenied()
                                }
                                .padding(vertical = 12.dp)
                                .testTag("tab_customer_login"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Login",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isLogin) ObsidianBlack else MutedSilver,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isLogin) MetallicGold else Color.Transparent)
                                .clickable {
                                    mode = AuthMode.CUSTOMER_SIGN_UP
                                    errorMessage = null
                                    onClearAdminDenied()
                                }
                                .padding(vertical = 12.dp)
                                .testTag("tab_customer_signup"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sign Up",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (!isLogin) ObsidianBlack else MutedSilver,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }

                // Main Auth Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(
                        1.dp,
                        if (mode == AuthMode.ADMIN_LOGIN) MetallicGold.copy(alpha = 0.6f) else ObsidianOutline
                    )
                ) {
                    AnimatedContent(targetState = mode, label = "auth_mode") { currentMode ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (currentMode) {
                                AuthMode.CUSTOMER_LOGIN -> {
                                    Text(
                                        text = "Customer Sign In",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = PureWhite
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Verify your email securely with Google Sign-In to browse industrial packaging & place orders.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedSilver,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(18.dp))

                                    VerifiedAuthPill(text = "Verified Email Session Required")
                                    Spacer(Modifier.height(18.dp))

                                    GoogleAuthCtaButton(
                                        label = "Sign in with Google",
                                        isLoading = isLoading,
                                        testTag = "customer_google_signin_button",
                                        onClick = {
                                            errorMessage = null
                                            onClearAdminDenied()
                                            isLoading = true
                                            AuthManager.onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onAuthSuccess = { user ->
                                                    isLoading = false
                                                    onCustomerSignInSuccess(user, "", "", "")
                                                },
                                                onAuthError = { msg ->
                                                    isLoading = false
                                                    errorMessage = msg
                                                },
                                                onAuthCancelled = { isLoading = false }
                                            )
                                        }
                                    )
                                }

                                AuthMode.CUSTOMER_SIGN_UP -> {
                                    Text(
                                        text = "Create Customer Account",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = PureWhite
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Enter your business contact details and verify with your Google email.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedSilver,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(16.dp))

                                    val fieldColors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MetallicGold,
                                        unfocusedBorderColor = ObsidianOutline,
                                        focusedLabelColor = LightGold,
                                        unfocusedLabelColor = MutedSilver,
                                        focusedTextColor = PureWhite,
                                        unfocusedTextColor = IvoryText
                                    )

                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it },
                                        label = { Text("Full Name *") },
                                        leadingIcon = { Icon(Icons.Default.Person, null, tint = MetallicGold) },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("signup_fullname_input")
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = mobileNumber,
                                        onValueChange = { mobileNumber = it },
                                        label = { Text("Mobile Number *") },
                                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = MetallicGold) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("signup_mobile_input")
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = companyName,
                                        onValueChange = { companyName = it },
                                        label = { Text("Company / Business Name (Optional)") },
                                        leadingIcon = { Icon(Icons.Default.Business, null, tint = MetallicGold) },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("signup_company_input")
                                    )
                                    Spacer(Modifier.height(18.dp))

                                    GoogleAuthCtaButton(
                                        label = "Verify Email & Create Account",
                                        isLoading = isLoading,
                                        testTag = "customer_signup_google_button",
                                        onClick = {
                                            if (fullName.isBlank() || mobileNumber.isBlank()) {
                                                errorMessage = "Please enter your Full Name and Mobile Number first."
                                                return@GoogleAuthCtaButton
                                            }
                                            errorMessage = null
                                            onClearAdminDenied()
                                            isLoading = true
                                            AuthManager.onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onAuthSuccess = { user ->
                                                    isLoading = false
                                                    onCustomerSignInSuccess(
                                                        user,
                                                        fullName.trim(),
                                                        mobileNumber.trim(),
                                                        companyName.trim()
                                                    )
                                                },
                                                onAuthError = { msg ->
                                                    isLoading = false
                                                    errorMessage = msg
                                                },
                                                onAuthCancelled = { isLoading = false }
                                            )
                                        }
                                    )
                                }

                                AuthMode.ADMIN_LOGIN -> {
                                    Surface(
                                        color = GoldTintSurface,
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = LightGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "ADMIN PANEL",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = LightGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Authorized Administrator Login",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = PureWhite,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Admin access is restricted to verified administrator Gmail accounts only. No public admin registration.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedSilver,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(16.dp))

                                    OutlinedTextField(
                                        value = adminEmailHint,
                                        onValueChange = { adminEmailHint = it },
                                        label = { Text("Authorized Admin Gmail") },
                                        placeholder = { Text("Configured via ADMIN_EMAIL or Admin Registry") },
                                        leadingIcon = { Icon(Icons.Default.Email, null, tint = MetallicGold) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MetallicGold,
                                            unfocusedBorderColor = ObsidianOutline,
                                            focusedLabelColor = LightGold,
                                            unfocusedLabelColor = MutedSilver,
                                            focusedTextColor = PureWhite,
                                            unfocusedTextColor = IvoryText
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_email_input")
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = RichGold,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Server-enforced Firestore role verification active",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedSilver
                                        )
                                    }
                                    Spacer(Modifier.height(18.dp))

                                    GoogleAuthCtaButton(
                                        label = "Sign in as Admin with Google",
                                        isLoading = isLoading,
                                        testTag = "admin_google_signin_button",
                                        onClick = {
                                            errorMessage = null
                                            onClearAdminDenied()
                                            isLoading = true
                                            AuthManager.onGoogleSignInClicked(
                                                context = context,
                                                credentialManager = credentialManager,
                                                scope = scope,
                                                onAuthSuccess = { user ->
                                                    isLoading = false
                                                    onAdminSignInSuccess(user)
                                                },
                                                onAuthError = { msg ->
                                                    isLoading = false
                                                    errorMessage = msg
                                                },
                                                onAuthCancelled = { isLoading = false }
                                            )
                                        }
                                    )
                                }
                            }

                            if (!activeError.isNullOrBlank()) {
                                Spacer(Modifier.height(14.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = CrimsonSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CrimsonError.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = activeError,
                                        color = PureWhite,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (mode != AuthMode.ADMIN_LOGIN) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                mode = AuthMode.ADMIN_LOGIN
                                errorMessage = null
                                onClearAdminDenied()
                            }
                            .testTag("open_admin_login_button"),
                        color = ObsidianCard,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ObsidianOutline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel Login",
                                tint = MetallicGold
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Admin Panel Login",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = LightGold
                                )
                                Text(
                                    text = "Manage products, orders, direct book billing & reports",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedSilver
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "UltraWrap Solution • Industrial Packaging & Direct Billing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSilver,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CompleteProfileDialogScreen(
    email: String,
    defaultName: String,
    isBusy: Boolean,
    onSave: (fullName: String, mobileNumber: String, companyName: String) -> Unit,
    onSignOut: () -> Unit
) {
    var fullName by remember { mutableStateOf(defaultName) }
    var mobileNumber by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            color = ObsidianSurface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UltraWrapLogoBadge(size = 64.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Complete Customer Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = PureWhite
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Signed in as $email",
                    style = MaterialTheme.typography.bodySmall,
                    color = MetallicGold
                )
                Spacer(Modifier.height(18.dp))

                val colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MetallicGold,
                    unfocusedBorderColor = ObsidianOutline,
                    focusedLabelColor = LightGold,
                    unfocusedLabelColor = MutedSilver,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = IvoryText
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().testTag("complete_profile_name")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().testTag("complete_profile_mobile")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name (Optional)") },
                    singleLine = true,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().testTag("complete_profile_company")
                )

                if (!errorText.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(text = errorText!!, color = CrimsonError, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(18.dp))
                GoldPrimaryButton(
                    text = if (isBusy) "Saving..." else "Continue to Store",
                    enabled = !isBusy,
                    testTag = "complete_profile_submit",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (fullName.isBlank() || mobileNumber.length < 7) {
                            errorText = "Please enter your full name and a valid mobile number."
                            return@GoldPrimaryButton
                        }
                        errorText = null
                        onSave(fullName, mobileNumber, companyName)
                    }
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onSignOut) {
                    Text("Switch Account / Sign Out", color = MutedSilver)
                }
            }
        }
    }
}

@Composable
private fun VerifiedAuthPill(text: String) {
    Surface(
        color = ObsidianElevated,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, ObsidianOutline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = MetallicGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = IvoryText
            )
        }
    }
}

@Composable
private fun GoogleAuthCtaButton(
    label: String,
    isLoading: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MetallicGold,
            contentColor = ObsidianBlack,
            disabledContainerColor = ObsidianElevated
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = ObsidianBlack,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
