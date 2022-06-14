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

class FCBPSession private constructor(val debuggerSession: DebuggerSession) {

    private val fcbpBreakpointManager = debuggerSession.project.service<FCBPBreakpointManager>()

    private lateinit var instrumenterChannel: SocketChannel

    fun initialize(socketChannel: SocketChannel) {
        instrumenterChannel = socketChannel

        // inject a PositionManager that will prevent debugger-side evaluation of instrumented conditions
        with(debuggerSession.process) {
            managerThread.invoke(PrioritizedTask.Priority.HIGH) {
                positionManager.appendPositionManager(FCBPPositionManager(this@FCBPSession))
            }
        }

        // transfer initialization info to the instrumenter
        val instrumenterLoggingDirectory = debuggerSession.project.basePath?.let { "$it/build/fcbp" }
        instrumenterChannel.sendFCBPPacket(FCBPInitializationStarted(instrumenterLoggingDirectory))

        // extract all current breakpoints and register to FCBPBreakpointManager for breakpoint updates
        val currentBreakpoints = fcbpBreakpointManager.breakpoints
        fcbpBreakpointManager.register(this)

        // transfer all current breakpoints to the instrumenter
        currentBreakpoints.forEach { breakpoint ->
            val breakpointAddedPacket = FCBPConditionAdded(breakpoint)
            instrumenterChannel.sendFCBPPacket(breakpointAddedPacket)
        }
        instrumenterChannel.sendFCBPPacket(FCBPInitializationCompleted)
    }

    fun invalidate() {
        debuggerSessionsToFCBPSessions -= debuggerSession
        fcbpBreakpointManager.deregister(this)
    }

    private val instrumentedBreakpoints = mutableListOf<FCBPBreakpoint>()

    private val delegatedBreakpoints = mutableListOf<FCBPBreakpoint>()

    fun registerInstrumentedBreakpoint(breakpoint: FCBPBreakpoint) {
        instrumentedBreakpoints += breakpoint
        delegatedBreakpoints -= breakpoint
    }

    fun registerDelegatedBreakpoint(breakpoint: FCBPBreakpoint) {
        instrumentedBreakpoints -= breakpoint
        delegatedBreakpoints += breakpoint
    }

    fun unregisterBreakpoint(breakpoint: FCBPBreakpoint) {
        instrumentedBreakpoints -= breakpoint
        delegatedBreakpoints -= breakpoint
    }

    fun analyzeBreakpointConditionStatus(location: Location): ThreeState {
        val className = location.declaringType().name()
        val lineNumber = location.lineNumber()
        while (!Thread.interrupted()) {
            val isInstrumented = instrumentedBreakpoints.find { bp ->
                bp.klass.name == className && bp.position.lineNumber == lineNumber
            }
            if (null != isInstrumented) return ThreeState.YES
            val isDelegated = delegatedBreakpoints.find { bp ->
                bp.klass.name == className && bp.position.lineNumber == lineNumber
            }
            if (null != isDelegated) return ThreeState.UNSURE
            Thread.sleep(500L) // TODO neaten this busy-waiting to something more adequate
        }
        return ThreeState.UNSURE // hopefully we should never end up here
    }

    companion object {

        private val debuggerSessionsToFCBPSessions = mutableMapOf<DebuggerSession, FCBPSession>()

        fun launch(debuggerSession: DebuggerSession) {
            debuggerSessionsToFCBPSessions[debuggerSession] = FCBPSession(debuggerSession)
        }

        fun getInstance(debuggerSession: DebuggerSession): FCBPSession? =
            debuggerSessionsToFCBPSessions[debuggerSession]

        fun findByInstrumenterID(instrumenterID: Int): FCBPSession? =
            debuggerSessionsToFCBPSessions.values.find { fcbpSession -> fcbpSession.instrumenterID == instrumenterID }

    }

}
