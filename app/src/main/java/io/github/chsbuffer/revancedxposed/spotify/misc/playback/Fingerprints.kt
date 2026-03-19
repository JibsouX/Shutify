package io.github.chsbuffer.revancedxposed.spotify.misc.playback

import io.github.chsbuffer.revancedxposed.S
import io.github.chsbuffer.revancedxposed.findMethodDirect
import io.github.chsbuffer.revancedxposed.findMethodListDirect

val adBreakContextFingerprint = findMethodDirect {
    findMethod {
        matcher {
            declaredClass(S.P1)
            name(S.P2)
        }
    }.single()
}

val adTrackBuilderFingerprints = findMethodListDirect {
    findMethod {
        matcher {
            name(S.P3)
            addParamType(S.P4)
        }
    }.filter { m ->
        val c = m.className
        c.contains(S.P5) || c.contains(S.P6)
    }
}
