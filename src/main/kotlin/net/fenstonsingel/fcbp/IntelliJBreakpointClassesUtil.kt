package net.fenstonsingel.fcbp

import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiUtil.isInnerClass
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPCondition
import net.fenstonsingel.fcbp.shared.FCBPMethod
import net.fenstonsingel.fcbp.shared.FCBPSourcePosition
import net.fenstonsingel.fcbp.shared.FCBPType
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

/** TODO documentation */
fun JavaLineBreakpoint.toFCBPBreakpoint(): FCBPBreakpoint = ReadAction.compute<FCBPBreakpoint, Nothing> {
    val context = checkNotNull(conditionPsi?.context) { "Condition PSI has no evaluation context" }

    val klass = context.enclosingClass
    val className = klass.binaryName
    checkNotNull(className) { "Class name wasn't resolved for a conditional breakpoint" }

    val method = context.enclosingBehavior
    val methodName = checkNotNull(method.binaryName) { "Method name wasn't resolved for a conditional breakpoint" }
    val methodParameters = when (method) {
        is PsiMethod -> {
            val explicitParameters = method.parameterList.parameters.map { parameter -> parameter.type.binaryName }
            val implicitParameters = mutableListOf<String>()
            if (method.isConstructor && isInnerClass(klass)) {
                val outerClass = checkNotNull(klass.containingClass) { "Inner class doesn't have an outer class" }
                val outerClassName = checkNotNull(outerClass.binaryName) { "Inner class's outer class is nameless" }
                implicitParameters += outerClassName
            }
            implicitParameters + explicitParameters
        }
        is PsiClassInitializer -> emptyList()
        else -> throw IllegalStateException("Property enclosingBehavior returned something other than a behavior")
    }.map { parameterName -> FCBPType(parameterName) }

    val lineNumber = lineIndex + 1
    val lambdaOrdinal = xBreakpoint.properties.lambdaOrdinal

    val conditionBody = xBreakpoint.conditionExpression?.expression
    checkNotNull(conditionBody) { "Relevant conditional breakpoint has no condition (probably a data race)" }

    FCBPBreakpoint(
        FCBPType(className),
        FCBPMethod(methodName, methodParameters),
        FCBPSourcePosition(lineNumber, lambdaOrdinal),
        FCBPCondition(conditionBody)
    )
}
