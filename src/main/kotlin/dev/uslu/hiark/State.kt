package dev.uslu.hiark

abstract class State {
    open fun enter() : Action.Transition? { return null }

    open fun handle(signal: Signal) : Action { return Action.Handled() }

    open fun exit() {}
}

class StateDescriptor(
    val stateFactory: () -> State,
    val parentState: StateDescriptor?
) {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is StateDescriptor -> {
                return stateFactory == other.stateFactory && parentState == other.parentState
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        var result = stateFactory.hashCode()
        result = 31 * result + (parentState?.hashCode() ?: 0)
        return result
    }
}
