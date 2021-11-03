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
            method1.insertAt(6, "System.out.println(\"Hello from example1!\");")

            val method2 = klass.getDeclaredMethod("example2")
            method2.insertAt(11, "System.out.println(\"First hello from example2!\");")
            method2.insertAt(12, "System.out.println(\"Second hello from example2!\");")

            val method3 = klass.getDeclaredMethod("example3")
            method3.insertAt(15, "if (b) System.out.println(\"Conditional hello from example3!\");")

            val method4 = klass.getDeclaredMethod("example4")
            method4.insertAt(18, "if (bs.length > 0) System.out.printf(\"Hello from example4 w/ %b!\\n\", new Object[] { new Boolean(bs[0]) });")

            val method5 = klass.getDeclaredMethod("example5")
            method5.insertAt(23, "i *= 2;")

            val method6 = klass.getDeclaredMethod("example6")
            method6.insertAt(28, "fooi /= 2;")
            method6.insertAt(29, "foov();")

            val method7 = klass.getDeclaredMethod("example7")
            method7.insertAt(32, "net.fenstonsingel.javassist.examples.code.JavassistExamples.A.foov();")
            method7.insertAt(32, "System.out.printf(\"Hello from example7 w/ %d!\\n\", new Object[] { new Integer(net.fenstonsingel.javassist.examples.code.JavassistExamples.A.fooi) });")

            val method8 = klass.getDeclaredMethod("example8")
            method8.insertAt(35, "{ net.fenstonsingel.javassist.examples.outside.OutsideClass oc = new net.fenstonsingel.javassist.examples.outside.OutsideClassImpl(); System.out.printf(\"Hello from example8 w/ %d!\\n\", new Object[] { new Integer(oc.foo()) }); }")

            val method9 = klass.getDeclaredMethod("example9")
            method9.insertAt(38, "barv(new net.fenstonsingel.javassist.examples.outside.OutsideClassImpl());")

            val method10 = klass.getDeclaredMethod("example10")
            method10.insertAt(41, "{ Integer i = (Integer) new net.fenstonsingel.javassist.examples.outside.OutsideGenericClass().id((Integer) new Integer(42)); System.out.printf(\"Hello from example10 w/ %d from OutsideGenericClass object!\\n\", new Object[] { i }); }")

            val method11 = klass.getDeclaredMethod("example11")
            method11.insertAt(44, "System.out.printf(\"Hello from example11 w/ (%s: %d)!\\n\", new Object[] { net.fenstonsingel.javassist.examples.outside.OutsideEnumClass.A.name(), new Integer(net.fenstonsingel.javassist.examples.outside.OutsideEnumClass.A.fooi()) });")

            // note to self: javassist cannot compile declarations of anonymous classes (lambdas also don't work)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        return klass.toBytecode()
    }

    private val classPool = ClassPool.getDefault()

}
