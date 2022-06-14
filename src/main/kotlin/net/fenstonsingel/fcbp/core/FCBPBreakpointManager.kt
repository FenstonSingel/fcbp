package net.fenstonsingel.fcbp.core

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.openapi.project.Project
import net.fenstonsingel.fcbp.JavaLineBreakpoint
import net.fenstonsingel.fcbp.XJavaLineBreakpoint
import net.fenstonsingel.fcbp.asJavaLineBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.toFCBPBreakpoint

class FCBPBreakpointManager(project: Project) {

    private val registeredSessions = mutableListOf<FCBPSession>()

    fun register(session: FCBPSession) { registeredSessions += session }

    fun deregister(session: FCBPSession) { registeredSessions -= session }

    private val breakpointManager by lazy { DebuggerManagerEx.getInstanceEx(project).breakpointManager }

    private val storage = mutableMapOf<XJavaLineBreakpoint, JavaLineBreakpoint>()

    val breakpoints: List<FCBPBreakpoint>
        get() = storage.values.map(JavaLineBreakpoint::toFCBPBreakpoint)

    private fun recordBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        val potentialBreakpoint = breakpointManager.breakpoints.find { bp -> bp.xBreakpoint == xBreakpoint }
        val breakpoint = checkNotNull(potentialBreakpoint?.asJavaLineBreakpoint) {
            "No appropriate JavaLineBreakpoint found for XJavaLineBreakpoint"
        }
        storage[xBreakpoint] = breakpoint

        val fcbpBreakpoint = breakpoint.toFCBPBreakpoint()
        registeredSessions.forEach { session -> session.record(fcbpBreakpoint) }
    }

    private fun forgetBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        val breakpoint = checkNotNull(storage.remove(xBreakpoint)) {
            "Invalid XJavaLineBreakpoint passed to be forgotten or valid JavaLineBreakpoint wasn't properly recorded"
        }

        val fcbpBreakpoint = breakpoint.toFCBPBreakpoint()
        registeredSessions.forEach { session -> session.forget(fcbpBreakpoint) }
    }

    private fun updateBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        val breakpoint = checkNotNull(storage[xBreakpoint]) {
            "Invalid XJavaLineBreakpoint passed to be updated or valid JavaLineBreakpoint wasn't properly recorded"
        }

        val fcbpBreakpoint = breakpoint.toFCBPBreakpoint()
        registeredSessions.forEach { session -> session.update(fcbpBreakpoint) }
    }

    fun addBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (null == xBreakpoint.conditionExpression) return
        recordBreakpoint(xBreakpoint)
    }

    fun removeBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (xBreakpoint !in storage) return
        forgetBreakpoint(xBreakpoint)
    }

    fun changeBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (null != xBreakpoint.conditionExpression) {
            if (xBreakpoint !in storage) recordBreakpoint(xBreakpoint)
            else updateBreakpoint(xBreakpoint)
        } else {
            if (xBreakpoint in storage) forgetBreakpoint(xBreakpoint)
        }
    }

}
