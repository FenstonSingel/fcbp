package net.fenstonsingel.fcbp

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiQualifiedNamedElement
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPCondition
import net.fenstonsingel.fcbp.shared.FCBPMethod
import net.fenstonsingel.fcbp.shared.FCBPSourcePosition
import net.fenstonsingel.fcbp.shared.FCBPType

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

private data class FCBPBreakpointData(
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

fun JavaLineBreakpoint.toFCBPBreakpoint(): FCBPBreakpoint {
    val breakpointData = ReadAction.compute<FCBPBreakpointData, Nothing> {
        val conditionPsi = conditionPsi
        val context = checkNotNull(conditionPsi.context) { "Condition PSI has no evaluation context" }

        val klass = context.enclosingClass
        val className = klass.binaryName
        checkNotNull(className) { "Class name wasn't resolved for a breakpoint" }

        val method = context.enclosingBehavior
        val methodName = checkNotNull(method.binaryName) { "Method name wasn't resolved for a breakpoint" }
        val methodParameters = (klass to method).parameterTypesNames

        val lineNumber = lineIndex + 1
        val lambdaOrdinal: Int? = xBreakpoint.properties.lambdaOrdinal

        val preparedCondition = prepareConditionForJavassist(conditionPsi)

        FCBPBreakpointData(className, methodName, methodParameters, lineNumber, lambdaOrdinal, preparedCondition)
    }

    return breakpointData.toFCBPBreakpoint()
}
