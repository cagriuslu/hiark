package dev.uslu.hiark

import kotlin.test.Test
import kotlin.test.assertContentEquals

class Extensions {
    @Test
    fun retainUntilFirstCommon() {
        val listA = listOf(1, 2, 3, 4)
        assertContentEquals(listOf(1, 2), listA.retainUntilFirstCommon(listOf(-1, -2, 3)))

        val listB = listOf(1, 2, 3)
        assertContentEquals(emptyList(), listB.retainUntilFirstCommon(listOf(1)))

        val listC = listOf(1, 2, 3)
        assertContentEquals(listC.retainUntilFirstCommon(listOf(3)), listOf(1, 2))
    }
}