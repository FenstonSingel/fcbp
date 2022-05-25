package net.fenstonsingel.fcbp.listeners

import com.intellij.execution.ExecutionListener
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.Disposable
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger

/*
 * This is a workaround solution to the "pass an FCBP instrumenter's -javaagent CMD argument
 * automatically at launch to the Java program being debugged" problem. It involves redacting
 * user data directly (twice), so it's rather hacky. It also only works if IDEA runs debugging
 * tasks itself instead of delegating it to Gradle. Not all run configurations involving Java
 * code might be supported.
 *
 * Easier and/or more general and/or more correct solutions might exist. If they don't, it might
 * make sense to expand the IntelliJ Platform accordingly. However, this functionality was not
 * the focus of the FCBP project and, as such, this implementation has been deemed acceptable
 * for the time being.
 */

// TODO make this work when IDEA delegates debug activities to Gradle
// TODO support more run configurations (i.e. Gradle, Spring, etc.)
class FCBPExecutionListener : ExecutionListener, Disposable {

    private val instrumenterIDsByModifiedRunProfiles = mutableMapOf<RunProfile, Int>()

    private fun RunProfile.injectFCBPInstrumenter() {
        val instrumenterID = vacantInstrumenterID
        val instrumenterVMArgument = getInstrumenterVMArgument(instrumenterID)
        when (this) {
            is ApplicationConfiguration ->
                vmParameters = vmParameters.let { userArguments ->
                    if (null != userArguments) "$instrumenterVMArgument $userArguments" else instrumenterVMArgument
                }
            // add more configurations here
            else -> return
        }
        instrumenterIDsByModifiedRunProfiles[this] = instrumenterID
    }

    private fun RunProfile.withdrawFCBPInstrumenter() {
        when (this) {
            is ApplicationConfiguration ->
                vmParameters = vmParameters.removeInstrumenterVMArgument()
            // add more configurations here
            else -> return
        }
        instrumenterIDsByModifiedRunProfiles -= this
    }

    private fun RunProfile.registerFCBPInstrumenter(referenceHandler: ProcessHandler) {
        val instrumenterID = instrumenterIDsByModifiedRunProfiles[this] ?: return
        instrumenterIDsByReferenceHandlers[referenceHandler] = instrumenterID
        withdrawFCBPInstrumenter()
    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
        if ("Debug" != executorId) return
        env.runProfile.injectFCBPInstrumenter()
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        if ("Debug" != executorId) return
        env.runProfile.registerFCBPInstrumenter(handler)
    }

    override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
        if ("Debug" != executorId) return
        instrumenterIDsByReferenceHandlers -= handler
    }

    override fun dispose() {
        instrumenterIDsByModifiedRunProfiles.keys.forEach { modifiedRunProfile ->
            modifiedRunProfile.withdrawFCBPInstrumenter()
        }
    }

    companion object {

        private val instrumenterIDsByReferenceHandlers = mutableMapOf<ProcessHandler, Int>()

        fun getInstrumenterID(referenceHandler: ProcessHandler): Int? =
            instrumenterIDsByReferenceHandlers[referenceHandler]

        // kotlin.random.Random's companion object refuses to initialize here so java.util.Random is used
        private val idSource = AtomicInteger(Random().nextInt(4096))
        private val vacantInstrumenterID get() = idSource.incrementAndGet()

        // FIXME the FCBP plugin version is hard-coded
        private const val instrumenterJarName = "fcbp/lib/instrumenter-0.0.0-all.jar"
        private val instrumenterJarPath get() = "${System.getProperty("idea.plugins.path")}/$instrumenterJarName"

        private fun getInstrumenterVMArgument(id: String) = "-javaagent:\"$instrumenterJarPath\"=\"$id\""
        private fun getInstrumenterVMArgument(id: Int) = getInstrumenterVMArgument(id.toString())

        private val instrumenterVMArgumentRegex = getInstrumenterVMArgument("\\d+").toRegex()

        private fun String?.removeInstrumenterVMArgument() =
            if (null != this) replace(instrumenterVMArgumentRegex, "").trim().ifEmpty { null }
            else null

    }

}
