@file:Suppress("unused")

package io.github.chsbuffer.revancedxposed

import dalvik.system.BaseDexClassLoader
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge.invokeOriginalMethod
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.findClassIfExists
import de.robv.android.xposed.XposedHelpers.findField
import de.robv.android.xposed.XposedHelpers.findFieldIfExists
import de.robv.android.xposed.XposedHelpers.findFirstFieldByExactType
import de.robv.android.xposed.XposedHelpers.getBooleanField
import de.robv.android.xposed.XposedHelpers.getIntField
import de.robv.android.xposed.XposedHelpers.getLongField
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.XposedHelpers.getStaticObjectField
import de.robv.android.xposed.XposedHelpers.newInstance
import de.robv.android.xposed.XposedHelpers.setBooleanField
import de.robv.android.xposed.XposedHelpers.setFloatField
import de.robv.android.xposed.XposedHelpers.setIntField
import de.robv.android.xposed.XposedHelpers.setLongField
import de.robv.android.xposed.XposedHelpers.setObjectField
import de.robv.android.xposed.XposedHelpers.setStaticObjectField
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.util.Enumeration

typealias MethodHookParam = MethodHookParam
typealias Replacer = (MethodHookParam) -> Any?

fun MethodHookParam.invokeOriginalMethod(): Any? = invokeOriginalMethod(method, thisObject, args)

inline fun <T, R> T.runCatchingOrNull(func: T.() -> R?) = try {
    func()
} catch (e: Throwable) {
    null
}

fun Any.getObjectField(field: String?): Any? = getObjectField(this, field)

fun Any.getObjectFieldOrNull(field: String?): Any? = runCatchingOrNull {
    getObjectField(this, field)
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectFieldAs(field: String?) = getObjectField(this, field) as T

@Suppress("UNCHECKED_CAST")
fun <T> Any.getObjectFieldOrNullAs(field: String?) = runCatchingOrNull {
    getObjectField(this, field) as T
}

fun Any.getIntField(field: String?) = getIntField(this, field)

fun Any.getIntFieldOrNull(field: String?) = runCatchingOrNull {
    getIntField(this, field)
}

fun Any.getLongField(field: String?) = getLongField(this, field)

fun Any.getLongFieldOrNull(field: String?) = runCatchingOrNull {
    getLongField(this, field)
}

fun Any.getBooleanFieldOrNull(field: String?) = runCatchingOrNull {
    getBooleanField(this, field)
}

fun Any.callMethod(methodName: String?, vararg args: Any?): Any? =
    callMethod(this, methodName, *args)

fun Any.callMethodOrNull(methodName: String?, vararg args: Any?): Any? = runCatchingOrNull {
    callMethod(this, methodName, *args)
}

fun Class<*>.callStaticMethod(methodName: String?, vararg args: Any?): Any? =
    callStaticMethod(this, methodName, *args)

fun Class<*>.callStaticMethodOrNull(methodName: String?, vararg args: Any?): Any? =
    runCatchingOrNull {
        callStaticMethod(this, methodName, *args)
    }

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.callStaticMethodAs(methodName: String?, vararg args: Any?) =
    callStaticMethod(this, methodName, *args) as T

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.callStaticMethodOrNullAs(methodName: String?, vararg args: Any?) =
    runCatchingOrNull {
        callStaticMethod(this, methodName, *args) as T
    }

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.getStaticObjectFieldAs(field: String?) = getStaticObjectField(this, field) as T

@Suppress("UNCHECKED_CAST")
fun <T> Class<*>.getStaticObjectFieldOrNullAs(field: String?) = runCatchingOrNull {
    getStaticObjectField(this, field) as T
}

fun Class<*>.getStaticObjectField(field: String?): Any? = getStaticObjectField(this, field)

fun Class<*>.getStaticObjectFieldOrNull(field: String?): Any? = runCatchingOrNull {
    getStaticObjectField(this, field)
}

fun Class<*>.setStaticObjectField(field: String?, obj: Any?) = apply {
    setStaticObjectField(this, field, obj)
}

fun Class<*>.setStaticObjectFieldIfExist(field: String?, obj: Any?) = apply {
    try {
        setStaticObjectField(this, field, obj)
    } catch (ignored: Throwable) {
    }
}

inline fun <reified T> Class<*>.findFieldByExactType(): Field? =
    findFirstFieldByExactType(this, T::class.java)

fun Class<*>.findFieldByExactType(type: Class<*>): Field? =
    findFirstFieldByExactType(this, type)

@Suppress("UNCHECKED_CAST")
fun <T> Any.callMethodAs(methodName: String?, vararg args: Any?) =
    callMethod(this, methodName, *args) as T

@Suppress("UNCHECKED_CAST")
fun <T> Any.callMethodOrNullAs(methodName: String?, vararg args: Any?) = runCatchingOrNull {
    callMethod(this, methodName, *args) as T
}

fun Any.callMethod(methodName: String?, parameterTypes: Array<Class<*>>, vararg args: Any?): Any? =
    callMethod(this, methodName, parameterTypes, *args)

fun Any.callMethodOrNull(
    methodName: String?,
    parameterTypes: Array<Class<*>>,
    vararg args: Any?
): Any? = runCatchingOrNull {
    callMethod(this, methodName, parameterTypes, *args)
}

fun Class<*>.callStaticMethod(
    methodName: String?,
    parameterTypes: Array<Class<*>>,
    vararg args: Any?
): Any? = callStaticMethod(this, methodName, parameterTypes, *args)

fun Class<*>.callStaticMethodOrNull(
    methodName: String?,
    parameterTypes: Array<Class<*>>,
    vararg args: Any?
): Any? = runCatchingOrNull {
    callStaticMethod(this, methodName, parameterTypes, *args)
}

fun String.findClass(classLoader: ClassLoader?): Class<*> = findClass(this, classLoader)

infix fun String.on(classLoader: ClassLoader?): Class<*> = findClass(this, classLoader)

fun String.findClassOrNull(classLoader: ClassLoader?): Class<*>? =
    findClassIfExists(this, classLoader)

infix fun String.from(classLoader: ClassLoader?): Class<*>? =
    findClassIfExists(this, classLoader)

fun Class<*>.new(vararg args: Any?): Any = newInstance(this, *args)

fun Class<*>.new(parameterTypes: Array<Class<*>>, vararg args: Any?): Any =
    newInstance(this, parameterTypes, *args)

fun Class<*>.findField(field: String?): Field = findField(this, field)

fun Class<*>.findFieldOrNull(field: String?): Field? = findFieldIfExists(this, field)

fun <T> T.setIntField(field: String?, value: Int) = apply {
    setIntField(this, field, value)
}

fun <T> T.setLongField(field: String?, value: Long) = apply {
    setLongField(this, field, value)
}

fun <T> T.setObjectField(field: String?, value: Any?) = apply {
    setObjectField(this, field, value)
}

fun <T> T.setBooleanField(field: String?, value: Boolean) = apply {
    setBooleanField(this, field, value)
}

fun <T> T.setFloatField(field: String?, value: Float) = apply {
    setFloatField(this, field, value)
}

fun Class<*>.findFirstFieldByExactType(type: Class<*>): Field =
    findFirstFieldByExactType(this, type)

fun Class<*>.findFirstFieldByExactTypeOrNull(type: Class<*>?): Field? = runCatchingOrNull {
    findFirstFieldByExactType(this, type)
}

fun Any.getFirstFieldByExactType(type: Class<*>): Any? =
    javaClass.findFirstFieldByExactType(type).get(this)

@Suppress("UNCHECKED_CAST")
fun <T> Any.getFirstFieldByExactTypeAs(type: Class<*>) =
    javaClass.findFirstFieldByExactType(type).get(this) as? T

inline fun <reified T : Any> Any.getFirstFieldByExactType() =
    javaClass.findFirstFieldByExactType(T::class.java).get(this) as? T

fun Any.getFirstFieldByExactTypeOrNull(type: Class<*>?): Any? = runCatchingOrNull {
    javaClass.findFirstFieldByExactTypeOrNull(type)?.get(this)
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.getFirstFieldByExactTypeOrNullAs(type: Class<*>?) =
    getFirstFieldByExactTypeOrNull(type) as? T

inline fun <reified T> Any.getFirstFieldByExactTypeOrNull() =
    getFirstFieldByExactTypeOrNull(T::class.java) as? T

inline fun ClassLoader.findDexClassLoader(crossinline delegator: (BaseDexClassLoader) -> BaseDexClassLoader = { x -> x }): BaseDexClassLoader? {
    var classLoader = this
    while (classLoader !is BaseDexClassLoader) {
        if (classLoader.parent != null) classLoader = classLoader.parent
        else return null
    }
    return delegator(classLoader)
}

inline fun ClassLoader.allClassesList(crossinline delegator: (BaseDexClassLoader) -> BaseDexClassLoader = { x -> x }): List<String> {
    return findDexClassLoader(delegator)?.getObjectField("pathList")
        ?.getObjectFieldAs<Array<Any>>("dexElements")
        ?.flatMap {
            it.getObjectField("dexFile")?.callMethodAs<Enumeration<String>>("entries")?.toList()
                .orEmpty()
        }.orEmpty()
}

val Member.isStatic: Boolean
    inline get() = Modifier.isStatic(modifiers)
val Member.isFinal: Boolean
    inline get() = Modifier.isFinal(modifiers)
val Member.isPublic: Boolean
    inline get() = Modifier.isPublic(modifiers)
val Member.isNotStatic: Boolean
    inline get() = !isStatic
val Member.isAbstract: Boolean
    inline get() = Modifier.isAbstract(modifiers)
val Member.isPrivate: Boolean
    inline get() = Modifier.isPrivate(modifiers)
val Class<*>.isAbstract: Boolean
    inline get() = !isPrimitive && Modifier.isAbstract(modifiers)
val Class<*>.isFinal: Boolean
    inline get() = !isPrimitive && Modifier.isFinal(modifiers)