package io.github.chsbuffer.revancedxposed

import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.matchers.ClassMatcher
import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher
import org.luckypray.dexkit.result.ClassData
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData
import org.luckypray.dexkit.util.DexSignUtil.getTypeName
import java.lang.reflect.Modifier

private fun getTypeNameCompat(it: String): String? {
    return if (it.trimStart('[').startsWith('L') && !it.endsWith(';')) null
    else getTypeName(it)
}

enum class AccessFlags(val modifier: Int) {
    PUBLIC(Modifier.PUBLIC),
    PRIVATE(Modifier.PRIVATE),
    PROTECTED(Modifier.PROTECTED),
    STATIC(Modifier.STATIC),
    FINAL(Modifier.FINAL),
    CONSTRUCTOR(0),
}

fun MethodMatcher.definingClass(descriptor: String) =
    this.declaredClass { this.descriptor = descriptor }

fun MethodMatcher.strings(vararg strings: String) =
    this.usingStrings(strings.toList())

fun MethodMatcher.opcodes(vararg opcodes: Opcode): OpCodesMatcher {
    return OpCodesMatcher(opcodes.map { it.opCode }).also {
        this.opCodes(it)
    }
}

fun MethodMatcher.accessFlags(vararg accessFlags: AccessFlags) {
    val modifiers = accessFlags.map { it.modifier }.reduce { acc, next -> acc or next }
    if (modifiers != 0) this.modifiers(modifiers)
    if (accessFlags.contains(AccessFlags.CONSTRUCTOR)) {
        if (accessFlags.contains(AccessFlags.STATIC)) this.name = "<clinit>"
        else this.name = "<init>"
    }
}

fun MethodMatcher.parameters(vararg parameters: String) {
    this.paramTypes(parameters.map(::getTypeNameCompat))
}

fun MethodMatcher.returns(returnType: String) {
    getTypeNameCompat(returnType)?.let { this.returnType = it }
}

fun MethodMatcher.literal(literalSupplier: () -> Number) {
    this.usingNumbers(literalSupplier())
}

class Fingerprint(val dexkit: DexKitBridge, init: Fingerprint.() -> Unit) {
    var classMatcher: ClassMatcher? = null
    val methodMatcher = MethodMatcher()

    init {
        init(this)
    }

    fun name(name: String) = methodMatcher.name(name)
    fun definingClass(descriptor: String) = classMatcher { this.descriptor = descriptor }
    fun strings(vararg strings: String) = methodMatcher.strings(*strings)
    fun opcodes(vararg opcodes: Opcode) = methodMatcher.opcodes(*opcodes)
    fun accessFlags(vararg accessFlags: AccessFlags) = methodMatcher.accessFlags(*accessFlags)
    fun parameters(vararg parameters: String) = methodMatcher.parameters(*parameters)
    fun returns(returnType: String) = methodMatcher.returns(returnType)
    fun literal(literalSupplier: () -> Number) = methodMatcher.literal(literalSupplier)

    /*
    * dexkit method matcher
    * */
    fun methodMatcher(block: MethodMatcher.() -> Unit) {
        block(methodMatcher)
    }

    /*
    * dexkit class matcher
    * */
    fun classMatcher(block: ClassMatcher.() -> Unit) {
        classMatcher = ClassMatcher().apply(block)
    }

    fun run(): MethodData {
        return if (classMatcher != null) {
            dexkit.findClass {
                matcher(classMatcher!!)
            }.findMethod {
                matcher(methodMatcher)
            }.single()
        } else {
            dexkit.findMethod {
                matcher(methodMatcher)
            }.single()
        }
    }
}

fun DexKitBridge.fingerprint(block: Fingerprint.() -> Unit): MethodData {
    return Fingerprint(this, block).run()
}

interface ResourceFinder {
    operator fun get(type: String, name: String): Int
}

typealias FindFunc = DexKitBridge.() -> Any
typealias FindClassFunc = DexKitBridge.() -> ClassData
typealias FindMethodFunc = DexKitBridge.() -> MethodData
typealias FindMethodListFunc = DexKitBridge.() -> List<MethodData>
typealias FindFieldFunc = DexKitBridge.() -> FieldData

fun fingerprint(block: Fingerprint.() -> Unit): FindMethodFunc {
    return { Fingerprint(this, block).run() }
}

fun findMethodDirect(block: FindMethodFunc) = block
fun findMethodListDirect(block: FindMethodListFunc) = block
fun findClassDirect(block: FindClassFunc) = block
fun findFieldDirect(block: FindFieldFunc) = block
