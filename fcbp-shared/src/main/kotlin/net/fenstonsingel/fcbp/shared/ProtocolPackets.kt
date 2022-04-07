package net.fenstonsingel.fcbp.shared

import kotlinx.serialization.Serializable

@Serializable
sealed class FCBPPacket

@Serializable
data class FCBPInstrumenterConnected(val instrumenterID: Int, val isOperative: Boolean = true) : FCBPPacket()

@Serializable
data class FCBPInitializationStarted(val loggingDirectory: String?) : FCBPPacket()

@Serializable
object FCBPInitializationCompleted : FCBPPacket()

@Serializable
sealed class FCBPDebuggerEvent : FCBPPacket() {
    abstract val breakpoint: FCBPBreakpoint
}

@Serializable
data class FCBPConditionAdded(override val breakpoint: FCBPBreakpoint) : FCBPDebuggerEvent()

@Serializable
data class FCBPConditionRemoved(override val breakpoint: FCBPBreakpoint) : FCBPDebuggerEvent()

@Serializable
data class FCBPConditionChanged(override val breakpoint: FCBPBreakpoint) : FCBPDebuggerEvent()

@Serializable
sealed class FCBPInstrumenterEvent : FCBPPacket() {
    abstract val breakpoint: FCBPBreakpoint
}

@Serializable
data class FCBPConditionInstrumented(override val breakpoint: FCBPBreakpoint) : FCBPInstrumenterEvent()

@Serializable
data class FCBPConditionDelegated(override val breakpoint: FCBPBreakpoint) : FCBPInstrumenterEvent()
