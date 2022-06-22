package net.fenstonsingel.fcbp.instrumenter

import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPConditionAdded
import net.fenstonsingel.fcbp.shared.FCBPConditionChanged
import net.fenstonsingel.fcbp.shared.FCBPConditionDelegated
import net.fenstonsingel.fcbp.shared.FCBPConditionForgotten
import net.fenstonsingel.fcbp.shared.FCBPConditionInstrumented
import net.fenstonsingel.fcbp.shared.FCBPConditionRemoved
import net.fenstonsingel.fcbp.shared.FCBPDebuggerEvent
import net.fenstonsingel.fcbp.shared.FCBPInitializationCompleted
import net.fenstonsingel.fcbp.shared.FCBPInitializationStarted
import net.fenstonsingel.fcbp.shared.FCBPInstrumenterConnected
import net.fenstonsingel.fcbp.shared.readFCBPPacket
import net.fenstonsingel.fcbp.shared.sendFCBPPacket
import java.io.File
import java.lang.instrument.Instrumentation
import java.net.InetSocketAddress
import java.net.Socket

// TODO implement good logging across the entire instrumenter
class FCBPInstrumenter private constructor(
    private val instrumenterID: Int,
    private val socket: Socket,
    private val instrumentation: Instrumentation
) {

    private val transformer: FCBPTransformer = FCBPTransformer(this)

    private val innerBreakpointsByClassName = mutableMapOf<String, MutableList<FCBPBreakpoint>>()

    val breakpointsByClassName: Map<String, List<FCBPBreakpoint>>
        get() = innerBreakpointsByClassName

    fun transmitConditionStatus(breakpoint: FCBPBreakpoint, status: FCBPConditionStatus) {
        val packet = when (status) {
            FCBPConditionStatus.INSTRUMENTED -> FCBPConditionInstrumented(breakpoint)
            FCBPConditionStatus.DELEGATED -> FCBPConditionDelegated(breakpoint)
            FCBPConditionStatus.FORGOTTEN -> FCBPConditionForgotten(breakpoint)
        }
        socket.sendFCBPPacket(packet)
    }

    var loggingDirectory: File? = null

    private fun initialize() {
        if (!instrumentation.isRetransformClassesSupported) {
            val inoperativeConnectionPacket = FCBPInstrumenterConnected(instrumenterID, isOperative = false)
            socket.sendFCBPPacket(inoperativeConnectionPacket)
            throw IllegalArgumentException("Target JVM doesn't support class retransformation")
        } else {
            instrumentation.addTransformer(transformer, true)
            val operativeConnectionPacket = FCBPInstrumenterConnected(instrumenterID)
            socket.sendFCBPPacket(operativeConnectionPacket)
        }

        val initializationStartedPacket = socket.readFCBPPacket()
        check(initializationStartedPacket is FCBPInitializationStarted) { "Wrong instrumenter initialization start" }
        initializationStartedPacket.loggingDirectory?.let { providedLoggingDirectory ->
            loggingDirectory = File(providedLoggingDirectory).apply { mkdirs() }
        }

        do {
            val packet = socket.readFCBPPacket()
            when (packet) {
                is FCBPConditionAdded -> addBreakpoint(packet.breakpoint)
                is FCBPInitializationCompleted -> Unit // finish the initialization process
                else -> throw IllegalStateException("Incorrect instrumenter initialization process")
            }
        } while (packet !is FCBPInitializationCompleted)
    }

    private fun work() {
        val packet = socket.readFCBPPacket()
        check(packet is FCBPDebuggerEvent) { "Incorrect debugger event packet received" }

        when (packet) {
            is FCBPConditionAdded -> addBreakpoint(packet.breakpoint)
            is FCBPConditionRemoved -> removeBreakpoint(packet.breakpoint)
            is FCBPConditionChanged -> changeBreakpoint(packet.breakpoint)
        }
    }

    private fun addBreakpoint(breakpoint: FCBPBreakpoint) {
        val className = breakpoint.klass.name.replace('.', '/')
        val classBreakpoints = innerBreakpointsByClassName.getOrPut(className, ::mutableListOf)
        check(breakpoint !in classBreakpoints) { "Newly added breakpoint is already accounted for" }

        if (breakpoint.shouldBeInstrumented) {
            classBreakpoints += breakpoint
            retransformClassIfLoaded(className)
        } else {
            transmitConditionStatus(breakpoint, FCBPConditionStatus.DELEGATED)
        }
    }

    private fun removeBreakpoint(breakpoint: FCBPBreakpoint) {
        val className = breakpoint.klass.name.replace('.', '/')
        val classBreakpoints = innerBreakpointsByClassName[className]
        checkNotNull(classBreakpoints) { "Removed breakpoint wasn't accounted for" }
        check(breakpoint in classBreakpoints) { "Removed breakpoint wasn't accounted for" }

        classBreakpoints -= breakpoint
        transmitConditionStatus(breakpoint, FCBPConditionStatus.FORGOTTEN)
        retransformClassIfLoaded(className)
    }

    private fun changeBreakpoint(breakpoint: FCBPBreakpoint) {
        val className = breakpoint.klass.name.replace('.', '/')
        val classBreakpoints = innerBreakpointsByClassName[className]
        checkNotNull(classBreakpoints) { "Changed breakpoint wasn't accounted for" }
        check(breakpoint in classBreakpoints) { "Changed breakpoint wasn't accounted for" }

        classBreakpoints -= breakpoint // remove instance with the old condition
        if (breakpoint.shouldBeInstrumented) classBreakpoints += breakpoint // re-add instance with a new condition
        else transmitConditionStatus(breakpoint, FCBPConditionStatus.DELEGATED)
        retransformClassIfLoaded(className)
    }

    private fun retransformClassIfLoaded(className: String) {
        if (className in transformer.loadedClasses) {
            val klass = Class.forName(className.replace('/', '.'))
            instrumentation.retransformClasses(klass)
        }
    }

    private fun executeSafely(doAction: () -> Unit) {
        try {
            doAction()
        } catch (e: Exception) {
            e.printStackTrace()
            instrumentation.removeTransformer(transformer)
        }
    }

    init { executeSafely { initialize() } }

    private fun run() {
        executeSafely {
            while (!Thread.interrupted()) {
                work()
            }
        }
    }

    companion object {

        private const val serverSocketPort = 14848
        private val serverSocketAddress = InetSocketAddress("localhost", serverSocketPort)

        fun launch(instrumenterID: Int, instrumentation: Instrumentation) {
            val socket = Socket().apply { connect(serverSocketAddress) }
            val instrumenter = FCBPInstrumenter(instrumenterID, socket, instrumentation)
            Thread { instrumenter.run() }.apply { isDaemon = true }.start()
        }

    }

}
