package net.fenstonsingel.fcbp

import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties

typealias XJavaLineBreakpoint = XLineBreakpoint<JavaLineBreakpointProperties>

// the point of this (arguably core property of the file) is that
// sometimes developers of plugins for JVM language (e.g. Kotlin) support
// inherit their classes from (X)JavaLineBreakpoint,
// rendering the type system a bit useless for filtering non-Java breakpoints out
val XBreakpoint<*>.isXJavaLineBreakpoint: Boolean
    get() = this is XLineBreakpoint<*> && type.id == "java-line"

@Suppress("UNCHECKED_CAST")
val XBreakpoint<*>.asXJavaLineBreakpoint: XJavaLineBreakpoint?
    get() = if (isXJavaLineBreakpoint) this as XJavaLineBreakpoint else null

typealias JavaLineBreakpoint = LineBreakpoint<JavaLineBreakpointProperties>

val Breakpoint<*>.isJavaLineBreakpoint: Boolean
    get() = xBreakpoint.isXJavaLineBreakpoint

@Suppress("UNCHECKED_CAST")
val Breakpoint<*>.asJavaLineBreakpoint: JavaLineBreakpoint?
    get() = if (isJavaLineBreakpoint) this as JavaLineBreakpoint else null
