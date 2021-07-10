package net.fenstonsingel.fcbp.extensions

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.xdebugger.XDebuggerManager

class FCBPStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        val breakpointManager = project.getComponent(XDebuggerManager::class.java).breakpointManager
        // TODO various language support for line breakpoints in the future
        val breakpoints = breakpointManager.getBreakpoints(JavaLineBreakpointType::class.java)

        val a = 1 + 1
    }

}