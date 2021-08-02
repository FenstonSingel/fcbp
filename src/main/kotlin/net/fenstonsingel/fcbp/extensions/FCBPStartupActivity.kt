package net.fenstonsingel.fcbp.extensions

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import net.fenstonsingel.fcbp.FCBP

class FCBPStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        val breakpointManager = project.getService(XDebuggerManager::class.java).breakpointManager

        // TODO various language support for line breakpoints in the future
        // TODO filter out breakpoints without a condition
        val breakpointType = XDebuggerUtil.getInstance().findBreakpointType(JavaLineBreakpointType::class.java)
        val breakpoints = breakpointManager.getBreakpoints(breakpointType)
        FCBP.projectBreakpoints.getOrPut(breakpointType.id) { mutableSetOf() } += breakpoints
    }

}
