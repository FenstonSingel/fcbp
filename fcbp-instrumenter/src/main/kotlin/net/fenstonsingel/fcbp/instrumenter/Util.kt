package net.fenstonsingel.fcbp.instrumenter

import javassist.CtBehavior
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPMethod
import net.fenstonsingel.fcbp.shared.FCBPType

fun CtBehavior.toFCBPMethod(): FCBPMethod = FCBPMethod(name, parameterTypes.map { type -> FCBPType(type.name) })

val FCBPBreakpoint.shouldBeInstrumented: Boolean
    get() {
        if ("false" == condition.body) return false // these are optimized out by Javassist anyways
        // TODO check if more breakpoints should be filtered out immediately
        return true
    }
