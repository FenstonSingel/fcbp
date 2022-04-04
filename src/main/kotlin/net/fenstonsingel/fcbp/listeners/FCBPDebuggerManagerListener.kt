package net.fenstonsingel.fcbp.listeners

import com.intellij.debugger.impl.DebuggerManagerListener
import com.intellij.debugger.impl.DebuggerSession
import net.fenstonsingel.fcbp.core.fcbpSession
import net.fenstonsingel.fcbp.core.launchFCBPSession

/**
 * Establishes an interrelation between debugger sessions and FCBP sessions
 * by launching and invalidating FCBP sessions at appropriate moments
 * of the debugger session's lifecycle.
 */
class FCBPDebuggerManagerListener : DebuggerManagerListener {

    /** Launches a corresponding FCBP session for a newly started debugger session. */
    override fun sessionCreated(debuggerSession: DebuggerSession) {
        // TODO do not bother with a FCBP session if there's no Java code to debug
        debuggerSession.launchFCBPSession()
    }

    /** Invalidates the FCBP session (if there's any) of a concluded debugger session. */
    override fun sessionRemoved(debuggerSession: DebuggerSession) {
        debuggerSession.fcbpSession?.invalidate()
    }

}
