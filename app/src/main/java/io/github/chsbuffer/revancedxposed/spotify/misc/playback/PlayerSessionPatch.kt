package io.github.chsbuffer.revancedxposed.spotify.misc.playback

import android.app.Activity
import app.revanced.extension.shared.Logger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.revancedxposed.S
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook

fun SpotifyHook.PlayerSession() {
    // Respect the global kill-switch defined in SpotifyHook
    if (!e) return

    listOf(S.V1, S.V2).forEach { className ->
        val clazz = XposedHelpers.findClassIfExists(className, classLoader) ?: run {
            return@forEach
        }
        XposedBridge.hookAllMethods(clazz, "onCreate", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                (param.thisObject as? Activity)?.finish()
            }
        })
    }

    ::adBreakContextFingerprint.hookMethod {
        before { param ->
            param.result = param.thisObject
        }
    }

    val adPrefix = S.V3
    val uriAccessor = runCatching {
        classLoader.loadClass(S.P4)
            .methods
            .firstOrNull { m -> (m.name == "uri" || m.name == "getUri") && m.parameterCount == 0 }
            ?.also { it.isAccessible = true }
    }.getOrNull().also { acc ->
            "PlayerSession: uri accessor ${if (acc != null) "resolved (${acc.name})" else "using fallback"}"
        }
    }

    val trackFilterHook = object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val track = param.args.firstOrNull() ?: return
            val uri = if (uriAccessor != null) {
                runCatching { uriAccessor.invoke(track)?.toString() }.getOrNull()
            } else {
                resolveTrackUri(track)
            } ?: return
            if (uri.startsWith(adPrefix)) {
                param.result = param.thisObject
            }
        }
    }

    ::adTrackBuilderFingerprints.dexMethodList.forEach { it.hookMethod(trackFilterHook) }
}

private fun resolveTrackUri(obj: Any): String? {
    return try {
        obj.javaClass.methods
            .firstOrNull { m ->
                (m.name == "uri" || m.name == "getUri") && m.parameterCount == 0
            }
            ?.let { m -> m.isAccessible = true; m.invoke(obj)?.toString() }
            ?: run {
                var clazz: Class<*>? = obj.javaClass
                while (clazz != null && clazz != Any::class.java) {
                    val field = clazz.declaredFields.firstOrNull { f ->
                        f.type == String::class.java && f.name.lowercase().contains("uri")
                    }
                    if (field != null) {
                        field.isAccessible = true
                        return field.get(obj)?.toString()
                    }
                    clazz = clazz.superclass
                }
                null
            }
    } catch (_: Throwable) {
        null
    }
}
