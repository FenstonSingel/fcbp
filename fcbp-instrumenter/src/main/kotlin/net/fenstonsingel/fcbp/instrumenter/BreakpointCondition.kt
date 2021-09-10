package net.fenstonsingel.fcbp.instrumenter

data class BreakpointCondition(
    val codeNumber: Int, // 1-indexed
    val labelNumbers: Set<Int>?, // 1-indexed, null is everything
    val expression: String
)
