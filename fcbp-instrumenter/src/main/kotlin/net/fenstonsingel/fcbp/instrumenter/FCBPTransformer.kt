package net.fenstonsingel.fcbp.instrumenter

import net.fenstonsingel.fcbp.instrumenter.compiler.LocalVariable
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class FCBPTransformer(private val instrumenterManager: FCBPInstrumenterManager) : ClassFileTransformer {

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? {
        val breakpointConditions = instrumenterManager.conditionsByClassName[className] ?: return null

        val classfileCopy = classfileBuffer.copyOf()

        val lvcClassReader = ClassReader(classfileCopy)
        val lvcClassWriter = ClassWriter(lvcClassReader, ClassWriter.COMPUTE_FRAMES)
        val localVariablesCollector = LocalVariable.Collector(lvcClassWriter)
        lvcClassReader.accept(localVariablesCollector, 0)
        val allLocalVariables = localVariablesCollector.result

        val classReader = ClassReader(classfileCopy)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
        val instrumenter = BreakpointConditionInstrumenter(classWriter, breakpointConditions, allLocalVariables)
        classReader.accept(instrumenter, ClassReader.EXPAND_FRAMES)

        return classWriter.toByteArray()
    }

}
