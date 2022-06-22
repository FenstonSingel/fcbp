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

    private fun add(xBreakpoint: XJavaLineBreakpoint) {
        val potentialBreakpoint = breakpointManager.breakpoints.find { bp -> bp.xBreakpoint == xBreakpoint }
        val breakpoint = checkNotNull(potentialBreakpoint?.asJavaLineBreakpoint) {
            "No appropriate JavaLineBreakpoint found for XJavaLineBreakpoint"
        }
        storage[xBreakpoint] = breakpoint

        val fcbpBreakpoint = breakpoint.toFCBPBreakpoint()
        registeredSessions.forEach { session -> session.add(fcbpBreakpoint) }
    }

    private fun remove(xBreakpoint: XJavaLineBreakpoint) {
        val breakpoint = checkNotNull(storage.remove(xBreakpoint)) {
            "Invalid XJavaLineBreakpoint passed to be removed or valid JavaLineBreakpoint wasn't properly recorded"
        }

        val fcbpBreakpoint = breakpoint.toFCBPBreakpoint()
        registeredSessions.forEach { session -> session.remove(fcbpBreakpoint) }
    }

    private fun change(xBreakpoint: XJavaLineBreakpoint) {
        val breakpoint = checkNotNull(storage[xBreakpoint]) {
            "Invalid XJavaLineBreakpoint passed to be changed or valid JavaLineBreakpoint wasn't properly recorded"
        }

        val fcbpBreakpoint = breakpoint.toFCBPBreakpoint()
        registeredSessions.forEach { session -> session.change(fcbpBreakpoint) }
    }

    fun addBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (null == xBreakpoint.conditionExpression) return
        add(xBreakpoint)
    }

    fun removeBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (xBreakpoint !in storage) return
        remove(xBreakpoint)
    }

    fun changeBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (null != xBreakpoint.conditionExpression) {
            if (xBreakpoint !in storage) add(xBreakpoint)
            else change(xBreakpoint)
        } else {
            if (xBreakpoint in storage) remove(xBreakpoint)
        }
    }

}
