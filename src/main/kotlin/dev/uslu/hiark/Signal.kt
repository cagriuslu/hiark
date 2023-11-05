package dev.uslu.hiark

// Parent of all user defined signals
// If a state handler does not know how to handle a user signal,
// it should return Action.Super. The framework will try handling the signal with the parent
interface Signal {
    // Signifies to the Actor that the Actor should stop
    // This signal is not dispatched to state handlers
    data object Stop : Signal
}
