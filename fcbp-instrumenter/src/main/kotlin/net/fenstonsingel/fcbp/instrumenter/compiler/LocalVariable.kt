package net.fenstonsingel.fcbp.instrumenter.compiler

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

class LocalVariable private constructor(
    val index: Int,
    val descriptor: String,
    val signature: String?,
    val startLabel: Int,
    val endLabel: Int
) {

    override fun toString(): String = "LocalVariable($index, $descriptor, $signature, $startLabel, $endLabel)"

    class Collector(classVisitor: ClassVisitor) : ClassVisitor(ASM9, classVisitor) {

        val result: Map<String, Map<String, LocalVariable>> get() = storage

        override fun visitMethod(a: Int, name: String, d: String, s: String?, es: Array<out String>?): MethodVisitor =
            MV(cv.visitMethod(a, name, d, s, es), name)

        private val storage = mutableMapOf<String, MutableMap<String, LocalVariable>>()

        private inner class MV(
            methodVisitor: MethodVisitor,
            private val methodName: String
        ) : MethodVisitor(ASM9, methodVisitor) {

            init {
                storage[methodName] = mutableMapOf()
            }

            override fun visitLabel(label: Label) {
                labelNumber[label] = ++totalNumberOfLabels
                mv.visitLabel(label)
            }

            override fun visitLocalVariable(
                name: String,
                descriptor: String,
                signature: String?,
                start: Label,
                end: Label,
                index: Int
            ) {
                val methodLocalVariables = storage[methodName]!!
                methodLocalVariables[name] = LocalVariable(
                    index, descriptor, signature, labelNumber[start] ?: -1, labelNumber[end] ?: -1
                )
                mv.visitLocalVariable(name, descriptor, signature, start, end, index)
            }

            private var totalNumberOfLabels = 0

            private val labelNumber = mutableMapOf<Label, Int>()

        }

    }

}
