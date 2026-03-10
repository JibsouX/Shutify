package io.github.chsbuffer.revancedxposed.spotify.misc.widgets

import de.robv.android.xposed.XC_MethodReplacement
import io.github.chsbuffer.revancedxposed.spotify.SpotifyHook

fun SpotifyHook.FixThirdPartyLaunchersWidgets() {
    ::canBindAppWidgetPermissionFingerprint.hookMethod(XC_MethodReplacement.returnConstant(true))
}