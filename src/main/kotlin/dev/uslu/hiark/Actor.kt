package dev.uslu.hiark

import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread
import kotlin.system.exitProcess

typealias State<T> = T.(Signal) -> Action<T>

@Suppress("UNCHECKED_CAST") // Casts from this to T is already ensured to succeed via `T: Actor<T>`
abstract class Actor<T: Actor<T>>(initialState: State<T>, val name: String) {
    var currentState : State<T> = Actor<T>::root
        private set
    private val signalQueue = LinkedBlockingDeque<Signal>()

    private fun handleAction(action: Action<T>, currentSignal: Signal) {
        fun abort(msg: String) {
            System.err.println(msg)
            exitProcess(-1)
        }

        when (action) {
            // The current state has finished handling the signal
            is Action.Handled -> {
                // If a deferred signal exists
                action.deferredSignal?.let { signalQueue.put(it) }

                System.err.println("$name> HANDLED")
            }

            // The current state wants to switch to another state
            is Action.Transition -> {
                System.err.println("$name> TRANSITION: ${action.state.toString().trimWarning()}")

                // Transition to root state is not allowed
                if (action.state == Actor<T>::root) {
                    // TODO check if the user has implemented a custom root state
                    abort("Implementation Error! $currentState tried to transition to rootState.")
                }

                // Probe state to find the common parent
                val srcStateHierarchy = probeStateHierarchy(currentState, mutableListOf(currentState))
                val dstStateHierarchy = probeStateHierarchy(action.state, mutableListOf(action.state))
                val srcStateHierarchyUntilCommon = srcStateHierarchy.retainUntilFirstCommon(dstStateHierarchy)
                val dstStateHierarchyUntilCommon = dstStateHierarchy.retainUntilFirstCommon(srcStateHierarchy)

                // Exit from src states
                srcStateHierarchyUntilCommon.forEach {
                    System.err.println("$name> EXIT: ${it.toString().trimWarning()}")
                    val exitAction = it(this as T, Signal.Exit)
                    if (exitAction !is Action.Handled<T>) {
                        abort("Implementation Error! $it returned an action other than Halt for Signal.Exit while transitioning.")
                    } else {
                        exitAction.deferredSignal?.let { s -> signalQueue.put(s) }
                    }
                }
                // Enter into dst states
                dstStateHierarchyUntilCommon.subList(1, dstStateHierarchyUntilCommon.size).reversed().forEach {
                    System.err.println("$name> ENTER: ${it.toString().trimWarning()}")
                    val enterAction = it(this as T, Signal.Enter)
                    if (enterAction !is Action.Handled<T>) {
                        abort("Implementation Error! $it returned an action other than Halt for Signal.Enter while transitioning.")
                    } else {
                        enterAction.deferredSignal?.let { s -> signalQueue.put(s) }
                    }
                }

                // Switch state
                currentState = action.state
                System.err.println("$name> ENTER NEW CURRENT STATE: ${currentState.toString().trimWarning()}")
                // Enter into switched state
                val enterAction = currentState(this as T, Signal.Enter)
                handleAction(enterAction, Signal.Enter)

                // If the is stable and if the original action had a `withSignal` parameter
                if (enterAction is Action.Handled) {
                    action.signalAfterTransition?.let {
                        System.err.println("$name> SIGNAL: $it -> NEW CURRENT STATE")
                        handleAction(currentState(this as T, it), it)
                    }
                }
            }

            // The current state wants its parent to handle the signal
            is Action.Super -> {
                // If current state isn't already the root state
                if (currentState != action.state) {
                    System.err.println("$name> SIGNAL: $currentSignal -> PARENT: ${action.state.toString().trimWarning()}")
                    handleAction(action.state(this as T, currentSignal), currentSignal)
                }
            }
        }
    }

    fun probeStateHierarchy(state: State<T>, hierarchy : MutableList<State<T>>) : MutableList<State<T>> {
        fun abort(msg: String) {
            System.err.println(msg)
            System.err.println(hierarchy)
            exitProcess(-1)
        }

        when (val superAction = state(this as T, Signal.Probe)) {
            // Keep probing
            is Action.Super -> {
                return if (state == superAction.state) {
                    hierarchy // Return from recursion
                } else {
                    hierarchy.add(superAction.state)
                    probeStateHierarchy(superAction.state, hierarchy)
                }
            }

            else -> abort("Implementation error! State returns non-Super Action. All states must propagate Probe signal to its parent.")
        }

        // Unreachable code
        return mutableListOf()
    }

    init {
        thread {
            // Since Action.Transition is abstract, we need to create a dummy class for initial transition
            class InitialTransition : Action.Transition<T>(initialState)
            handleAction(InitialTransition(), Signal.Enter)

            while (true) {
                // Get one signal from the queue
                System.err.println("$name> WAITING FOR SIGNAL...")
                val signal = signalQueue.take()
                if (signal is Signal.Stop) {
                    break;
                }

                // Dispatch the signal to current state, handle resulting action
                System.err.println("$name> SIGNAL: $signal -> CURRENT STATE: ${currentState.toString().trimWarning()}")
                handleAction(currentState(this as T, signal), signal)
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

    // An example root state that can be used by implementations
    // The implementation of this function can also be used as a skeleton for new states
    fun root(signal: Signal) : Action<T> {
        return when (signal) {
            is Signal.Enter, is Signal.Exit -> Action.Handled()
            else -> Action.Super(Actor<T>::root)
        }
    }
}