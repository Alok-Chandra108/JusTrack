package com.alok.justrack.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    /**
     * Observable stream of user info.
     */
    val currentUser: Flow<UserInfo?> = supabase.auth.sessionStatus
        .map { status ->
            supabase.auth.currentUserOrNull()
        }

    /**
     * Sign in with Email and Password.
     */
    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Sign up with Email and Password.
     */
    suspend fun signUp(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Sign out.
     */
    suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }

    /**
     * Get current user ID.
     */
    fun getUserId(): String? = supabase.auth.currentUserOrNull()?.id
}
