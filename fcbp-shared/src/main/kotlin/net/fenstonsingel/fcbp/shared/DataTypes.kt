package net.fenstonsingel.fcbp.shared

import kotlinx.serialization.Serializable

@Serializable @JvmInline
value class FCBPType(val name: String)

@Serializable
data class FCBPMethod(val name: String, val parameters: List<FCBPType>)

// TODO include lambda ordinals in the comparison
@Serializable
data class FCBPSourcePosition(val lineNumber: Int, val lambdaOrdinal: Int? = null) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FCBPSourcePosition
        if (lineNumber != other.lineNumber) return false
        return true
    }

    override fun hashCode(): Int {
        return lineNumber
    }
}

@Serializable @JvmInline
value class FCBPCondition(val expression: String)

@Serializable
data class FCBPBreakpoint(
    val klass: FCBPType,
    val method: FCBPMethod,
    val position: FCBPSourcePosition,
    val condition: FCBPCondition
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FCBPBreakpoint
        if (klass != other.klass) return false
        if (method != other.method) return false
        if (position != other.position) return false
        return true
    }

    override fun hashCode(): Int {
        var result = klass.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + position.hashCode()
        return result
    }
}
