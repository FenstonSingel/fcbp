package net.fenstonsingel.fcbp.instrumenter

/**
 * This object's string representation is a Java expression used for
 * generating placeholder bytecode that marks locations for breakpoint placement.
 *
 * Used in [FCBPTransformer] and [FCBPMethodVisitor].
 */
object FCBPCompilationPlaceholder {

    /** Does nothing. */
    @JvmStatic fun holdPlace() { /* nothing */ }

    private val className = javaClass.name

    val methodName: String = FCBPCompilationPlaceholder::holdPlace.name

    override fun toString(): String = "$className.$methodName()"

    val binaryClassName: String = className.replace('.', '/')

}
