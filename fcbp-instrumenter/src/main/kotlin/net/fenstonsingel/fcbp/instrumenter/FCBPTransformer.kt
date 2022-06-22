package net.fenstonsingel.fcbp.instrumenter

import javassist.ClassPool
import javassist.CtBehavior
import javassist.CtClass
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class FCBPTransformer(private val instrumenter: FCBPInstrumenter) : ClassFileTransformer {

    private val classPool = ClassPool.getDefault()

    private val resultingBytecodeDebugFolder by lazy {
        File(instrumenter.loggingDirectory, "classfiles").apply {
            deleteRecursively()
            mkdir()
        }
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? {
        val breakpoints = instrumenter.breakpointsByClassName[className] ?: return null
        val breakpointsByMethod = breakpoints.groupBy(FCBPBreakpoint::method)

        val unsupportedBreakpoints = mutableSetOf<FCBPBreakpoint>()
        val klass: CtClass = classPool[className.replace('/', '.')]
        for (behavior: CtBehavior in klass.declaredBehaviors) {
            val methodBreakpoints = breakpointsByMethod[behavior.toFCBPMethod()] ?: continue
            for (breakpoint in methodBreakpoints) {
                try {
                    behavior.insertAt(
                        breakpoint.position.lineNumber,
                        "if (${breakpoint.condition.expression}) { $FCBPCompilationPlaceholder; }"
                    )
                } catch (e: Exception) {
                    unsupportedBreakpoints += breakpoint
                }
            }
        }

        val supportedBreakpoints = breakpoints - unsupportedBreakpoints
        val classReader = ClassReader(klass.toBytecode())
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
        val fcbpClassVisitor = FCBPClassVisitor(classWriter, className, supportedBreakpoints)

        try {
            classReader.accept(fcbpClassVisitor, ClassReader.EXPAND_FRAMES)
        } catch (e: Exception) {
            println(e)
            breakpoints.forEach { bp -> instrumenter.transmitConditionStatus(bp, FCBPConditionStatus.DELEGATED) }
            return null
        }

        unsupportedBreakpoints.forEach { bp -> instrumenter.transmitConditionStatus(bp, FCBPConditionStatus.DELEGATED) }
        supportedBreakpoints.forEach { bp -> instrumenter.transmitConditionStatus(bp, FCBPConditionStatus.INSTRUMENTED) }

        // this is purely a debug thing that saves resulting bytecodes to a hard drive for further analysis
        val resultingBytecode = classWriter.toByteArray()
        val resultingBytecodeFilePath = "${className.replace('/', '.')}.class"
        File(resultingBytecodeDebugFolder, resultingBytecodeFilePath).outputStream().apply {
            write(resultingBytecode)
            close()
        }

        klass.detach() // let Javassist know to use original bytecodes when dealing with this class next time
        return resultingBytecode
    }

}
