package net.fenstonsingel.fcbp.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import net.fenstonsingel.fcbp.XJavaLineBreakpoint
import net.fenstonsingel.fcbp.core.FCBPBreakpointManager
import net.fenstonsingel.fcbp.isXJavaLineBreakpoint

/**
 * Delegates events related to project breakpoints to the project's FCBP breakpoint manager
 * (filtering out hopefully-all non-Java breakpoints preliminarily).
 */
class FCBPXBreakpointListener(project: Project) : XBreakpointListener<XJavaLineBreakpoint> {

    private val fcbpBreakpointManager = project.service<FCBPBreakpointManager>()

    /** Delegates to [FCBPBreakpointManager.addBreakpoint]. */
    override fun breakpointAdded(xBreakpoint: XJavaLineBreakpoint) {
        if (!xBreakpoint.isXJavaLineBreakpoint) return
        fcbpBreakpointManager.addBreakpoint(xBreakpoint)
    }

    /** Delegates to [FCBPBreakpointManager.removeBreakpoint]. */
    override fun breakpointRemoved(xBreakpoint: XJavaLineBreakpoint) {
        if (!xBreakpoint.isXJavaLineBreakpoint) return
        fcbpBreakpointManager.removeBreakpoint(xBreakpoint)
    }

    /** Delegates to [FCBPBreakpointManager.updateBreakpoint]. */
    override fun breakpointChanged(xBreakpoint: XJavaLineBreakpoint) {
        if (!xBreakpoint.isXJavaLineBreakpoint) return
        fcbpBreakpointManager.changeBreakpoint(xBreakpoint)
    }

}
