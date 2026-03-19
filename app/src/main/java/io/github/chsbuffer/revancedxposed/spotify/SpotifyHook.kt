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
import io.github.chsbuffer.revancedxposed.spotify.misc.playback.PlayerSession
import io.github.chsbuffer.revancedxposed.spotify.misc.widgets.FixThirdPartyLaunchersWidgets

class SpotifyHook(app: Application, lpparam: LoadPackageParam) : BaseHook(app, lpparam) {
    // Global kill-switch for all ad-blocking: set to false to disable everything.
    val e = true

    override val hooks = arrayOf(
        ::Extension,
        ::SanitizeSharingLinks,
        ::UnlockPremium,
        ::FixThirdPartyLaunchersWidgets,
        ::PlayerSession,
        ::h
    )

    fun Extension() {
        injectHostClassLoaderToSelf(this::class.java.classLoader!!, classLoader)
    }

    fun h() {
        runCatching {
            val a = XposedHelpers.findClassIfExists(S.C1, classLoader) ?: run {
                return
            }

            val b = runCatching {
                Class.forName(S.C2, true, classLoader)
            }.getOrNull() ?: run {
                return
            }

            val c = a.methods.firstOrNull { m ->
                m.parameterCount == 1 && m.parameterTypes[0] == b
            } ?: run {
                return
            }


            val bl = Regex(S.BL)
            val bp = Regex(S.BP)
            val d  = S.D1
            val wl = Regex(S.WL)

            XposedBridge.hookMethod(c, object : XC_MethodHook() {
                @Volatile var g: java.lang.reflect.Field? = null

                override fun beforeHookedMethod(param: MethodHookParam) {
                    val request = param.args[0] ?: return

                    val urlStr: String
                    if (g != null) {
                        urlStr = g!!.get(request)?.toString() ?: return
                    } else {
                        urlStr = request.javaClass.declaredFields.firstNotNullOfOrNull { f ->
                            f.isAccessible = true
                            val v = f.get(request) ?: return@firstNotNullOfOrNull null
                            val s = v.toString()
                            val prefixes = listOf("http://", "https://")
                            if (prefixes.any { s.startsWith(it) }) {
                                g = f
                                s
                            } else null
                        } ?: return
                    }


                    val spd = urlStr.contains(d)

                    if (spd) {

                        if (wl.containsMatchIn(urlStr)) {
                            return
                        }

                        if (bl.containsMatchIn(urlStr)) {
                            if (e) {
                                val label = if (bp.containsMatchIn(urlStr)) "ad path" else "BL"
                                param.throwable = java.io.IOException()
                            }
                            return
                        }

                        if (e) {
                            param.throwable = java.io.IOException()
                        }
                        return
                    }

                    if (bl.containsMatchIn(urlStr)) {
                        if (e) {
                            val label = if (bp.containsMatchIn(urlStr)) "ad path" else "BL"
                            param.throwable = java.io.IOException()
                        }
                    }
                }
            })
    }
}