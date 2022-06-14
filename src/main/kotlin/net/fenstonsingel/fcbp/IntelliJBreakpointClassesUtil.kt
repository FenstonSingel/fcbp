package net.fenstonsingel.fcbp

import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedNamedElement
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

private data class PreparedCondition(
    private val psi: JavaCodeFragment,
    private val qualifiersToAdd: List<Pair<String, PsiJavaCodeReferenceElement>>
) {
    override fun toString(): String {
        /*
         * PSI modifications during visitor execution prevent other nodes from being visited,
         * so the modifications are performed post factum
         */
        qualifiersToAdd.forEach { (qualifier, reference) -> reference.addQualifier(qualifier) }

        return psi.text
    }
}

// TODO implement more modifications
private fun prepareConditionForJavassist(condition: JavaCodeFragment): PreparedCondition {
    val preprocessor = object : JavaRecursiveElementVisitor() {
        fun execute(): PreparedCondition {
            condition.accept(this)
            return PreparedCondition(condition, qualifiersToAdd)
        }

        override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
            super.visitReferenceElement(reference) // maintaining recursion

            // only leftmost reference of any dot-qualified expression is of any interest to us
            if (null != reference.qualifier) return

            val target = reference.resolve()
            checkNotNull(target) { "Unresolved entity $reference found in a breakpoint condition $condition" }

            when {
                target is PsiQualifiedNamedElement -> { // resolving types to their FQN (e.g. in static method calls)
                    val qualifier = target.qualifiedName?.substringBeforeLast('.')
                    if (null != qualifier) qualifiersToAdd += qualifier to reference
                }
            }
        }

        private val qualifiersToAdd = mutableListOf<Pair<String, PsiJavaCodeReferenceElement>>()
    }

    return preprocessor.execute()
}

private data class FCBPBreakpointInitializationData(
    val className: String,
    val methodName: String,
    val methodParameters: List<String>,
    val lineNumber: Int,
    val lambdaOrdinal: Int?,
    val condition: PreparedCondition
) {
    fun toFCBPBreakpoint(): FCBPBreakpoint = FCBPBreakpoint(
        FCBPType(className),
        FCBPMethod(methodName, methodParameters.map { type -> FCBPType(type) }),
        FCBPSourcePosition(lineNumber, lambdaOrdinal),
        FCBPCondition(condition.toString())
    )
}

/** TODO documentation */
fun JavaLineBreakpoint.toFCBPBreakpoint(): FCBPBreakpoint {
    val initializationData = ReadAction.compute<FCBPBreakpointInitializationData, Nothing> {
        val conditionPsi = conditionPsi
        val context = checkNotNull(conditionPsi.context) { "Condition PSI has no evaluation context" }

        val klass = context.enclosingClass
        val className = klass.binaryName
        checkNotNull(className) { "Class name wasn't resolved for a breakpoint" }

        val method = context.enclosingBehavior
        val methodName = checkNotNull(method.binaryName) { "Method name wasn't resolved for a breakpoint" }
        val methodParameters = when (method) {
            is PsiMethod -> {
                val parameters = method.parameterList.parameters.map { parameter -> parameter.type.binaryName }
                if (method.isConstructor && isInnerClass(klass)) {
                    val outerClass = checkNotNull(klass.containingClass) { "Inner class doesn't have an outer class" }
                    val outerClassName = checkNotNull(outerClass.binaryName) { "Inner class's outer class is nameless" }
                    listOf(outerClassName) + parameters
                } else {
                    parameters
                }
            }
            is PsiClassInitializer ->
                emptyList()
            else ->
                throw IllegalStateException("Property enclosingBehavior returned something other than a behavior")
        }

        val lineNumber = lineIndex + 1
        val lambdaOrdinal: Int? = xBreakpoint.properties.lambdaOrdinal

        val preparedCondition = prepareConditionForJavassist(conditionPsi)

        FCBPBreakpointInitializationData(
            className, methodName, methodParameters, lineNumber, lambdaOrdinal, preparedCondition
        )
    }

    return initializationData.toFCBPBreakpoint()
}
