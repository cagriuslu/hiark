package dev.uslu.hiark

import kotlin.test.Test
import kotlin.test.assertTrue

class SimpleActorTest {
    class SimpleState : State() {
        var flag = false

        override fun handle(signal: Signal): Action {
            flag = true
            return Action.Handled()
        }

        companion object {
            fun simpleState() : State = SimpleState()

            val simpleStateDesc = StateDescriptor(::simpleState, null)
        }
    }

    class SimpleSignal : Signal

    @Test
    fun test() {
        val simpleActor = Actor(SimpleState.simpleStateDesc, "Simple")
        simpleActor.queue(SimpleSignal())
        Thread.sleep(100)
        simpleActor.stop()

        val simpleState = simpleActor.currentState()
        assertTrue { (simpleState as SimpleState).flag }
    }
}
