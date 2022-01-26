package net.fenstonsingel.fcbp.instrumenter

import javassist.ClassPool
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

        val classReader = ClassReader(classfileBuffer.copyOf())
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
        val instrumenter = BreakpointConditionInstrumenter(classWriter, breakpointConditions)
        classReader.accept(instrumenter, ClassReader.EXPAND_FRAMES)

        val ctClass = ClassPool.getDefault().makeClass(classWriter.toByteArray().inputStream())
        // TODO actually compile all conditions

        return ctClass.toBytecode()
    }

}
