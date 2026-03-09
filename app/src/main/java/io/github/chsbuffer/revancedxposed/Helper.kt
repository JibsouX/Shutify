package io.github.chsbuffer.revancedxposed

import android.content.res.loader.ResourcesLoader
import android.content.res.loader.ResourcesProvider
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member

typealias IScopedHookCallback = ScopedHookParam.(MethodHookParam) -> Unit
typealias IHookCallback = (MethodHookParam) -> Unit

class HookDsl<TCallback>(emptyCallback: TCallback) {
    var before: TCallback = emptyCallback
    var after: TCallback = emptyCallback

    fun before(f: TCallback) {
        this.before = f
    }

    fun after(f: TCallback) {
        this.after = f
    }
}

inline fun Member.hookMethod(crossinline block: HookDsl<IHookCallback>.() -> Unit) {
    val builder = HookDsl<IHookCallback> {}.apply(block)
    hookMethodInternal(builder.before, builder.after)
}

inline fun Member.hookMethodInternal(
    crossinline before: IHookCallback, crossinline after: IHookCallback
) {
    XposedBridge.hookMethod(this, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam) {
            before(param)
        }

        override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
            after(param)
        }
    })
}

@JvmInline
value class ScopedHookParam(val outerParam: MethodHookParam)

fun scopedHook(vararg pairs: Pair<Member, HookDsl<IScopedHookCallback>.() -> Unit>): XC_MethodHook {
    val hook = ScopedHook()
    pairs.forEach { (member, block) ->
        val builder = HookDsl<IScopedHookCallback> {}.apply(block)
        hook.hookInnerMethod(member, builder.before, builder.after)
    }
    return hook
}

inline fun scopedHook(
    hookMethod: Member, crossinline f: HookDsl<IScopedHookCallback>.() -> Unit
): XC_MethodHook {
    val hook = ScopedHook()
    val builder = HookDsl<IScopedHookCallback> {}.apply(f)
    hook.hookInnerMethod(hookMethod, builder.before, builder.after)
    return hook
}

class ScopedHook : XC_MethodHook() {
    inline fun hookInnerMethod(
        hookMethod: Member,
        crossinline before: IScopedHookCallback,
        crossinline after: IScopedHookCallback
    ) {
        XposedBridge.hookMethod(hookMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val outerParam = outerParam.get()
                if (outerParam == null) return
                before(ScopedHookParam(outerParam), param)
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                val outerParam = outerParam.get()
                if (outerParam == null) return
                after(ScopedHookParam(outerParam), param)
            }
        })
    }

    val outerParam: ThreadLocal<XC_MethodHook.MethodHookParam> = ThreadLocal<MethodHookParam>()

    override fun beforeHookedMethod(param: MethodHookParam) {
        outerParam.set(param)
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        outerParam.remove()
    }
}

lateinit var XposedInit: IXposedHookZygoteInit.StartupParam

private val resourceLoader by lazy @RequiresApi(Build.VERSION_CODES.R) {
    val fileDescriptor = ParcelFileDescriptor.open(
        File(XposedInit.modulePath), ParcelFileDescriptor.MODE_READ_ONLY
    )
    val provider = ResourcesProvider.loadFromApk(fileDescriptor)
    val loader = ResourcesLoader()
    loader.addProvider(provider)
    return@lazy loader
}

fun injectHostClassLoaderToSelf(self: ClassLoader, host: ClassLoader) {
    val findClassMethod =
        XposedHelpers.findMethodExact(ClassLoader::class.java, "findClass", String::class.java)
    self.setObjectField("parent", object : ClassLoader(self.parent) {
        /**
         * In the context of Xposed modules, the class loading hierarchy can be complex.
         * The module's classes are loaded by its own ClassLoader (`self`).
         * The host application's classes are loaded by its ClassLoader (`host`).
         *
         * The goal here is to allow the module to access classes from the host application.
         * We achieve this by creating a new ClassLoader that becomes the parent of `self`.
         * This new parent ClassLoader will first attempt to load classes using `self.findClass()`.
         * If that fails, it will then try to load the class from the `host` ClassLoader.
         *
         * This explicit ordering is crucial for compatibility with various Xposed frameworks:
         * - **LSPosed:** LSPosed's `LspModuleClassLoader` already prioritizes its own `findClass`
         *   before delegating to `parent.loadClass`. So, this customization might seem redundant for LSPosed.
         *
         * - **Other Xposed Frameworks:** Other frameworks might use a standard `PathClassLoader`
         *   as the module's ClassLoader. A standard `PathClassLoader` typically delegates to
         *   `parent.loadClass` *before* attempting `findClass` itself. If the host ClassLoader's parent
         *   is replaced with another intermediary ClassLoader that attempts to load classes from the module
         *   (see `SponsorBlockPatch.kt`), it could lead to infinite recursion.
         *
         * By inserting this intermediary ClassLoader and overriding `findClass` to prioritize
         * `self.findClass()`, we ensure that the module's classes are always checked first,
         * preventing potential infinite recursion and ensuring that stubbed classes are loaded
         * from the host only as a fallback.
         */
        override fun findClass(name: String): Class<*> {
            try {
                return findClassMethod(self, name) as Class<*>
            } catch (_: InvocationTargetException) {
            }

            try {
                return host.loadClass(name)
            } catch (_: ClassNotFoundException) {
            }

            throw ClassNotFoundException(name)
        }
    })
}

@Suppress("UNCHECKED_CAST")
fun Class<*>.enumValueOf(name: String): Enum<*>? {
    return try {
        java.lang.Enum.valueOf(this as Class<out Enum<*>>, name)
    } catch (_: IllegalArgumentException) {
        null
    }
}