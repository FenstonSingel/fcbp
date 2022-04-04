package net.fenstonsingel.fcbp.instrumenter

import javassist.ClassPool
import javassist.CtClass
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class FCBPTransformer(private val instrumenter: FCBPInstrumenter) : ClassFileTransformer {

    private val classPool = ClassPool.getDefault()

    private val resultingBytecodeDebugFolder = File("build/fcbp-class-files")

    init {
        resultingBytecodeDebugFolder.deleteRecursively()
        resultingBytecodeDebugFolder.mkdir()
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
        for (behavior in klass.declaredBehaviors) {
            if (null == behavior) continue
            val methodBreakpoints = breakpointsByMethod[behavior.toFCBPMethod()] ?: continue
            for (breakpoint in methodBreakpoints) {
                try {
                    behavior.insertAt(
                        breakpoint.position.lineNumber,
                        "if (${breakpoint.condition.body}) { $FCBPCompilationPlaceholder; }"
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
            e.printStackTrace()
            breakpoints.forEach { bp -> instrumenter.tellBreakpointStatusToDebugger(bp, isInstrumented = false) }
            return null
        }

        unsupportedBreakpoints.forEach { bp -> instrumenter.tellBreakpointStatusToDebugger(bp, isInstrumented = false) }
        supportedBreakpoints.forEach { bp -> instrumenter.tellBreakpointStatusToDebugger(bp, isInstrumented = true) }

        // this is purely a debug thing that saves resulting bytecodes to a hard drive for further analysis
        val resultingBytecode = classWriter.toByteArray()
        val resultingBytecodeFilePath = "${className.replace('/', '.')}.class"
        File(resultingBytecodeDebugFolder, resultingBytecodeFilePath).outputStream().write(resultingBytecode)

        return resultingBytecode
    }

}
