package net.fenstonsingel.fcbp.instrumenter.compiler

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

interface ContextAwareCompiler {
    operator fun invoke(
        method: MethodVisitor,
        context: CompilationContext,
        expression: String,
        trueTarget: Label,
        falseTarget: Label
    )
}
