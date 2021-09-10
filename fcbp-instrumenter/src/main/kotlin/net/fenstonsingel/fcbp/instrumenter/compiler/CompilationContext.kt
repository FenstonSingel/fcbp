package net.fenstonsingel.fcbp.instrumenter.compiler

data class CompilationContext constructor (
    val localVariables: Map<String, LocalVariable>
)
