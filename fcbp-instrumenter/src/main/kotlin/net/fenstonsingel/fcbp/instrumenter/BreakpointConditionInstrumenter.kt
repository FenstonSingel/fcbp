package net.fenstonsingel.fcbp.instrumenter

import net.fenstonsingel.fcbp.instrumenter.compiler.CompilationContext
import net.fenstonsingel.fcbp.instrumenter.compiler.LocalVariable
import net.fenstonsingel.fcbp.instrumenter.compiler.components.ExpressionCompiler
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class BreakpointConditionInstrumenter(
    classVisitor: ClassVisitor,
    private val conditions: List<BreakpointCondition>,
    private val allLocalVariables: Map<String, Map<String, LocalVariable>>
) : ClassVisitor(ASM9, classVisitor) {

    override fun visitMethod(a: Int, name: String, d: String, s: String?, es: Array<out String>?): MethodVisitor =
        MethodInstrumenter(cv.visitMethod(a, name, d, s, es), name)

    private val lineNumberCounters = mutableMapOf<Int, Int>()

    private inner class MethodInstrumenter(
        methodVisitor: MethodVisitor,
        methodName: String
    ) : MethodVisitor(ASM9, methodVisitor) {

        override fun visitLabel(label: Label) {
            ++lastVisitedLabel
            mv.visitLabel(label)
        }

        override fun visitLineNumber(line: Int, start: Label) {
            val relevantCondition = conditions.find { (lineNumber, _, _) -> line == lineNumber }
            if (relevantCondition == null) {
                mv.visitLineNumber(line, start)
                return
            }

            with(relevantCondition) {
                lineNumberCounters.merge(line, 1, Int::plus)
                if (labelNumbers != null && lineNumberCounters[line] !in labelNumbers) {
                    mv.visitLineNumber(line, start)
                    return
                }

                val breakpointLabel = Label()
                val codeLabel = Label()
                ExpressionCompiler(mv, compilationContext, expression, breakpointLabel, codeLabel)

                mv.visitLabel(breakpointLabel)
                mv.visitLineNumber(line, breakpointLabel)
                mv.visitInsn(Opcodes.NOP)

                mv.visitLabel(codeLabel)
            }
        }

        private var lastVisitedLabel = 0

        private val methodLocalVariables = allLocalVariables[methodName]
            ?: throw IllegalStateException("No attempt to find local variables for method $methodName was made.")

        private val compilationContext: CompilationContext get() =
            CompilationContext(
                methodLocalVariables.filter { (_, lv) ->
                    lv.startLabel <= lastVisitedLabel && lv.endLabel >= lastVisitedLabel
                }
            )

    }

}
