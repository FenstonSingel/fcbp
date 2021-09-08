package net.fenstonsingel.fcbp.instrumenter

import java.lang.instrument.Instrumentation

@Suppress("UNUSED")
object PremainClass {

    @JvmStatic
    fun premain(premainArguments: String?, instrumentation: Instrumentation) {
        if (hasPremainAlreadyBeenStarted) return
        hasPremainAlreadyBeenStarted = true

        // TODO wait for a connection with the debugger to be established (also, well, establish it, yeah)
        //      (don't forget about the timeout, don't make rookie mistakes)

        fun execute(performAction: () -> Unit) {
            try {
                performAction()
            } catch (e: Exception) {
                instrumentation.removeTransformer(FCBPTransformer)
                // TODO if exception is instrumenter's fault, tell the debugger that something went wrong and close the connection

                System.err.println(e)
            }
        }

        execute {
            if (!instrumentation.isRetransformClassesSupported) abort("Target JVM can't retransform classes.")
            instrumentation.addTransformer(FCBPTransformer, true)

            // TODO
        }
    }

    // just a way to describe exception throwing in a shorter form
    private fun abort(reason: String): Nothing = throw InstrumenterAbortException(reason)

    // this is a godforsaken workaround motivated by
    // a bug in IDEA's Gradle run delegation feature
    // that runs all(?) premain methods twice
    private var hasPremainAlreadyBeenStarted = false

    private class InstrumenterAbortException(message: String?) : IllegalStateException(message)

}
