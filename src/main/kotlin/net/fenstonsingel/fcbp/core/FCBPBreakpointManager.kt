package net.fenstonsingel.fcbp.core

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.openapi.project.Project
import net.fenstonsingel.fcbp.JavaLineBreakpoint
import net.fenstonsingel.fcbp.XJavaLineBreakpoint
import net.fenstonsingel.fcbp.asJavaLineBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.toFCBPBreakpoint

/**
 * TODO documentation
 */
class FCBPBreakpointManager(project: Project) {

    private val registeredSessions = mutableListOf<FCBPSession>()

    /** TODO documentation */
    fun register(session: FCBPSession) { registeredSessions += session }

    /** TODO documentation */
    fun deregister(session: FCBPSession) { registeredSessions -= session }

    private val breakpointManager by lazy { DebuggerManagerEx.getInstanceEx(project).breakpointManager }

    private val storage = mutableMapOf<XJavaLineBreakpoint, JavaLineBreakpoint>()

    /** TODO documentation */
    val breakpoints: List<FCBPBreakpoint>
        get() = storage.values.map(JavaLineBreakpoint::toFCBPBreakpoint)

    private fun recordBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        val potentialBreakpoint = breakpointManager.breakpoints.find { bp -> bp.xBreakpoint == xBreakpoint }
        val breakpoint = checkNotNull(potentialBreakpoint?.asJavaLineBreakpoint) {
            "No appropriate JavaLineBreakpoint found for XJavaLineBreakpoint"
        }
        storage[xBreakpoint] = breakpoint

        Unit // TODO notify all FCBP sessions
    }

    private fun forgetBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        val breakpoint = checkNotNull(storage.remove(xBreakpoint)) {
            "Invalid XJavaLineBreakpoint passed to be forgotten or valid JavaLineBreakpoint wasn't properly recorded"
        }

        Unit // TODO notify all FCBP sessions
    }

    private fun updateBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        val breakpoint = checkNotNull(storage[xBreakpoint]) {
            "Invalid XJavaLineBreakpoint passed to be updated or valid JavaLineBreakpoint wasn't properly recorded"
        }

        Unit // TODO notify all FCBP sessions
    }

    /** TODO documentation */
    fun addBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (null == xBreakpoint.conditionExpression) return
        recordBreakpoint(xBreakpoint)
    }

    /** TODO documentation */
    fun removeBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (xBreakpoint !in storage) return
        forgetBreakpoint(xBreakpoint)
    }

    /** TODO documentation */
    fun changeBreakpoint(xBreakpoint: XJavaLineBreakpoint) {
        if (null != xBreakpoint.conditionExpression) {
            if (xBreakpoint !in storage) recordBreakpoint(xBreakpoint)
            else updateBreakpoint(xBreakpoint)
        } else {
            if (xBreakpoint in storage) forgetBreakpoint(xBreakpoint)
        }
    }

}
