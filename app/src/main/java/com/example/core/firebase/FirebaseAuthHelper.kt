package com.example.core.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val isAnonymous: Boolean = false,
    val isSynced: Boolean = false
)

class FirebaseAuthHelper(private val context: Context) {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val isFirebaseAvailable: Boolean by lazy {
        try {
            FirebaseApp.initializeApp(context) != null || FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private val auth: FirebaseAuth? by lazy {
        if (isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    init {
        checkCurrentAuth()
    }

    private fun checkCurrentAuth() {
        val user = auth?.currentUser
        if (user != null) {
            _currentUser.value = UserProfile(
                uid = user.uid,
                email = user.email ?: "developer@nova-ai.local",
                displayName = user.displayName ?: "Nova Developer"
            )
        } else {
            // Default local offline developer profile
            _currentUser.value = UserProfile(
                uid = "local_dev_user",
                email = "developer@nova-ai.local",
                displayName = "Local Developer",
                isAnonymous = true,
                isSynced = false
            )
        }
    }

    fun isConfigured(): Boolean = auth != null

    suspend fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            // Fallback for local offline mode
            _currentUser.value = UserProfile(
                uid = "offline_${System.currentTimeMillis()}",
                email = email,
                displayName = email.substringBefore('@').replaceFirstChar { it.uppercase() }
            )
            onResult(true, "Signed in locally (Offline Mode). Configure Firebase Console for cloud sync.")
            return
        }

        firebaseAuth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val u = result.user
                if (u != null) {
                    _currentUser.value = UserProfile(
                        uid = u.uid,
                        email = u.email ?: email,
                        displayName = u.displayName ?: email.substringBefore('@')
                    )
                }
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    suspend fun signUpWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _currentUser.value = UserProfile(
                uid = "offline_${System.currentTimeMillis()}",
                email = email,
                displayName = email.substringBefore('@').replaceFirstChar { it.uppercase() }
            )
            onResult(true, "Account created in local storage. Connect Firebase for multi-device sync.")
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val u = result.user
                if (u != null) {
                    _currentUser.value = UserProfile(
                        uid = u.uid,
                        email = u.email ?: email,
                        displayName = u.displayName ?: email.substringBefore('@')
                    )
                }
                onResult(true, null)
            }
            .addOnFailureListener { e ->
                onResult(false, e.localizedMessage)
            }
    }

    fun signOut() {
        auth?.signOut()
        _currentUser.value = UserProfile(
            uid = "local_dev_user",
            email = "developer@nova-ai.local",
            displayName = "Local Developer",
            isAnonymous = true
        )
    }
}
