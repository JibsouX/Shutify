package io.github.chsbuffer.revancedxposed.spotify.misc

import app.revanced.extension.shared.Logger
import app.revanced.extension.spotify.misc.UnlockPremiumPatch
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.chsbuffer.revancedxposed.callMethod
import io.github.chsbuffer.revancedxposed.findField
import io.github.chsbuffer.revancedxposed.findFirstFieldByExactType
import io.github.chsbuffer.revancedxposed.S
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook
import org.luckypray.dexkit.wrap.DexField
import org.luckypray.dexkit.wrap.DexMethod
import java.lang.reflect.Constructor
import java.lang.reflect.Field

@Suppress("UNCHECKED_CAST")
fun SpotifyHook.UnlockPremium() {
    // Override the attributes map in the getter method.
    ::productStateProtoFingerprint.hookMethod {
        val field = ::attributesMapField.field
        before { param ->
            UnlockPremiumPatch.overrideAttributes(field.get(param.thisObject) as Map<String, *>)
        }
    }

    // Add the query parameter trackRows to show popular tracks in the artist page.
    ::buildQueryParametersFingerprint.hookMethod {
        after { param ->
            val result = param.result
            val FIELD = S.N1
            if (result.toString().contains("${FIELD}=")) {
                param.result = XposedBridge.invokeOriginalMethod(
                    param.method, param.thisObject, arrayOf(param.args[0], true)
                )
            }
        }
    }

    // Enable choosing a specific song/artist via Google Assistant.
    ::contextFromJsonFingerprint.hookMethod {
        fun removeStationString(field: Field, obj: Any) {
            field.set(obj, UnlockPremiumPatch.removeStationString(field.get(obj) as String))
        }

        after { param ->
            val thiz = param.result
            val clazz = param.result.javaClass
            removeStationString(clazz.findField("uri"), thiz)
            removeStationString(clazz.findField("url"), thiz)
        }
    }

    // Disable forced shuffle when asking for an album/playlist via Google Assistant.
    XposedHelpers.findAndHookMethod(
        S.M1,
        classLoader,
        "build",
        object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.thisObject.callMethod(S.M2, false)
            }
        })

    // Hook the method which adds context menu items and return before adding if the item is a Premium ad.
    val contextMenuViewModelClazz = ::contextMenuViewModelClass.clazz
    XposedBridge.hookAllConstructors(
        contextMenuViewModelClazz, object : XC_MethodHook() {
            val isPremiumUpsell = ::isPremiumUpsellField.field

            override fun beforeHookedMethod(param: MethodHookParam) {
                val parameterTypes = (param.method as Constructor<*>).parameterTypes
                for (i in 0 until param.args.size) {
                    if (parameterTypes[i].name != "java.util.List") continue
                    val original = param.args[i] as? List<*> ?: continue
                    val filtered = original.filter {
                        it!!.callMethod(S.M3).let { isPremiumUpsell.get(it) } != true
                    }
                    param.args[i] = filtered
                }
            }
        })

    // Remove ads sections from home.
    ::homeStructureGetSectionsFingerprint.hookMethod {
        after { param ->
            val sections = param.result
            // Set sections mutable
            sections.javaClass.findFirstFieldByExactType(Boolean::class.java).set(sections, true)
            UnlockPremiumPatch.removeHomeSections(param.result as MutableList<*>)
        }
    }
    // Remove ads sections from browser.
    ::browseStructureGetSectionsFingerprint.hookMethod {
        after { param ->
            val sections = param.result
            // Set sections mutable
            sections.javaClass.findFirstFieldByExactType(Boolean::class.java).set(sections, true)
            UnlockPremiumPatch.removeBrowseSections(param.result as MutableList<*>)
        }
    }

    // Remove pendragon (pop up ads) requests and return the errors instead.
    val replaceFetchRequestSingleWithError = object : XC_MethodHook() {
        val justMethod =
            DexMethod("Lio/reactivex/rxjava3/core/Single;->just(Ljava/lang/Object;)Lio/reactivex/rxjava3/core/Single;").toMethod()

        val onErrorField =
            DexField("Lio/reactivex/rxjava3/internal/operators/single/SingleOnErrorReturn;->b:Lio/reactivex/rxjava3/functions/Function;").toField()

        override fun afterHookedMethod(param: MethodHookParam) {
            if (!param.result.javaClass.name.endsWith("SingleOnErrorReturn")) return
            val justError = justMethod.invoke(null, onErrorField.get(param.result))
            param.result = justError
        }
    }

    ::pendragonJsonFetchMessageRequestFingerprint.hookMethod(replaceFetchRequestSingleWithError)
    ::pendragonJsonFetchMessageListRequestFingerprint.hookMethod(replaceFetchRequestSingleWithError)
}