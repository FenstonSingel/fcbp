package net.fenstonsingel.fcbp.listeners

import com.intellij.debugger.impl.DebuggerManagerListener
import com.intellij.debugger.impl.DebuggerSession
import net.fenstonsingel.fcbp.extensions.FCBPPositionManagerFactory

class FCBPDebuggerManagerListener : DebuggerManagerListener {

    override fun sessionAttached(session: DebuggerSession?) {
        // TODO wait for a connection with the instrumentation agent to be established (also, well, establish it, yeah)
        //      (don't forget about the timeout, don't make rookie mistakes)

        // re-append the stub position manager to make sure to intercept all condition evaluations
        session?.process?.positionManager?.appendPositionManager(FCBPPositionManagerFactory.FCBPPositionManager)

        /*
         * it might be useful to schedule re-addition of the stub position manager
         * to deal with other plugins possibly using the same trick
         * (or it might be a really dumb and/or useless idea, think it through beforehand)
         */

        // TODO calculate classes for each conditional breakpoint using CompoundPositionManager

        // TODO
    }

    // just a way to describe exception throwing in a shorter form
    private fun abort(reason: String): Nothing = throw DebuggerAbortException(reason)

    private class DebuggerAbortException(message: String?) : IllegalStateException(message)

}
