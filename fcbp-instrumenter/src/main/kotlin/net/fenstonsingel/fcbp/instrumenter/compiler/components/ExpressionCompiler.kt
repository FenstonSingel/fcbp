package net.fenstonsingel.fcbp.instrumenter.compiler.components

import net.fenstonsingel.fcbp.instrumenter.compiler.CompilationContext
import net.fenstonsingel.fcbp.instrumenter.compiler.ContextAwareCompiler
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

object ExpressionCompiler : ContextAwareCompiler {
    override fun invoke(
        method: MethodVisitor,
        context: CompilationContext,
        expression: String,
        trueTarget: Label,
        falseTarget: Label
    ) {
        // -- works (10L == 10L => always bp)
//        method.visitLdcInsn(10L)
//        method.visitLdcInsn(10L)
//        method.visitInsn(Opcodes.LCMP)
//        method.visitJumpInsn(Opcodes.IFNE, falseTarget)

        // -- works (10L != 10L => no bp)
//        method.visitLdcInsn(10L)
//        method.visitLdcInsn(10L)
//        method.visitInsn(Opcodes.LCMP)
//        method.visitJumpInsn(Opcodes.IFEQ, falseTarget)

        // index == 10
//        method.visitVarInsn(Opcodes.LLOAD, context.localVariables["index"]!!.index)
//        method.visitLdcInsn(10L)
//        method.visitInsn(Opcodes.LCMP)
//        method.visitJumpInsn(Opcodes.IFNE, falseTarget)

        // index == settings.loopLength
        method.visitVarInsn(Opcodes.LLOAD, context.localVariables["index"]!!.index)
        method.visitVarInsn(Opcodes.ALOAD, context.localVariables["settings"]!!.index)
        method.visitFieldInsn(Opcodes.GETFIELD, "net/fennmata/Benchmark\$BenchmarkSettings", "loopLength", "J")
        method.visitInsn(Opcodes.LCMP)
        method.visitJumpInsn(Opcodes.IFNE, falseTarget)
    }
}
