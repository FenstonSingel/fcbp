package net.fenstonsingel.fcbp.instrumenter

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class DummyInstrumenter(classVisitor: ClassVisitor) : ClassVisitor(ASM9, classVisitor) {

    inner class DummyMethodInstrumenter(methodVisitor: MethodVisitor) : MethodVisitor(ASM9, methodVisitor) {
        override fun visitCode() {
            super.visitCode()
            mv.visitInsn(Opcodes.NOP)
        }

        override fun visitInsn(opcode: Int) {
            if (opcode == Opcodes.RETURN) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/fennmata/InstrumentationTests", "log", "()V", false)
            }
            super.visitInsn(opcode)
        }
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return if (name == "run") {
            val default = cv.visitMethod(access, name, descriptor, signature, exceptions)
            DummyMethodInstrumenter(default)
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

}
