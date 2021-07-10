package net.fenstonsingel.fcbp.listeners

import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointListener

class FCBPXBreakpointListener : XBreakpointListener<XBreakpoint<*>> {

    override fun breakpointAdded(breakpoint: XBreakpoint<*>) {
        val a = 1 + 1
    }

    override fun breakpointRemoved(breakpoint: XBreakpoint<*>) {
        val a = 1 + 1
    }

    override fun breakpointChanged(breakpoint: XBreakpoint<*>) {
        val a = 1 + 1
    }

}