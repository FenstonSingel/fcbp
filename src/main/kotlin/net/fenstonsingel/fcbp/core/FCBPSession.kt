package net.fenstonsingel.fcbp.core

import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.impl.PrioritizedTask
import com.intellij.openapi.components.service
import com.intellij.util.ThreeState
import com.sun.jdi.Location
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPConditionAdded
import net.fenstonsingel.fcbp.shared.FCBPInitializationCompleted
import net.fenstonsingel.fcbp.shared.FCBPInitializationStarted
import net.fenstonsingel.fcbp.shared.sendFCBPPacket
import java.nio.channels.SocketChannel

/**
 * TODO documentation
 */
class FCBPSession private constructor(val debuggerSession: DebuggerSession) {

    private val fcbpBreakpointManager = debuggerSession.project.service<FCBPBreakpointManager>()

    init {
        fcbpBreakpointManager.register(this)
    }

    private lateinit var instrumenterChannel: SocketChannel

    /** TODO documentation */
    fun initialize(socketChannel: SocketChannel) {
        with(debuggerSession.process) {
            managerThread.invoke(PrioritizedTask.Priority.HIGH) {
                positionManager.appendPositionManager(FCBPPositionManager(this@FCBPSession))
            }
        }
        instrumenterChannel = socketChannel
        val instrumenterLoggingDirectory: String? = debuggerSession.project.basePath?.let { "$it/build/fcbp" }
        instrumenterChannel.sendFCBPPacket(FCBPInitializationStarted(instrumenterLoggingDirectory))
        fcbpBreakpointManager.breakpoints.forEach { breakpoint ->
            val breakpointAddedPacket = FCBPConditionAdded(breakpoint)
            instrumenterChannel.sendFCBPPacket(breakpointAddedPacket)
        }
        instrumenterChannel.sendFCBPPacket(FCBPInitializationCompleted)
    }

    /** TODO documentation */
    fun invalidate() {
        debuggerSessionsToFCBPSessions -= debuggerSession
        fcbpBreakpointManager.deregister(this)
    }

    private val instrumentedBreakpoints = mutableListOf<FCBPBreakpoint>()

    private val delegatedBreakpoints = mutableListOf<FCBPBreakpoint>()

    /** TODO documentation */
    fun unregisterBreakpoint(breakpoint: FCBPBreakpoint) {
        instrumentedBreakpoints -= breakpoint
        delegatedBreakpoints -= breakpoint
    }

    /** TODO documentation */
    fun registerInstrumentedBreakpoint(breakpoint: FCBPBreakpoint) {
        instrumentedBreakpoints += breakpoint
        delegatedBreakpoints -= breakpoint
    }

    /** TODO documentation */
    fun registerDelegatedBreakpoint(breakpoint: FCBPBreakpoint) {
        instrumentedBreakpoints -= breakpoint
        delegatedBreakpoints += breakpoint
    }

    /** TODO documentation */
    fun analyzeBreakpointConditionStatus(location: Location, expression: String): ThreeState {
        val className = location.declaringType().name()
        val lineNumber = location.lineNumber()
        while (!Thread.interrupted()) {
            val isInstrumented = instrumentedBreakpoints.find { bp ->
                bp.klass.name == className && bp.position.lineNumber == lineNumber && bp.condition.body == expression
            }
            if (null != isInstrumented) return ThreeState.YES
            val isDelegated = delegatedBreakpoints.find { bp ->
                bp.klass.name == className && bp.position.lineNumber == lineNumber && bp.condition.body == expression
            }
            if (null != isDelegated) return ThreeState.UNSURE
            Thread.sleep(500L) // TODO neaten this busy-waiting to something more adequate
        }
        return ThreeState.UNSURE // hopefully we should never end up here
    }

    companion object {

        private val debuggerSessionsToFCBPSessions = mutableMapOf<DebuggerSession, FCBPSession>()

        /** TODO documentation */
        fun launch(debuggerSession: DebuggerSession) {
            debuggerSessionsToFCBPSessions[debuggerSession] = FCBPSession(debuggerSession)
        }

        /** TODO documentation */
        fun getInstance(debuggerSession: DebuggerSession): FCBPSession? =
            debuggerSessionsToFCBPSessions[debuggerSession]

        /** TODO documentation */
        fun findByInstrumenterID(instrumenterID: Int): FCBPSession? =
            debuggerSessionsToFCBPSessions.values.find { fcbpSession -> fcbpSession.instrumenterID == instrumenterID }

    }

}
