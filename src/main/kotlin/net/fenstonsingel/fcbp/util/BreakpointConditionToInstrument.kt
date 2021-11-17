package net.fenstonsingel.fcbp.util

import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiQualifiedNamedElement
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiUtil

sealed interface ExtractedBreakpointConditionParameter

class LocalVariableReference(val typeFQN: String, val name: String) : ExtractedBreakpointConditionParameter {
    override fun toString(): String = "$typeFQN $name"
}
class ThisReference(val classFQN: String) : ExtractedBreakpointConditionParameter {
    override fun toString(): String = "$classFQN thisRef"
}
class OuterThisReference(val classFQN: String, val number: Int) : ExtractedBreakpointConditionParameter {
    override fun toString(): String = "$classFQN outerThisRef\$$number"
}

data class BreakpointConditionToInstrument(
    val psi: JavaCodeFragment,
    val extractedParameters: Set<ExtractedBreakpointConditionParameter>
) {
    override fun toString(): String = "BreakpointConditionToInstrument(${psi.text}, $extractedParameters)"
}

fun prepareBreakpointConditionForInstrumentation(conditionPsi: JavaCodeFragment): BreakpointConditionToInstrument? {
    // only line breakpoints are supported; this is expected to filter all others
    val conditionContext = conditionPsi.context ?: return null

    val extractedParameters = mutableSetOf<ExtractedBreakpointConditionParameter>()
    val qualifierAdditionQueue = mutableListOf<Pair<PsiJavaCodeReferenceElement, String>>()

    conditionPsi.accept(object : JavaRecursiveElementVisitor() {
        override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
            val target = reference.resolve()
                ?: throw IllegalStateException(
                    "unresolved entity $reference found in a breakpoint condition $conditionPsi"
                )

            // null means "no parameter to extract was found"
            var extractedParameter: ExtractedBreakpointConditionParameter? = null

            when {
                target is PsiQualifiedNamedElement -> {
                    val qualifier = target.qualifiedName?.substringBeforeLast('.')
                    if (qualifier != null) qualifierAdditionQueue += reference to qualifier
                }
                PsiUtil.isJvmLocalVariable(target) -> {
                    target as PsiVariable // both PsiLocalVariable and PsiParameter are PsiVariable
                    extractedParameter = LocalVariableReference(target.type.canonicalText, reference.text)
                }
                reference.qualifier == null && target.enclosingClass == conditionContext.enclosingClass -> {
                    val thisRefType = target.enclosingClass?.qualifiedName
                    if (thisRefType != null) extractedParameter = ThisReference(thisRefType)
                    qualifierAdditionQueue += reference to "thisRef"
                }
                reference.qualifier == null && target.enclosingClass in conditionContext.allEnclosingClasses -> {
                    val outerThisRefType = target.enclosingClass?.qualifiedName
                    val counter = extractedParameters.count { parameter -> parameter is OuterThisReference }
                    if (outerThisRefType != null) extractedParameter = OuterThisReference(outerThisRefType, counter)
                    qualifierAdditionQueue += reference to "outerThisRef\$$counter"
                }
            }

            if (extractedParameter != null) extractedParameters += extractedParameter

            /*
             * for some reason JavaRecursiveElementVisitor refuses to visit children of dot-qualified expressions
             * e.g. "a.b.c" is resolved with regard to c while a.b qualifier is just silently ignored
             * this workaround solves this issue
             */
            if (reference.qualifier != null) visitReferenceElement(reference.qualifier as PsiJavaCodeReferenceElement)
        }
    })

    /*
     * PSI modifications during visitor execution prevent other nodes from being visited,
     * so the modifications are performed after all parameters are extracted
     */
    qualifierAdditionQueue.forEach { (reference, qualifier) -> reference.addQualifier(qualifier) }

    return BreakpointConditionToInstrument(conditionPsi, extractedParameters)
}
