package dev.uslu.hiark

fun String.trimWarning() : String {
    val warning = " (Kotlin reflection is not available)"
    if (this.endsWith(warning)) {
        return substring(0, length - warning.length)
    }
    return this
}
