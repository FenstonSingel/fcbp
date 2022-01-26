package net.fenstonsingel.fcbp

import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiTypeParameter
import com.intellij.util.containers.JBIterable

/**
 * A PSI element representing the breakpoint's condition.
 *
 * Is null when the breakpoint doesn't have a condition.
 *
 * TODO check the invocation case when context == null
 */
val Breakpoint<*>.conditionPsi: JavaCodeFragment? get() {
    val condition = TextWithImportsImpl.fromXExpression(xBreakpoint.conditionExpression)
        ?: return null
    val context = evaluationElement
    return DebuggerUtilsEx
        .findAppropriateCodeFragmentFactory(condition, context)
        .createCodeFragment(condition, context, project)
}

/**
 * All classes that are available for referencing from this code point, including outer ones.
 */
val PsiElement.allEnclosingClasses: List<PsiClass> get() = JBIterable
    .generate(this) { psi -> psi.parent }
    .takeWhile { psi -> psi !is PsiFile }
    .filter(PsiClass::class.java)
    .filter { psi -> psi !is PsiTypeParameter }
    .toList()

/**
 * The class this code point belongs to.
 */
val PsiElement.enclosingClass: PsiClass? get() = allEnclosingClasses.firstOrNull()

/**
 * For any PSI reference "X", given PSI reference "Y", substitutes "Y.X" for "X".
 */
fun PsiJavaCodeReferenceElement.addQualifier(qualifier: PsiJavaCodeReferenceElement) {
    val psiFactory = JavaPsiFacade.getElementFactory(project)
    val template = psiFactory.createExpressionFromText("x.y", null)

    /*
     * per documentation, all PSI transformations must occur as write actions
     * thankfully, it doesn't seem to propagate to the original data set by the user
     */
    WriteCommandAction.runWriteCommandAction(project) {
        template.firstChild.replace(qualifier)
        template.lastChild.replace(this)
        replace(template)
    }
}

/**
 * For any PSI reference "X", given a qualifier "Y" as a string, substitutes "Y.X" for "X".
 */
fun PsiJavaCodeReferenceElement.addQualifier(qualifier: String) {
    val psiFactory = JavaPsiFacade.getElementFactory(project)
    val qualifierPsi = psiFactory.createExpressionFromText(qualifier, null) as PsiJavaCodeReferenceElement
    addQualifier(qualifierPsi)
}

/**
 * Returns a given number of strings, each of which doesn't appear in a provided set of non-fresh identifiers.
 * Returned strings follow alphabetical order in lowercase
 * ("a", "b", ... "z", "aa", ..., "az", "ba", ..., "zz", "aaa", ...).
 */
fun generateFreshIdentifiers(number: Int, nonFreshIdentifiers: Set<String> = emptySet()): Set<String> {
    return stringsInAlphabeticalOrder.filter { candidate -> candidate !in nonFreshIdentifiers }.take(number).toSet()
}

/**
 * A freshly generated sequence of strings that follow the pattern of alphabetical order in lowercase
 * ("a", "b", ... "z", "aa", ..., "az", "ba", ..., "zz", "aaa", ...).
 */
val stringsInAlphabeticalOrder: Sequence<String> get() = generateSequence("a") { str ->
    val zsIndex = str.indexOfLast { c -> c != 'z' } + 1
    val oldPrefix = str.substring(0, zsIndex)
    val newPrefix = if (oldPrefix.isEmpty()) "a" else "${oldPrefix.dropLast(1)}${oldPrefix.last() + 1}"
    "$newPrefix${"a".repeat(str.length - zsIndex)}"
}
