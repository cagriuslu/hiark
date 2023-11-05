package dev.uslu.hiark

import dev.uslu.hiark.annotations.ActorDescription
import dev.uslu.hiark.annotations.InitialState
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmName


class Graph<T : Any>(actorDesc: KClass<T>) {
    class StateAndDescriptor(val state: KClass<State>, val descriptor: StateDescriptor)
    private val states: List<StateAndDescriptor>

    class StateTransition(val from: KClass<State>, val to: KClass<State>)
    private val transitions: MutableList<StateTransition> = mutableListOf()

    private val initialState: StateDescriptor

    private fun descriptorName(stateName: String): String =
        stateName.substring(0, 1).lowercase(Locale.getDefault()) + stateName.substring(1)

    init {
        // Validations
        assert(actorDesc.hasAnnotation<ActorDescription>()) { "Given class does not have ActorDescription annotation" }

        // Gather states
        states = actorDesc.nestedClasses.filter { it.superclasses.contains(State::class) }.map { stateClass ->
            // Check if the state has a companion object
            assert(stateClass.companionObject != null) { "${stateClass.simpleName} state does not have a companion object" }

            // Check if the companion object has the StateDescriptor
            val stateDescriptorProperty = stateClass.companionObject!!.declaredMemberProperties.filter {
                // Property has the correct name
                it.name == descriptorName(stateClass.simpleName!!)
                        // Property implements StateDescriptor
                        && it.returnType.toString() == StateDescriptor::class.jvmName
            }
            assert(stateDescriptorProperty.size == 1) {
                "${stateClass.simpleName}'s companion object does not contain a StateDescriptor with name ${
                    descriptorName(
                        stateClass.simpleName!!
                    )
                }"
            }

            // Call the getter of the property to get the StateDescriptor
            val stateDescriptor =
                stateDescriptorProperty[0].getter.call(stateClass.companionObjectInstance) as StateDescriptor

            // TODO Check that the parent (if exists) is a state of this Actor

            // Map to Pair
            StateAndDescriptor(stateClass as KClass<State>, stateDescriptor)
        }

        // Validate states
        states.forEach { stateAndDescriptor ->
            val state = stateAndDescriptor.state

            // Check if state has a Transition subclass
            val transitionClasses = state.nestedClasses.filter {
                it.isSealed && it.simpleName == "Transition" && it.superclasses.contains(Action.Transition::class)
            }
            assert(transitionClasses.size == 1) { "${state.simpleName} state does not contain a sealed Transition class that implements Action.Transition" }

            // Validate transitions
            transitionClasses[0].sealedSubclasses.forEach { transition ->
                // Check that the transition has the same name as a state
                val transitionedStateClass = states.filter { it.state.simpleName == transition.simpleName }
                assert(transitionedStateClass.size == 1) { "${state.simpleName}.Transition contains a subclass ${transition.simpleName} that doesn't point to any state" }

                // Add transition to map
                transitions.add(StateTransition(state, transitionedStateClass[0].state))

                // Check that the name of the Transition, and the given StateDescriptor point to the same State
                val transitionedStateDesc =
                    transition.memberProperties.filter { it.name == "stateDesc" }[0].getter.call(transition.createInstance()) as StateDescriptor
                val transitionedStateClass2 = states.filter { it.descriptor == transitionedStateDesc }
                assert(transitionedStateClass == transitionedStateClass2) { "${state.simpleName}.Transition.${transitionedStateClass[0].state.simpleName} calls the ${state.simpleName}.Transition with a different state's descriptor" }
            }
        }

        // Validate initial state
        val initialStates = states.filter { it.state.hasAnnotation<InitialState>() }
        assert(initialStates.size == 1) { "Actor must have exactly one InitialState" }
        initialState = initialStates[0].descriptor
    }

    private fun descriptorToStateName(sd: StateDescriptor) : String {
        return "\\$([_a-zA-Z]+)\\$[_a-zA-Z]+\\$[_a-zA-Z]+\\$[0-9]+$".toRegex().find(sd.stateFactory::class.jvmName)?.groupValues?.get(1)!!
    }

    private fun isStateAParent(sd: StateDescriptor) : Boolean = states.any { it.descriptor.parentState == sd }

    private fun appendRootState(out: StringBuilder, stateName: String, indent: Int) {
        out.appendLine("${" ".repeat(indent)}subgraph cluster_${stateName} {")
        out.appendLine("${" ".repeat(indent + 4)}label = \"${stateName}\";")
        out.appendLine("${" ".repeat(indent + 4)}$stateName;")
        out.appendLine("${" ".repeat(indent + 4)}$stateName [shape=rect];")
        out.appendLine("${" ".repeat(indent + 4)}color=blue;")

        // Find each immediate child
        states.filter { state -> state.descriptor.parentState != null && descriptorToStateName(state.descriptor.parentState) == stateName }
            .forEach { child ->
                if (isStateAParent(child.descriptor)) {
                    appendRootState(out, descriptorToStateName(child.descriptor), indent + 4)
                } else {
                    out.appendLine("${" ".repeat(indent + 4)}${descriptorToStateName(child.descriptor)};")
                }
            }
        out.appendLine("${" ".repeat(indent)}}") // subgraph cluster_state
    }

    fun graph(): String {
        val dot = StringBuilder()
        dot.appendLine("digraph G {")

        // Draw subgraphs and declare states
        states
            .filter { it.descriptor.parentState == null } // For each state that doesn't have a parent
            .forEach { appendRootState(dot, descriptorToStateName(it.descriptor), 4) } // Add one root state

        // Draw transitions
        transitions.forEach {
            dot.appendLine("    ${it.from.simpleName} -> ${it.to.simpleName};")
        }

        // Draw initial transition
        dot.appendLine("    __start__ -> ${descriptorToStateName(initialState)};")
        dot.appendLine("    __start__ [shape=point];")

        // End graph
        dot.appendLine("}") // digraph G
        return dot.toString()
    }
}
