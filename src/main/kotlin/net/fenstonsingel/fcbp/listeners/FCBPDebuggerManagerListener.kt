package net.fenstonsingel.fcbp.listeners

import com.intellij.debugger.impl.DebuggerManagerListener
import com.intellij.debugger.impl.DebuggerSession

/**
 * A listener to manage start-up and shutdown of the FCBP server.
 */
object FCBPDebuggerManagerListener : DebuggerManagerListener {

    /**
     * Starts the FCBP server for condition instrumenters to connect to.
     */
    override fun sessionCreated(session: DebuggerSession) {
        // TODO see method description
    }

    /**
     * Shutdowns the FCBP server.
     */
    override fun sessionRemoved(session: DebuggerSession) {
        // TODO see method description
    }

}
