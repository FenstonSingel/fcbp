package net.fenstonsingel.fcbp

import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import net.fenstonsingel.fcbp.utilities.LineBreakpointRecords

object FCBPManager {

    val projectConditionalBreakpoints: Map<String, Set<XLineBreakpoint<*>>>
        get() = projectCBStorage

    fun add(breakpoint: XLineBreakpoint<*>) {
        if (!isSpeedUpImplemented(breakpoint)) return

        // if the breakpoint isn't conditional, don't bother
        if (breakpoint.conditionExpression == null) return
        // if this is the first breakpoint of its type, create appropriate records for it
        val breakpointRecordsForRightType = projectCBStorage.getOrPut(breakpoint.type.id) { mutableSetOf() }

        breakpointRecordsForRightType.record(breakpoint)
    }

    fun remove(breakpoint: XLineBreakpoint<*>) {
        if (!isSpeedUpImplemented(breakpoint)) return

        // if there are no recorded breakpoints with the right type, don't bother
        val breakpointRecordsForRightType = projectCBStorage[breakpoint.type.id] ?: return
        // if this exact breakpoint wasn't recorded, don't bother either
        if (breakpoint !in breakpointRecordsForRightType) return

        breakpointRecordsForRightType.forget(breakpoint)
    }

    fun update(breakpoint: XLineBreakpoint<*>) {
        if (!isSpeedUpImplemented(breakpoint)) return

        val breakpointRecordsForRightType = projectCBStorage[breakpoint.type.id]
        if (breakpointRecordsForRightType == null || breakpoint !in breakpointRecordsForRightType) {
            // breakpoint wasn't recorded, but it might be conditional now, so try to record it
            add(breakpoint)
        } else {
            if (breakpoint.conditionExpression == null) {
                // breakpoint was recorded, but it's not conditional anymore — forget it
                breakpointRecordsForRightType.forget(breakpoint)
            } else {
                // breakpoint was recorded and is still conditional — there's nothing to do
            }
        }
    }

    private val projectCBStorage = mutableMapOf<String, LineBreakpointRecords>()

    private fun isSpeedUpImplemented(breakpoint: XLineBreakpoint<*>): Boolean {
        // TODO add support for languages other than Java
        // note: kotlin fu— people inherited java's breakpoint types in several places,
        // so checking the breakpoint type's Java class is out of the window now
        // for now it seems like breakpoint type ids are the only valid way
        return breakpoint.type.id == "java-line"
    }

    private fun LineBreakpointRecords.record(breakpoint: XLineBreakpoint<*>) {
        add(breakpoint)
        // TODO as a side-effect, notify the instrumentation agent (if it's running)
        //       there's a new condition to instrument
    }

    private fun LineBreakpointRecords.forget(breakpoint: XLineBreakpoint<*>) {
        remove(breakpoint)
        // TODO as a side-effect, notify the instrumentation agent (if it's running)
        //       one of the conditions has to be un-instrumented
    }

    // FIXME debugging galore, erase when it's no longer needed
    init {
        Thread {
            while (true)
                Unit
        }.start()
    }

}
