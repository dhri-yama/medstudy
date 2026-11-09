package com.medstudy.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom

/** What Google gave back: the ID token to hand to Supabase and the raw nonce it was bound to. */
class GoogleToken(val idToken: String, val rawNonce: String, val displayName: String?)

object GoogleSignIn {
    val isConfigured get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Shows Google's account chooser. Returns null if the learner dismissed it; throws [AuthException] with a readable message otherwise. */
    suspend fun request(context: Context): GoogleToken? {
        if (!isConfigured) throw AuthException("Google sign-in isn't set up in this build yet.")
        val activity = context.findActivity() ?: throw AuthException("Couldn't open Google sign-in.")
        val rawNonce = ByteArray(16).also(SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }
        val hashedNonce = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray()).joinToString("") { "%02x".format(it) }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setNonce(hashedNonce)
            .build()
        return try {
            val result = CredentialManager.create(activity).getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleToken(google.idToken, rawNonce, google.displayName)
            } else throw AuthException("Google sign-in returned something unexpected.")
        } catch (e: GetCredentialCancellationException) {
            null
        } catch (e: GetCredentialException) {
            throw AuthException("Google sign-in didn't work: ${e.message ?: e.type}")
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
