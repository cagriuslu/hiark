package dev.uslu.hiark

import java.util.LinkedList
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread

// TODO implement enter and exit
// TODO check that if a handler returns a Transition, it returns something from the correct Transition class

class Actor(initialState: StateDescriptor, val name: String) {
    /**
     * Contains the states from ROOT to LEAF
     */
    private var currentStateChain = LinkedList<Pair<StateDescriptor, State>>()
    private val signalQueue = LinkedBlockingDeque<Signal>()

    private fun exitLeafState() {
        // Destroy state
        currentStateChain.last().second.exit()
        // Remove state from chain
        currentStateChain.removeLastOrNull()
    }

    private fun exitAll() {
        while (currentStateChain.isNotEmpty()) {
            exitLeafState()
        }
    }

    private fun exitUntil(sd: StateDescriptor) {
        while (currentStateChain.last().first != sd) {
            exitLeafState()
        }
    }

    private fun enterState(sd: StateDescriptor) {

    }

    private fun enterAll(a: List<StateDescriptor>) : State {
        a.forEach {
            currentStateChain.add(Pair(it, it.stateFactory()))
        }
        return currentStateChain.last().second
    }

    private fun enterAfter(a: List<StateDescriptor>, index: Int) : State {
        for (i in index + 1 .. a.size) {
            currentStateChain.add(Pair(a[i], a[i].stateFactory()))
        }
        return currentStateChain.last().second
    }

    private fun transitionToState(state: StateDescriptor) : State {
        val parentChain = buildParentChain(state)
        // Try to find the common state in the chains
        return when (val commonState = findFirstCommonLeaf(parentChain, currentStateChain)) {
            null -> {
                exitAll() // There isn't any common state, exit all
                enterAll(parentChain) // Enter all in the new parentChain
            }
            else -> {
                exitUntil(commonState) // Exit until the common
                enterAfter(parentChain, parentChain.indexOf(commonState)) // Enter the states after the common
            }
        }
    }

    private fun handleSignal(state: State, signal: Signal) {
        when (val action = state.handle(signal)) {
            // The current state has finished handling the signal
            is Action.Handled -> {
                // If a deferred signal exists
                action.deferredSignal?.let { signalQueue.put(it) }
                System.err.println("$name> HANDLED")
            }

            // The current state wants to switch to another state
            is Action.Transition -> {
                System.err.println("$name> TRANSITION: ${action.stateDesc}")
                transitionToState(action.stateDesc)
                action.signalAfterTransition?.let {
                    System.err.println("$name> SIGNAL: $it -> NEW CURRENT STATE")
                    handleSignal(currentStateChain.last().second, it)
                }
            }

            // The current state wants its parent to handle the signal
            is Action.Super -> {
                val currIndex = currentStateChain.indexOfFirst { it.second == state }
                if (0 < currIndex) {
                    System.err.println("$name> SIGNAL: $signal -> PARENT: ${currentStateChain[currIndex - 1].second}")
                    handleSignal(currentStateChain[currIndex - 1].second, signal)
                }
            }
        }
    }

    init {
        thread {
            transitionToState(initialState)
            while (true) {
                // Get one signal from the queue
                System.err.println("$name> WAITING FOR SIGNAL...")
                val signal = signalQueue.take()
                if (signal is Signal.Stop) {
                    break
                }
                // Dispatch the signal to current state, handle resulting action
                System.err.println("$name> SIGNAL: $signal -> CURRENT STATE: ${currentStateChain.last().second}")
                handleSignal(currentStateChain.last().second, signal)
            }
            System.err.println("$name> STOPPED")
        }
    }

    fun queue(signal: Signal) {
        signalQueue.put(signal)
    }

    fun stop() {
        System.err.println("$name> SIGNALING TO STOP")
        signalQueue.put(Signal.Stop)
    }

    fun currentState() : State = currentStateChain.last().second

    companion object {
        /**
         * Returns the states from the ROOT to the given state
         */
        fun buildParentChain(state: StateDescriptor) : List<StateDescriptor> {
            val chain = LinkedList(listOf(state))
            // Add parents to the chain unless null
            for (i in 1..100) {
                when(val parent = chain.first().parentState){
                    null -> { return chain }
                    else -> { chain.addFirst(parent) }
                }
            }
            // If no root has been reached after 100 states, throw
            throw IllegalStateException("Parent chain of a state has more that 100 states")
        }

        /**
         * Returns the first common state in two state chains
         */
        private fun findFirstCommonLeaf(a: List<StateDescriptor>, b: List<Pair<StateDescriptor, State>>) : StateDescriptor? {
            // Try to find the common state in the chains
            return a.reversed().find { aState ->
                b.reversed().any { it.first == aState }
            }
        }
    }
}
