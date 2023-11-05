package dev.uslu.hiark

sealed class Action {
    /// Signifies that the state has finished handling the current signal
    class Handled : Action() {
        var deferredSignal: Signal? = null
            private set

        /// A new signal may be given to be queued at the back of the signal queue
        fun withDeferredSignal(signal: Signal) : Handled {
            deferredSignal = signal
            return this
        }
    }

    /// Signifies that the state wants to transition to another state
    abstract class Transition(val stateDesc: StateDescriptor) : Action() {
        var signalAfterTransition: Signal? = null
            private set

        /// A new signal may be given to be queued for the newly transitioned state
        fun withSignalAfterTransition(signal: Signal) : Transition {
            signalAfterTransition = signal
            return this
        }
    }

    /// Signifies that the state wants the signal to be handled by the parent state
    class Super : Action()
}
