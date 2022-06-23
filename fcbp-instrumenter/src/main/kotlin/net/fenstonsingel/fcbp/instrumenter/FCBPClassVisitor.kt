package net.fenstonsingel.fcbp.instrumenter

import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPMethod
import net.fenstonsingel.fcbp.shared.FCBPType
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class FCBPClassVisitor(
    classVisitor: ClassVisitor,
    className: String,
    breakpoints: List<FCBPBreakpoint>
) : ClassVisitor(Opcodes.ASM9, classVisitor) {

    private val constructorName by lazy { className.substringAfterLast('/') }

    private val breakpointsByMethod = breakpoints.groupBy(FCBPBreakpoint::method)

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv: MethodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions)

        val methodName = if ("<init>" == name) constructorName else name
        val parameterTypes: Array<Type> = Type.getMethodType(descriptor).argumentTypes
        val methodParameters = parameterTypes.map { type -> FCBPType(type.className) }
        val method = FCBPMethod(methodName, methodParameters)
        val breakpoints = breakpointsByMethod[method] ?: return mv

        return FCBPMethodVisitor(mv, breakpoints)
    }

}
