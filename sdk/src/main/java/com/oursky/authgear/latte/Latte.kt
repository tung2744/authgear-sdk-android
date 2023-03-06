package com.oursky.authgear.latte

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import com.oursky.authgear.*
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalAuthgearApi::class)
class Latte(
    private val authgear: Authgear,
    private val customUIEndpoint: String
) {
    var delegate: LatteDelegate? = null

    private data class LatteResult(val broadcastAction: String, val finishUri: String?) {
        inline fun <T> handle(authgear: Authgear, fn: (finishUri: String) -> T): LatteHandle<T> {
            val finishUri = this.finishUri ?: return LatteHandle.Failure(authgear, broadcastAction, CancelException())
            return try {
                val value = fn(finishUri)
                LatteHandle.Success(authgear, broadcastAction, value)
            } catch (e: Throwable) {
                LatteHandle.Failure(authgear, broadcastAction, e)
            }
        }
    }

    private fun makeRandomAction(app: Application): String {
        val rng = SecureRandom()
        val byteArray = ByteArray(32)
        rng.nextBytes(byteArray)
        val action = base64UrlEncode(byteArray)
        return "${app.packageName}.latte.$action"
    }

    private suspend fun startActivity(url: Uri, redirectUri: String): LatteResult {
        return suspendCoroutine { k ->
            val app = authgear.core.application
            val broadcastAction = makeRandomAction(app)
            val br = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val message = intent?.let { LatteActivity.extract(it) } ?: return

                    when (message) {
                        is LatteMessage.HandleRedirectURI -> {
                            app.unregisterReceiver(this)
                            k.resume(LatteResult(broadcastAction, message.finishUri))
                        }
                        is LatteMessage.OpenEmailClient -> {
                            if (context == null) return
                            val intent = EmailClient.makeEmailClientIntentChooser(
                                context,
                                "Choose Email Client",
                                listOf(EmailClient.GMAIL, EmailClient.OUTLOOK)
                            )
                            context.startActivity(intent)
                        }
                        is LatteMessage.ViewPage -> {
                            delegate?.onViewPage(message.event)
                        }
                        else -> {}
                    }
                }
            }
            app.registerReceiver(br, IntentFilter(broadcastAction))
            app.startActivity(
                LatteActivity.createIntent(app, url, redirectUri, broadcastAction)
            )
        }
    }

    suspend fun authenticate(options: AuthenticateOptions): LatteHandle<UserInfo> {
        val request = authgear.createAuthenticateRequest(options)
        val result = startActivity(request.url, request.redirectUri)
        return result.handle(authgear) {
            authgear.finishAuthentication(it, request)
        }
    }

    suspend fun verifyEmail(
        email: String,
        state: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<UserInfo> {
        val entryUrl = "$customUIEndpoint/verify/email"
        val redirectUri = "$customUIEndpoint/verify/email/completed"

        val verifyEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("redirect_uri", redirectUri)
            if (state != null) {
                appendQueryParameter("state", state)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(verifyEmailUrl.toString())
        val result = startActivity(url, redirectUri)
        return result.handle(authgear) {
            authgear.fetchUserInfo()
        }
    }

    suspend fun changePassword(
        state: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<Unit> {
        val entryUrl = "$customUIEndpoint/settings/change_password"
        val redirectUri = "latte://complete"

        val changeEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("redirect_uri", redirectUri)
            if (state != null) {
                appendQueryParameter("state", state)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(changeEmailUrl.toString())
        val result = startActivity(url, redirectUri)
        return result.handle(authgear) { }
    }

    suspend fun resetPassword(uri: Uri): LatteHandle<Unit> {
        val entryUrl = "$customUIEndpoint/recovery/reset"
        val redirectUri = "latte://reset-complete"

        val resetPasswordUrl = Uri.parse(entryUrl).buildUpon().apply {
            for (q in uri.getQueryList()) {
                appendQueryParameter(q.first, q.second)
            }
            appendQueryParameter("redirect_uri", redirectUri)
        }.build()
        val result = startActivity(resetPasswordUrl, redirectUri)
        return result.handle(authgear) { }
    }

    suspend fun changeEmail(
        email: String,
        phoneNumber: String,
        state: String? = null,
        uiLocales: List<String>? = null
    ): LatteHandle<UserInfo> {
        val entryUrl = "$customUIEndpoint/settings/change_email"
        val redirectUri = "$customUIEndpoint/verify/email/completed"

        val changeEmailUrl = Uri.parse(entryUrl).buildUpon().apply {
            appendQueryParameter("email", email)
            appendQueryParameter("phone", phoneNumber)
            appendQueryParameter("redirect_uri", redirectUri)
            if (state != null) {
                appendQueryParameter("state", state)
            }
            if (uiLocales != null) {
                appendQueryParameter("ui_locales", UILocales.stringify(uiLocales))
            }
        }.build()
        val url = authgear.generateUrl(changeEmailUrl.toString())
        val result = startActivity(url, redirectUri)
        return result.handle(authgear) {
            authgear.fetchUserInfo()
        }
    }
}
