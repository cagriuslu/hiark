package dev.uslu.hiark.terminal

import dev.uslu.hiark.Graph
import kotlin.test.Test

class Graph {
    @Test
    fun graphTerminal() {
        println(Graph(Terminal::class).graph())
    }
}