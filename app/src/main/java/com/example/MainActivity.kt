package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AuthManager
import com.example.data.AuthManager.authStateFlow
import com.example.data.UltraWrapRepository
import com.example.ui.admin.AdminAppScaffold
import com.example.ui.auth.AuthMode
import com.example.ui.auth.AuthScreen
import com.example.ui.customer.CustomerAppScaffold
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.MutedSilver
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.UltraWrapTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.CustomerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class PortalSession {
    CUSTOMER,
    ADMIN_CHECKING,
    ADMIN
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            UltraWrapTheme {
                UltraWrapRootApp()
            }
        }
    }
}

@Composable
fun UltraWrapRootApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var portalSession by remember { mutableStateOf(PortalSession.CUSTOMER) }
    var authInitialMode by remember { mutableStateOf(AuthMode.CUSTOMER_LOGIN) }
    var adminDeniedMessage by remember { mutableStateOf<String?>(null) }

    var pendingFullName by remember { mutableStateOf("") }
    var pendingMobile by remember { mutableStateOf("") }
    var pendingCompany by remember { mutableStateOf("") }

    // Silent auto sign-in attempt after initial frame sync settles
    LaunchedEffect(Unit) {
        withFrameNanos { }
        delay(250)
        if (auth.currentUser == null) {
            AuthManager.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = { portalSession = PortalSession.CUSTOMER },
                onUnauthenticated = {}
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            initialMode = authInitialMode,
            adminDeniedMessage = adminDeniedMessage,
            onClearAdminDenied = { adminDeniedMessage = null },
            onCustomerSignInSuccess = { _, name, mobile, company ->
                pendingFullName = name
                pendingMobile = mobile
                pendingCompany = company
                adminDeniedMessage = null
                portalSession = PortalSession.CUSTOMER
            },
            onAdminSignInSuccess = {
                adminDeniedMessage = null
                portalSession = PortalSession.ADMIN_CHECKING
            }
        )
        return
    }

    val uid = user.uid
    val email = user.email.orEmpty()
    val displayName = user.displayName.orEmpty()

    if (portalSession == PortalSession.ADMIN_CHECKING) {
        LaunchedEffect(uid) {
            val db = FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
            val repo = UltraWrapRepository(db, auth)
            val isAuthorized = repo.verifyIsAdmin()
            if (isAuthorized) {
                portalSession = PortalSession.ADMIN
            } else {
                adminDeniedMessage =
                    "Access denied: ${email.ifBlank { "This account" }} is not authorized as an Administrator. Sign in with your configured Admin Gmail account."
                authInitialMode = AuthMode.ADMIN_LOGIN
                AuthManager.signOut(credentialManager, scope) {
                    portalSession = PortalSession.CUSTOMER
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxSize().background(ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MetallicGold)
                Spacer(Modifier.height(14.dp))
                Text("Verifying Admin Authorization...", style = MaterialTheme.typography.bodyMedium, color = MutedSilver)
            }
        }
        return
    }

    key(uid, portalSession) {
        if (portalSession == PortalSession.ADMIN) {
            val adminVm: AdminViewModel = viewModel(
                key = "admin_$uid",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                        AdminViewModel(
                            repository = UltraWrapRepository(db),
                            adminUid = uid,
                            adminEmail = email
                        )
                    }
                }
            )
            AdminAppScaffold(
                viewModel = adminVm,
                onLogout = {
                    authInitialMode = AuthMode.ADMIN_LOGIN
                    AuthManager.signOut(credentialManager, scope) {
                        portalSession = PortalSession.CUSTOMER
                    }
                }
            )
        } else {
            val customerVm: CustomerViewModel = viewModel(
                key = "customer_$uid",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                        CustomerViewModel(
                            repository = UltraWrapRepository(db),
                            currentUserId = uid,
                            userEmail = email,
                            userDisplayName = displayName
                        )
                    }
                }
            )
            CustomerAppScaffold(
                viewModel = customerVm,
                pendingFullName = pendingFullName,
                pendingMobile = pendingMobile,
                pendingCompany = pendingCompany,
                onClearPendingSignup = {
                    pendingFullName = ""
                    pendingMobile = ""
                    pendingCompany = ""
                },
                onLogout = {
                    authInitialMode = AuthMode.CUSTOMER_LOGIN
                    AuthManager.signOut(credentialManager, scope) {
                        portalSession = PortalSession.CUSTOMER
                    }
                }
            )
        }
    }
}
