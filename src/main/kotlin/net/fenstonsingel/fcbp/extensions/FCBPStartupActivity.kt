package net.fenstonsingel.fcbp.extensions

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import net.fenstonsingel.fcbp.FCBPManager

class FCBPStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        val breakpointManager = project.getService(XDebuggerManager::class.java).breakpointManager
        val allLineBreakpoints = breakpointManager.allBreakpoints.filterIsInstance<XLineBreakpoint<*>>()
        allLineBreakpoints.forEach { breakpoint -> FCBPManager.add(breakpoint) }
    }

}
