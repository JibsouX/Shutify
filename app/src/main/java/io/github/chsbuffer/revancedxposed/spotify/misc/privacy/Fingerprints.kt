package io.github.chsbuffer.revancedxposed.spotify.misc.privacy

import io.github.chsbuffer.revancedxposed.findMethodDirect
import io.github.chsbuffer.revancedxposed.fingerprint
import java.lang.reflect.Modifier

val shareCopyUrlFingerprint = findMethodDirect {
    runCatching {
        fingerprint {
            returns("Ljava/lang/Object;")
            parameters("Ljava/lang/Object;")
            strings("clipboard", "Spotify Link")
            methodMatcher { name = "invokeSuspend" }
        }
    }.getOrElse {
        fingerprint {
            returns("Ljava/lang/Object;")
            parameters("Ljava/lang/Object;")
            strings("clipboard", "createNewSession failed")
            methodMatcher { name = "apply" }
        }
    }
}

val formatAndroidShareSheetUrlFingerprint = findMethodDirect {
    runCatching {
        findMethod {
            matcher {
                returnType("java.lang.String")
                addUsingNumber('\n'.code)
                modifiers = Modifier.PUBLIC or Modifier.STATIC
                paramTypes(null, "java.lang.String")
            }
        }.single {
            // exclude
            // `(PlayerState, String) -> String` usingNumbers(1, 10); usingStrings("")
            !it.usingStrings.contains("")
        }
    }.getOrElse {
        findMethod {
            matcher {
                returnType("java.lang.String")
                addUsingNumber('\n'.code)
                modifiers = Modifier.PUBLIC
                paramTypes("com.spotify.share.social.sharedata.ShareData", "java.lang.String")
            }
        }.single {
            // exclude
            // `(PlayerState, String) -> String` usingNumbers(1, 10); usingStrings("")
            !it.usingStrings.contains("")
        }
    }

}