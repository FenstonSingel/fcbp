package net.fenstonsingel.javassist.examples.javaagent

import javassist.ClassPool
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

internal object Transformer : ClassFileTransformer {

    override fun transform(
            loader: ClassLoader?,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain,
            classfileBuffer: ByteArray
    ): ByteArray? {
        if (className != "net/fenstonsingel/javassist/examples/code/JavassistExamples") return null

        val dotSeparatedClassName = className.replace('/', '.')
        val klass = classPool[dotSeparatedClassName]

        try {
            val method1 = klass.getDeclaredMethod("example1")
            method1.insertAt(5, "System.out.println(\"Hello from example1!\");")

            val method2 = klass.getDeclaredMethod("example2")
            method2.insertAt(8, "System.out.println(\"First hello from example2!\");")
            method2.insertAt(9, "System.out.println(\"Second hello from example2!\");")

            val method3 = klass.getDeclaredMethod("example3")
            method3.insertAt(11, "if (b) System.out.println(\"Conditional hello from example3!\");")

            val method4 = klass.getDeclaredMethod("example4")
            method4.insertAt(13, "example2();")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        return klass.toBytecode()
    }

    private val classPool = ClassPool.getDefault()

}
