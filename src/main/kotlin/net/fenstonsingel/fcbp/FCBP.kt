package net.fenstonsingel.fcbp

import com.intellij.xdebugger.breakpoints.XLineBreakpoint

object FCBP {

    val projectBreakpoints = mutableMapOf<String, MutableSet<XLineBreakpoint<*>>>()

    // debugging galore
    init {
        Thread {
            while (true) {
                1 + 1
            }
        }.start()
    }

}
