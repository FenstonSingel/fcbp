package net.fenstonsingel.fcbp.instrumenter

import java.lang.instrument.Instrumentation

@Suppress("UNUSED")
object PremainClass {

    @JvmStatic
    fun premain(arguments: String?, instrumentation: Instrumentation) {
        if (hasPremainAlreadyBeenStarted) return
        hasPremainAlreadyBeenStarted = true

        try {
            val instrumenterID = checkNotNull(arguments?.toIntOrNull()) { "FCBP instrumenter ID wasn't provided" }
            FCBPInstrumenter.launch(instrumenterID, instrumentation)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // this is a godforsaken workaround motivated by
    // a bug in IDEA's Gradle run delegation feature
    // that runs all(?) premain methods twice
    private var hasPremainAlreadyBeenStarted = false

}
