package net.fenstonsingel.fcbp.listeners

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.impl.DebuggerManagerListener
import com.intellij.debugger.impl.DebuggerSession
import net.fenstonsingel.fcbp.util.conditionPsi
import net.fenstonsingel.fcbp.util.prepareBreakpointConditionForInstrumentation

/**
 * A listener to manage start-up and shutdown of the FCBP server.
 */
class FCBPDebuggerManagerListener : DebuggerManagerListener {

    /**
     * Start-ups the FCBP server for condition instrumenters to connect to.
     */
    override fun sessionCreated(session: DebuggerSession) {
        val debuggerManager = DebuggerManagerEx.getInstanceEx(session.project)
        val breakpoints = debuggerManager.breakpointManager.breakpoints
        val psis = breakpoints.mapNotNull { breakpoint -> breakpoint.conditionPsi }
        val conditionsToInstrument = psis.map { psi -> prepareBreakpointConditionForInstrumentation(psi) }
        Unit

        // TODO see method description
    }

    /**
     * Shutdowns the FCBP server.
     */
    override fun sessionRemoved(session: DebuggerSession) {
        // TODO see method description
    }

}
