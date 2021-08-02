package net.fenstonsingel.fcbp.listeners

import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import net.fenstonsingel.fcbp.FCBP

class FCBPXBreakpointListener : XBreakpointListener<XBreakpoint<*>> {

    override fun breakpointAdded(breakpoint: XBreakpoint<*>) {
        // TODO filter out breakpoints without a condition
        if (!isSpeedUpImplemented(breakpoint)) return

        breakpoint as XLineBreakpoint
        FCBP.projectBreakpoints.getOrPut(breakpoint.type.id) { mutableSetOf() } += breakpoint

        // TODO notify the instrumentation agent if the new breakpoint has a condition
    }

    override fun breakpointRemoved(breakpoint: XBreakpoint<*>) {
        FCBP.projectBreakpoints[breakpoint.type.id]?.remove(breakpoint)

        // TODO think about whether removing deleted breakpoint's instrumentation is a reasonable thing to do
        // note: if instrumentation is not removed after the breakpoint is gone,
        // something must cache all instrumentations ever made so placing the breakpoint
        // back doesn't end up in multiple nested instrumentations
    }

    override fun breakpointChanged(breakpoint: XBreakpoint<*>) {
        if (!isSpeedUpImplemented(breakpoint)) return

        // TODO account for breakpoints that didn't have a condition and got one

        // TODO notify the instrumentation agent if a condition of the breakpoint has changed
    }

    private fun isSpeedUpImplemented(breakpoint: XBreakpoint<*>): Boolean {
        // TODO checking of appropriateness of the breakpoint for various languages
        // note: kotlin fu— bad people inherited java's line breakpoint type, so
        // checking the [breakpoint] type's [Java] type is out of the window now
        // for now it seems like ids are the only valid way
        return breakpoint.type.id == "java-line"
    }

}
