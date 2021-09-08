package net.fenstonsingel.fcbp.listeners

import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.breakpoints.XLineBreakpoint

class FCBPXBreakpointListener : XBreakpointListener<XLineBreakpoint<*>> {

    // TODO update information about conditional breakpoints where it's saved
    override fun breakpointAdded(breakpoint: XLineBreakpoint<*>) = Unit
    override fun breakpointRemoved(breakpoint: XLineBreakpoint<*>) = Unit
    override fun breakpointChanged(breakpoint: XLineBreakpoint<*>) = Unit

}
