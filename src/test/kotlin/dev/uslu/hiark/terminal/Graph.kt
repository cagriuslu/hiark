package dev.uslu.hiark.terminal

import dev.uslu.hiark.graphActor
import kotlin.test.Test

class Graph {
    @Test
    fun graphTerminal() {
        println(graphActor(Terminal::class, TerminalImpl(true, false)))
    }
}