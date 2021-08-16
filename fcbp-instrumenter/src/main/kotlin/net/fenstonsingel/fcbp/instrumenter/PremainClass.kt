package net.fenstonsingel.fcbp.instrumenter

import java.lang.instrument.Instrumentation

object PremainClass {

    @JvmStatic
    fun premain(args: String, instr: Instrumentation) {
        args.length
        instr.allLoadedClasses
    }

}
