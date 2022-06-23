package net.fenstonsingel.javassist.examples.javaagent

import java.lang.instrument.Instrumentation

@Suppress("UNUSED")
internal object PremainClass {

    @JvmStatic
    fun premain(premainArguments: String?, instrumentation: Instrumentation) {
        instrumentation.addTransformer(Transformer)
    }

}
