package app.revanced.extension.shared.settings

import io.github.chsbuffer.revancedxposed.BuildConfig

class BooleanSetting(val value: Boolean) {
    fun get() = value
}

class BaseSettings {
    companion object {
        @JvmField
        val DEBUG = BooleanSetting(BuildConfig.DEBUG)

        @JvmField
        val DEBUG_TOAST_ON_ERROR = BooleanSetting(BuildConfig.DEBUG)

        @JvmField
        val DEBUG_STACKTRACE = BooleanSetting(false)
    }
}
