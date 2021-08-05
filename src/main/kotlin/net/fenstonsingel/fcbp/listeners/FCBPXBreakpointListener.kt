package net.fenstonsingel.fcbp.listeners

import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import net.fenstonsingel.fcbp.FCBPManager

class FCBPXBreakpointListener : XBreakpointListener<XLineBreakpoint<*>> {

    override fun breakpointAdded(breakpoint: XLineBreakpoint<*>) = FCBPManager.add(breakpoint)
    override fun breakpointRemoved(breakpoint: XLineBreakpoint<*>) = FCBPManager.remove(breakpoint)
    override fun breakpointChanged(breakpoint: XLineBreakpoint<*>) = FCBPManager.update(breakpoint)

}
