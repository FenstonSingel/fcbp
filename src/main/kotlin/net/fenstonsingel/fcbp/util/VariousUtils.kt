package net.fenstonsingel.fcbp.util

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
 */
val Breakpoint<*>.conditionPsi: JavaCodeFragment? get() {
    val context = evaluationElement
    val condition = TextWithImportsImpl.fromXExpression(xBreakpoint.conditionExpression) ?: return null
    return DebuggerUtilsEx
        .findAppropriateCodeFragmentFactory(condition, context)
        .createCodeFragment(condition, context, project)
}

/**
 * All classes that are available to reference from this code point, including outer ones.
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

fun PsiJavaCodeReferenceElement.addQualifier(qualifier: String) {
    val psiFactory = JavaPsiFacade.getElementFactory(project)
    val qualifierPsi = psiFactory.createExpressionFromText(qualifier, null) as PsiJavaCodeReferenceElement
    addQualifier(qualifierPsi)
}
