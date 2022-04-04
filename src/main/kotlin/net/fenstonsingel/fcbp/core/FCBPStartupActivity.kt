package net.fenstonsingel.fcbp.core

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.breakpoints.XBreakpoint
import net.fenstonsingel.fcbp.asXJavaLineBreakpoint

class FCBPStartupActivity : StartupActivity, DumbAware {

    init { FCBPServer.launch() }

    override fun runActivity(project: Project) {
        // extract all persistent breakpoint data to the project's FCBP breakpoint manager
        val fcbpBreakpointManager = project.service<FCBPBreakpointManager>()
        val xBreakpoints = ReadAction.compute<Array<XBreakpoint<*>>, Nothing> {
            project.service<XDebuggerManager>().breakpointManager.allBreakpoints
        }
        xBreakpoints.mapNotNull(XBreakpoint<*>::asXJavaLineBreakpoint).forEach { xBreakpoint ->
            fcbpBreakpointManager.addBreakpoint(xBreakpoint)
        }
    }

}
