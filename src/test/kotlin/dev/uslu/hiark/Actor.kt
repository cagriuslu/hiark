package dev.uslu.hiark

import kotlin.test.Test
import kotlin.test.assertTrue

class SimpleActorTest {
    class SimpleActor : Actor<SimpleActor>(SimpleActor::startState, SimpleActor::class.java.simpleName) {
        var flag = false

        fun startState(signal: Signal) : Action<SimpleActor> {
            return when (signal) {
                is Signal.Enter -> {
                    flag = true
                    Action.Handled()
                }
                is Signal.Exit -> Action.Handled()
                else -> Action.Super(SimpleActor::startState)
            }
        }
    }

    @Test
    fun test() {
        val simpleActor = SimpleActor()
        Thread.sleep(1000)
        simpleActor.stop()

        assertTrue { simpleActor.flag }
    }
}
