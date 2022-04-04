package net.fenstonsingel.fcbp.core

import com.intellij.debugger.impl.DebuggerSession
import com.intellij.execution.process.ProcessHandler
import net.fenstonsingel.fcbp.listeners.FCBPExecutionListener

/** See [FCBPSession] and [FCBPSession.launch] for info. */
fun DebuggerSession.launchFCBPSession() { FCBPSession.launch(this) }

/** See [FCBPSession] and [FCBPSession.getInstance] for info. */
val DebuggerSession.fcbpSession: FCBPSession?
    get() = FCBPSession.getInstance(this)

/** See [FCBPExecutionListener.getInstrumenterID] for info. */
val FCBPSession.instrumenterID: Int?
    get() {
        val referenceHandler: ProcessHandler = debuggerSession.process.processHandler
        return FCBPExecutionListener.getInstrumenterID(referenceHandler)
    }
