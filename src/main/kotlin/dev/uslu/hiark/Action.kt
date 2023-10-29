package dev.uslu.hiark

sealed class Action<T> {
    /// Signifies that the actor has finished handling the current signal
    class Handled<T> : Action<T>() {
        var deferredSignal: UserSignal? = null
            private set

        /// A new signal may be given to be queued at the back of the signal queue
        fun withDeferredSignal(signal: UserSignal) : Handled<T> {
            deferredSignal = signal
            return this
        }
    }

    /// Signifies that the actor wants to transition states
    abstract class Transition<T>(val state: State<T>) : Action<T>() {
        var signalAfterTransition: UserSignal? = null
            private set

        /// A new signal may be given to be queued for the newly transitioned state
        fun withSignalAfterTransition(signal: UserSignal) : Transition<T> {
            signalAfterTransition = signal
            return this
        }

        override fun toString(): String {
            return "Action.Transition(${state}, ${signalAfterTransition})"
        }
    }

    /// Signifies that the actor wants the signal to be handled by the super state
    class Super<T>(val state: State<T>) : Action<T>()
}