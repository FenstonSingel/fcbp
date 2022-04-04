package net.fenstonsingel.fcbp.shared

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class FCBPType(val name: String)

@Serializable
data class FCBPMethod(val name: String, val parameters: List<FCBPType>)

@Serializable
data class FCBPSourcePosition(val lineNumber: Int, val lambdaOrdinal: Int? = null)

@Serializable
@JvmInline
value class FCBPCondition(val body: String)

@Serializable
data class FCBPBreakpoint(
    val klass: FCBPType,
    val method: FCBPMethod,
    val position: FCBPSourcePosition,
    val condition: FCBPCondition
)
