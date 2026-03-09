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


            val bl = arrayOf(Regex(S.R1), Regex(S.R2), Regex(S.R3))
            val d = S.D1
            val wl = Regex(S.WL)

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


                    if (bl.any { it.containsMatchIn(urlStr) }) {
                        if (e) param.throwable = java.io.IOException()
                        return
                    }

                    if (!urlStr.contains(d)) return
                    if (wl.containsMatchIn(urlStr)) return

                    if (e) param.throwable = java.io.IOException()
                }
            })
    }
}
