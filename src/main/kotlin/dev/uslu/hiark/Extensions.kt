package dev.uslu.hiark

// Retains only the elements up to the first common element in this collection compared against the specified container
fun <E> List<E>.retainUntilFirstCommon(other: List<E>) : List<E> {
    for ((index, value) in withIndex()) {
        if (other.contains(value)) {
            // Trim the list
            return subList(0, index)
        }
    }
    return this
}

fun String.trimWarning() : String {
    val warning = " (Kotlin reflection is not available)"
    if (this.endsWith(warning)) {
        return substring(0, length - warning.length)
    }
    return this
}
