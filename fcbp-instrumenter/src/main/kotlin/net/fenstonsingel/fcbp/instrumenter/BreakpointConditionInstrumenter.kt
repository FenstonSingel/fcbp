package net.fenstonsingel.fcbp.instrumenter

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class BreakpointConditionInstrumenter(
    classVisitor: ClassVisitor,
    private val conditions: List<BreakpointCondition>
) : ClassVisitor(ASM9, classVisitor) {

    override fun visitMethod(a: Int, name: String, d: String, s: String?, es: Array<out String>?): MethodVisitor =
        MethodInstrumenter(cv.visitMethod(a, name, d, s, es), name)

    private val lineNumberCounters = mutableMapOf<Int, Int>()

    private inner class MethodInstrumenter(
        methodVisitor: MethodVisitor,
        methodName: String
    ) : MethodVisitor(ASM9, methodVisitor) {

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

                mv.visitLabel(breakpointLabel)
                mv.visitLineNumber(line, breakpointLabel)
                mv.visitInsn(Opcodes.NOP)

                mv.visitLabel(codeLabel)
            }
        }

    }

}
