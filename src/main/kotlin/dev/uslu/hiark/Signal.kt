package dev.uslu.hiark

sealed interface Signal {
    // Signifies that the actor has entered the state
    // The handler must return Action.Handled except for the target state
    data object Enter : Signal

    // Signifies that the actor is exiting the state
    // The handler must return Action.Handled
    data object Exit : Signal

    // Internal signal used by the framework to learn the hierarchy of a state
    // The handler must return Action.Super with the correct parent state
    // The top-most parent should be Actor<T>::root
    data object Probe : Signal

    // Signifies to the Actor that the Actor should stop
    // This signal is not dispatched to state handlers
    data object Stop : Signal
}

// Parent of all user defined signals
// If a state handler does not know how to handle a user signal,
// it should return Action.Super. The framework will try handling the signal with the parent
interface UserSignal : Signal
