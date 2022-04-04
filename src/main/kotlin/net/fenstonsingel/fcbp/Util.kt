package net.fenstonsingel.fcbp

/**
 * Returns a given number of strings, each of which doesn't appear in a provided set of non-fresh identifiers.
 * Returned strings follow alphabetical order in lowercase
 * ("a", "b", ... "z", "aa", ..., "az", "ba", ..., "zz", "aaa", ...).
 */
fun generateFreshIdentifiers(number: Int, nonFreshIdentifiers: Set<String> = emptySet()): Set<String> {
    return stringsInAlphabeticalOrder.filter { candidate -> candidate !in nonFreshIdentifiers }.take(number).toSet()
}

/**
 * A freshly generated sequence of strings that follow the pattern of alphabetical order in lowercase
 * ("a", "b", ... "z", "aa", ..., "az", "ba", ..., "zz", "aaa", ...).
 */
val stringsInAlphabeticalOrder: Sequence<String>
    get() = generateSequence("a") { str ->
        val zsIndex = str.indexOfLast { c -> c != 'z' } + 1
        val oldPrefix = str.substring(0, zsIndex)
        val newPrefix = if (oldPrefix.isEmpty()) "a" else "${oldPrefix.dropLast(1)}${oldPrefix.last() + 1}"
        "$newPrefix${"a".repeat(str.length - zsIndex)}"
    }
