package net.fenstonsingel.fcbp.util

import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiQualifiedNamedElement
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiUtil

data class BreakpointConditionMethod(val body: String, val parameters: Set<BreakpointConditionMethodParameter>) {
    companion object {
        fun from(conditionPsi: JavaCodeFragment) = BreakpointConditionPsiProcessor(conditionPsi).process()
    }
}

data class BreakpointConditionMethodParameter(val typeFQN: String, val identifier: String)

class BreakpointConditionPsiProcessor(private val condition: JavaCodeFragment) : JavaRecursiveElementVisitor() {

    fun process(): BreakpointConditionMethod {
        condition.accept(this)

        // assigning fresh identifiers to all explicit "this" references to be added to avoid name collisions
        val thisToExtract = thisReferencesToExtract.map { pair -> pair.first }.toSet()
        val identifiersForThis = thisToExtract
            .zip(generateFreshIdentifiers(thisToExtract.size, nonFreshIdentifiers))
            .toMap()

        // registering all necessary parameters to forward targets of "this" references
        for ((thisRefType, identifier) in identifiersForThis) {
            extractedParameters += BreakpointConditionMethodParameter(thisRefType.fullyQualifiedName, identifier)
        }

        // marking implicit "this" references for qualifier addition to turn them into explicit references
        for ((thisRefType, referencePsi) in thisReferencesToExtract) {
            val identifierForThis = identifiersForThis[thisRefType]
                ?: throw IllegalStateException("an identifier wasn't generated for a \"this\" reference")
            qualifiersToAdd += identifierForThis to referencePsi
        }

        /*
         * PSI modifications during visitor execution prevent other nodes from being visited,
         * so the modifications are performed after all parameters are extracted
         */
        qualifiersToAdd.forEach { (qualifier, reference) -> reference.addQualifier(qualifier) }

        return BreakpointConditionMethod(condition.text, extractedParameters)
    }

    override fun visitIdentifier(identifier: PsiIdentifier) {
        super.visitIdentifier(identifier) // maintaining recursion
        nonFreshIdentifiers += identifier.text
    }

    /*
     * TODO extracting private members/methods
     * TODO extracting explicit "this" references
     */
    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        super.visitReferenceElement(reference) // maintaining recursion

        // only leftmost reference of any dot-qualified expression is of any interest to us
        if (reference.qualifier != null) return

        val target = reference.resolve()
            ?: throw IllegalStateException("unresolved entity $reference found in a breakpoint condition $condition")

        val context = condition.context
            ?: throw IllegalStateException("breakpoint condition has no execution context")

        when {
            PsiUtil.isJvmLocalVariable(target) -> { // extracting context method's local variables and parameters
                target as PsiVariable // both PsiLocalVariable and PsiParameter are PsiVariable
                extractedParameters += BreakpointConditionMethodParameter(target.type.canonicalText, reference.text)
            }
            target is PsiQualifiedNamedElement -> { // resolving types to their FQN (e.g. in static method calls)
                val qualifier = target.qualifiedName?.substringBeforeLast('.')
                if (qualifier != null) qualifiersToAdd += qualifier to reference
            }
            target.enclosingClass in context.allEnclosingClasses -> { // prepping to extract [outer] "this" references
                val thisRefType = target.enclosingClass?.qualifiedName
                if (thisRefType != null) thisReferencesToExtract += ThisReferenceType(thisRefType) to reference
            }
        }
    }

    private val nonFreshIdentifiers = mutableSetOf<String>()

    private val thisReferencesToExtract = mutableListOf<Pair<ThisReferenceType, PsiJavaCodeReferenceElement>>()

    private val qualifiersToAdd = mutableListOf<Pair<String, PsiJavaCodeReferenceElement>>()

    private val extractedParameters = mutableSetOf<BreakpointConditionMethodParameter>()

}

@JvmInline
private value class ThisReferenceType(val fullyQualifiedName: String)
