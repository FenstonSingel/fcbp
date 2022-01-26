package net.fenstonsingel.fcbp.instrumenter

import java.lang.instrument.Instrumentation

class FCBPInstrumenterManager(private val instrumentation: Instrumentation) : Runnable {

    val conditionsByClassName: Map<String, List<BreakpointCondition>> get() = mutableConditionsByClassName

    val methodNamesByBytecodeLabel = mutableMapOf<BytecodeLabel, String>()

    override fun run() = executeSafely {
        check(instrumentation.isRetransformClassesSupported) { "Target JVM can't retransform classes." }
        instrumentation.addTransformer(transformer, true)

        addCondition(
            "net.fenstonsingel.fcbp.testbed.Benchmark",
            BreakpointCondition(71, null, "i % 7 == 0")
        )
    }

    private inline fun executeSafely(performAction: () -> Unit) {
        try {
            performAction()
        } catch (e: RuntimeException) {
            instrumentation.removeTransformer(transformer)
            e.printStackTrace()
        }
    }

    private fun addCondition(dotSeparatedClassName: String, condition: BreakpointCondition) {
        val className = dotSeparatedClassName.replace('.', '/')
        mutableConditionsByClassName.getOrPut(className) { mutableListOf() } += condition
    }

    private val transformer = FCBPTransformer(this)

    private val mutableConditionsByClassName = mutableMapOf<String, MutableList<BreakpointCondition>>()

}
