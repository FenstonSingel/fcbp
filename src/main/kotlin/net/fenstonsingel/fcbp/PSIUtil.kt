package net.fenstonsingel.fcbp

import com.intellij.debugger.engine.evaluation.TextWithImports
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.JavaCodeFragment
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.util.PsiUtil
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl

/**
 * A PSI element representing the breakpoint's condition,
 * or a PSI element for an empty string (with proper evaluation context)
 * if the breakpoint doesn't have a condition.
 */
val Breakpoint<*>.conditionPsi: JavaCodeFragment
    get() {
        val conditionExpression: XExpression = xBreakpoint.conditionExpression ?: XExpressionImpl.EMPTY_EXPRESSION
        val condition: TextWithImports = TextWithImportsImpl.fromXExpression(conditionExpression)
        val context = evaluationElement // the case of this being null is untested
        val codeFragmentFactory = DebuggerUtilsEx.findAppropriateCodeFragmentFactory(condition, context)
        return codeFragmentFactory.createCodeFragment(condition, context, project)
    }

/**
 * A PSI element representing a method, initializer, or class initializer block
 * that this element belongs to.
 *
 * @throws NoSuchElementException if element in question doesn't belong to any of the above.
 */
val PsiElement.enclosingBehavior: PsiMember
    get() = generateSequence(this) { psi -> psi.parent }
        .filterIsInstance<PsiMember>()
        .filter { psi -> psi is PsiMethod || psi is PsiClassInitializer }
        .first()

/**
 * A list of string representations for types of the method's parameters
 * (including an implicit reference in case of inner class constructors).
 */
val Pair<PsiClass, PsiMember>.parameterTypesNames: List<String>
    get() {
        val (klass, method) = this
        return when (method) {
            is PsiMethod -> {
                val parameters = method.parameterList.parameters.map { parameter -> parameter.type.binaryName }
                if (method.isConstructor && PsiUtil.isInnerClass(klass)) {
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
                throw IllegalStateException("Not a behavior was passed as a behavior")
        }
    }

/**
 * PSI elements representing a class that this element belongs to
 * and all of that class's outer classes.
 */
val PsiElement.allEnclosingClasses: List<PsiClass>
    get() = generateSequence(this) { psi -> psi.parent }
        .filterIsInstance<PsiClass>()
        .filter { psi -> psi !is PsiTypeParameter }
        .toList()

/**
 * A PSI element representing a class that this element belongs to.
 *
 * @throws NoSuchElementException if element in question doesn't belong to a class.
 */
val PsiElement.enclosingClass: PsiClass
    get() = allEnclosingClasses.first()

/**
 * A dot-separated Javassist-compatible (and theoretically JVM-compatible) name,
 * or null if there's no such name.
 */
val PsiMember.binaryName: String?
    get() = when (this) {
        is PsiClass ->
            containingClass.let { outerClass ->
                if (null != outerClass) "${outerClass.binaryName}$$name" else qualifiedName
            }
        is PsiMethod ->
            if (!isConstructor) name
            else allEnclosingClasses.reversed().map { klass -> klass.name }.joinToString(separator = "$")
        is PsiClassInitializer ->
            "<clinit>"
        // TODO check if enum classes are processed correctly
        // TODO process anonymous objects correctly
        else ->
            null
    }

/**
 * A dot-separated Javassist-compatible (and theoretically JVM-compatible) name,
 * or null if there's no such name.
 */
val PsiType.binaryName: String
    get() = when (this) {
        is PsiPrimitiveType ->
            name
        is PsiClassReferenceType ->
            when (val klass = checkNotNull(resolve()) { "Method parameter's type resolution failed" }) {
                is PsiTypeParameter -> klass.extendsListTypes.firstOrNull()?.binaryName ?: "java.lang.Object"
                else -> checkNotNull(klass.binaryName) { "Method parameter's type (somehow) has no binary name" }
            }
        else ->
            throw IllegalStateException("Unknown PsiType subclass encountered during binary name construction")
    }

/**
 * For any PSI reference "X", given PSI reference "Y", substitutes "Y.X" for "X".
 */
fun PsiJavaCodeReferenceElement.addQualifier(qualifier: PsiJavaCodeReferenceElement) {
    val template = ReadAction.compute<PsiExpression, Nothing> {
        val psiFactory: PsiElementFactory = JavaPsiFacade.getElementFactory(project)
        psiFactory.createExpressionFromText("x.y", null)
    }

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
    val qualifierPsi = ReadAction.compute<PsiJavaCodeReferenceElement, Nothing> {
        val psiFactory: PsiElementFactory = JavaPsiFacade.getElementFactory(project)
        psiFactory.createExpressionFromText(qualifier, null) as PsiJavaCodeReferenceElement
    }

    addQualifier(qualifierPsi)
}
