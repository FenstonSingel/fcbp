package net.fenstonsingel.fcbp.instrumenter

import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPConditionAdded
import net.fenstonsingel.fcbp.shared.FCBPConditionDelegated
import net.fenstonsingel.fcbp.shared.FCBPConditionInstrumented
import net.fenstonsingel.fcbp.shared.FCBPInitializationCompleted
import net.fenstonsingel.fcbp.shared.FCBPInstrumenterConnected
import net.fenstonsingel.fcbp.shared.readFCBPPacket
import net.fenstonsingel.fcbp.shared.sendFCBPPacket
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

    fun tellBreakpointStatusToDebugger(breakpoint: FCBPBreakpoint, isInstrumented: Boolean) {
        val packet = if (isInstrumented) FCBPConditionInstrumented(breakpoint) else FCBPConditionDelegated(breakpoint)
        socket.sendFCBPPacket(packet)
    }

    private fun executeSafely(doAction: () -> Unit) {
        try {
            doAction()
        } catch (e: Exception) {
            e.printStackTrace()
            instrumentation.removeTransformer(transformer)
        }
    }

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

        do {
            val packet = socket.readFCBPPacket()
            if (packet !is FCBPConditionAdded) continue
            if (packet.breakpoint.shouldBeInstrumented) {
                val className = packet.breakpoint.klass.name.replace('.', '/')
                innerBreakpointsByClassName.getOrPut(className, ::mutableListOf) += packet.breakpoint
            } else {
                tellBreakpointStatusToDebugger(packet.breakpoint, isInstrumented = false)
            }
        } while (packet !is FCBPInitializationCompleted)
    }

    init {
        executeSafely { initialize() }
    }

    private fun work() {
        val packet = socket.readFCBPPacket()
        // TODO support breakpoint events after starting the program under debug
    }

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
