package io.github.chsbuffer.revancedxposed.spotify

import android.app.Application
import app.revanced.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import io.github.chsbuffer.revancedxposed.BaseHook
import io.github.chsbuffer.revancedxposed.S
import io.github.chsbuffer.revancedxposed.injectHostClassLoaderToSelf
import io.github.chsbuffer.revancedxposed.spotify.misc.UnlockPremium
import io.github.chsbuffer.revancedxposed.spotify.misc.privacy.SanitizeSharingLinks
import io.github.chsbuffer.revancedxposed.spotify.misc.widgets.FixThirdPartyLaunchersWidgets

class SpotifyHook(app: Application, lpparam: LoadPackageParam) : BaseHook(app, lpparam) {
    override val hooks = arrayOf(
        ::Extension,
        ::SanitizeSharingLinks,
        ::UnlockPremium,
        ::FixThirdPartyLaunchersWidgets,
        ::h
    )

    fun Extension() {
        injectHostClassLoaderToSelf(this::class.java.classLoader!!, classLoader)
    }

    fun h() {
        runCatching {
            val a = XposedHelpers.findClassIfExists(S.C1, classLoader) ?: run {
                Logger.printDebug { "DexHttp: RealInterceptorChain not found!" }
                return
            }

            val b = runCatching {
                Class.forName(S.C2, true, classLoader)
            }.getOrNull() ?: run {
                Logger.printDebug { "DexHttp: okhttp3.Request not found!" }
                return
            }

            val c = a.methods.firstOrNull { m ->
                m.parameterCount == 1 && m.parameterTypes[0] == b
            } ?: run {
                Logger.printDebug { "DexHttp: proceed(Request) not found!" }
                return
            }

            Logger.printDebug { "DexHttp: setup complete — proceed=${c.name}" }

            val bl = arrayOf(Regex(S.R1), Regex(S.R2), Regex(S.R3))
            val d = S.D1
            val wl = arrayOf(
                Regex(S.P1),
                Regex(S.P2),
                Regex(S.P3),
                Regex(S.P4),
                Regex(S.P5),
                Regex(S.P6),
                Regex(S.P7),
                Regex(S.P8),
            )

            val e = true

            XposedBridge.hookMethod(c, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val request = param.args[0] ?: return

                    val urlStr = request.javaClass.declaredFields.firstNotNullOfOrNull { f ->
                        f.isAccessible = true
                        val v = f.get(request) ?: return@firstNotNullOfOrNull null
                        val s = v.toString()
                        if (s.startsWith("http://") || s.startsWith("https://")) s else null
                    } ?: return

                    Logger.printDebug { "DexHttp: ${request}" }
                    Logger.printDebug { "DexHttp url: $urlStr" }

                    if (bl.any { it.containsMatchIn(urlStr) }) {
                        if (e) param.throwable = java.io.IOException()
                        Logger.printDebug { "AntiDetection: ad blocked → $urlStr" }
                        return
                    }

                    if (!urlStr.contains(d)) return
                    Logger.printDebug { "DexHttp spclient: $urlStr" }
                    if (wl.any { it.containsMatchIn(urlStr) }) return

                    if (e) param.throwable = java.io.IOException()
                    Logger.printDebug { "AntiDetection: spclient blocked → $urlStr" }
                }
            })
        }.onFailure { Logger.printDebug { "DexHttp: setup failed: ${it.message}" } }
    }
}
