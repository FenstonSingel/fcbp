package net.fenstonsingel.fcbp.instrumenter

import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class FCBPMethodVisitor(
    methodVisitor: MethodVisitor,
    private val breakpoints: List<FCBPBreakpoint>
) : MethodVisitor(Opcodes.ASM9, methodVisitor) {

    private var breakpointInProcessing: FCBPBreakpoint? = null

    private val processedBreakpoints = mutableSetOf<FCBPBreakpoint>()

    override fun visitLineNumber(line: Int, start: Label) {
        val breakpoint = breakpoints.find { bp -> line == bp.position.lineNumber }
        if (null == breakpoint) mv.visitLineNumber(line, start)
        else breakpointInProcessing = breakpoint
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        val isInvokeStatic = Opcodes.INVOKESTATIC == opcode
        val isCompilationPlaceholderClass = FCBPCompilationPlaceholder.binaryClassName == owner
        val isCompilationPlaceholderMethod = FCBPCompilationPlaceholder.methodName == name
        if (isInvokeStatic && isCompilationPlaceholderClass && isCompilationPlaceholderMethod) {
            val breakpoint = checkNotNull(breakpointInProcessing) { "Compilation placeholder found for no breakpoint" }
            breakpointInProcessing = null

            val breakpointLabel = Label()
            mv.visitLabel(breakpointLabel)
            mv.visitLineNumber(breakpoint.position.lineNumber, breakpointLabel)
            mv.visitInsn(Opcodes.NOP)
            processedBreakpoints += breakpoint
        } else {
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }

    override fun visitEnd() {
        check(breakpoints.toSet() == processedBreakpoints) {
            "Some breakpoint conditions weren't processed completely during instrumentation"
        }
    }

}
