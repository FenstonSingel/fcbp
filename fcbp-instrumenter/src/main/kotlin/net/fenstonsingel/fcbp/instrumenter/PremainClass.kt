package net.fenstonsingel.fcbp.instrumenter

import java.lang.instrument.Instrumentation

@Suppress("UNUSED")
object PremainClass {

    @JvmStatic
    fun premain(premainArguments: String?, instrumentation: Instrumentation) {
        if (hasPremainAlreadyBeenStarted) return
        hasPremainAlreadyBeenStarted = true

        FCBPInstrumenterManager(instrumentation).run()
    }

    // this is a godforsaken workaround motivated by
    // a bug in IDEA's Gradle run delegation feature
    // that runs all(?) premain methods twice
    private var hasPremainAlreadyBeenStarted = false

}
