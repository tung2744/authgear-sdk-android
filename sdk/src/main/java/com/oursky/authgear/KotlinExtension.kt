@file:JvmName("KotlinExtensions")

package com.oursky.authgear

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @see [Authgear.configure].
 */
suspend fun Authgear.configure() {
    return withContext(Dispatchers.IO) {
        core.configure()
    }
}

/**
 * @see [Authgear.refreshAccessTokenIfNeededSync].
 */
suspend fun Authgear.refreshTokenIfNeeded(): String? {
    return core.refreshAccessTokenIfNeeded()
}

/**
 * @see [Authgear.authenticate].
 */
suspend fun Authgear.authenticate(options: AuthenticateOptions): UserInfo {
    return core.authenticate(options)
}

/**
 * @see [Authgear.createAuthenticateRequest].
 */
@ExperimentalAuthgearApi
suspend fun Authgear.createAuthenticateRequest(options: AuthenticateOptions): AuthenticationRequest {
    return withContext(Dispatchers.IO) {
        core.createAuthenticateRequest(options)
    }
}

/**
 * @see [Authgear.finishAuthentication].
 */
@ExperimentalAuthgearApi
suspend fun Authgear.finishAuthentication(
    finishUri: String,
    request: AuthenticationRequest
): UserInfo {
    return withContext(Dispatchers.IO) {
        core.finishAuthorization(finishUri, request.verifier)
    }
}

/**
 * @see [Authgear.authenticateAnonymously]
 */
suspend fun Authgear.authenticateAnonymousLy(): UserInfo {
    return core.authenticateAnonymously()
}

/**
 * @see [Authgear.logout]
 */
suspend fun Authgear.logout(force: Boolean? = null) {
    return core.logout(force)
}

/**
 * @see [Authgear.promoteAnonymousUser].
 */
suspend fun Authgear.promoteAnonymousUser(options: PromoteOptions): UserInfo {
    return core.promoteAnonymousUser(options)
}

/**
 * @see [Authgear.fetchUserInfo].
 */
suspend fun Authgear.fetchUserInfo(): UserInfo {
    return withContext(Dispatchers.IO) {
        core.fetchUserInfo()
    }
}

/**
 * @see [Authgear.enableBiometric]
 */
suspend fun Authgear.enableBiometric(options: BiometricOptions) {
    core.enableBiometric(options)
}

/**
 * @see [Authgear.authenticateBiometric]
 */
suspend fun Authgear.authenticateBiometric(options: BiometricOptions): UserInfo {
    return core.authenticateBiometric(options)
}

/**
 * @see [Authgear.generateUrl].
 */
@ExperimentalAuthgearApi
suspend fun Authgear.generateUrl(redirectURI: String): Uri {
    return withContext(Dispatchers.IO) {
        core.generateUrl(redirectURI)
    }
}