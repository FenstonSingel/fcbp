package net.fenstonsingel.fcbp.instrumenter

import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

object FCBPTransformer : ClassFileTransformer {

    override fun transform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        return if (classBeingRedefined == null)
            transform(loader, className, protectionDomain, classfileBuffer)
        else
            retransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
    }

    private fun transform(
        loader: ClassLoader?,
        className: String?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        TODO()
    }

    private fun retransform(
        loader: ClassLoader?,
        className: String?,
        classBeingRedefined: Class<*>,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray?
    ): ByteArray? {
        TODO()
    }

}
