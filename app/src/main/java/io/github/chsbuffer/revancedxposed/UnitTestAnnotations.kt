package io.github.chsbuffer.revancedxposed

// Skip Unit Test on unused fingerprint.
// Use with caution!!!
@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class SkipTest()

@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class TargetApp(val app: String)

class AppVersion(val versionString: String) : Comparable<AppVersion> {
    init {
        require(versionString.matches(Regex("\\d+(\\.\\d+)*"))) {
            "Version string must consist of numbers separated by dots (e.g., 1.2 or 1.2.3.4)"
        }
    }

    private val parts: List<Int> by lazy { versionString.split('.').map { it.toInt() } }

    fun getPart(index: Int): Int = parts.elementAtOrElse(index) { 0 }

    override fun compareTo(other: AppVersion): Int {
        require(this.parts.size == other.parts.size) {
            "Version parts count mismatch: ${this.versionString} (${this.parts.size} parts) vs ${other.versionString} (${other.parts.size} parts)"
        }

        val len = parts.size
        for (i in 0 until len) {
            val thisPart = this.getPart(i)
            val otherPart = other.getPart(i)
            if (thisPart != otherPart) {
                return thisPart.compareTo(otherPart)
            }
        }
        return 0
    }

    override fun toString(): String = versionString
}

// Skip Unit Test by version constraint
@Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class RequireAppVersion(
    val minVersion: String = "",
    val maxVersion: String = ""
)

class VersionConstraintFailedException(message: String) : Exception(message)

fun match(appVersion: AppVersion, minVersionStr: String, maxVersionStr: String) {
    val minVersion = minVersionStr.takeIf { it.isNotEmpty() }?.let { AppVersion(it) }
    val maxVersion = maxVersionStr.takeIf { it.isNotEmpty() }?.let { AppVersion(it) }
    when {
        minVersion == null && maxVersion == null -> return // No version constraint
        minVersion != null && appVersion < minVersion ->
            throw VersionConstraintFailedException("Min version mismatch (current: $appVersion, required: $minVersion)")

        maxVersion != null && appVersion > maxVersion ->
            throw VersionConstraintFailedException("Max version mismatch (current: $appVersion, required: $maxVersion)")
    }
}
