package dev.uslu.hiark

import dev.uslu.hiark.annotations.ActorDecl
import dev.uslu.hiark.annotations.StateDecl
import dev.uslu.hiark.annotations.TransitionDecl
import java.lang.StringBuilder
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.jvmName

typealias TypeTree = MutableMap<String, MutableSet<String>>

// TODO refactor

fun <TDecl : Actor<TDecl>, TImpl : TDecl> graphActor(
    actorDecl: KClass<TDecl>,
    sampleActor: TImpl
): String {
    // List the explicit states
    val states = actorDecl.declaredMemberFunctions.filter {
        it.hasAnnotation<StateDecl>()
    }
    val stateNames = states.map {
        it.name
    }
    val stateHierarchies = states.map {
        sampleActor.probeStateHierarchy(it as State<TDecl>, mutableListOf())
            .map { s ->
                // Extract name of the state from jvmName
                "\\$([_a-zA-Z]+)\\$".toRegex().find(s::class.jvmName)?.groupValues?.get(1)!!
            }
    }

    // Build a tree from root to leaf states
    val tree : TypeTree = mutableMapOf()
    stateHierarchies.forEach { hierarchy ->
        hierarchy.reversed().zipWithNext().forEach{
            if (tree.contains(it.first)) {
                tree[it.first]!!.add(it.second)
            } else {
                tree[it.first] = mutableSetOf(it.second)
            }
        }
    }

    val edges = mutableListOf<Pair<String, String>>()
    actorDecl.nestedClasses
        .filter { it.hasAnnotation<TransitionDecl>() && it.isSealed }
        .forEach {
            val srcState = it.findAnnotation<TransitionDecl>()!!.srcStateName
            it.sealedSubclasses.forEach {transition ->
                val statifiedName = transition.simpleName!!.first().lowercase() + transition.simpleName!!.substring(1)
                // TODO check if statifiedName exists in stateNames
                edges.add(srcState to statifiedName)
            }
        }


    // Draw graph

    val dot = StringBuilder()
    dot.appendLine("digraph G {")

    // Draw subgraphs and declare states
    fun appendRootState(state: String, tree: TypeTree) {
        dot.appendLine("subgraph cluster_${state} {")
        dot.appendLine("label = \"${state}\";")
        dot.appendLine("$state;")
        dot.appendLine("$state [shape=rect];")
        dot.appendLine("color=blue;")
        tree[state]!!.forEach {
            if (tree.contains(it)) {
                // Found another root state
                appendRootState(it, tree)
            } else {
                // Found a leaf state
                dot.appendLine("$it;")
            }
        }
        dot.appendLine("}") // subgraph cluster_state
    }
    appendRootState("root", tree)

    // Draw edges
    edges.forEach {
        dot.appendLine("${it.first} -> ${it.second};")
    }

    dot.appendLine("__start__ -> ${actorDecl.findAnnotation<ActorDecl>()!!.initialStateName};")
    dot.appendLine("__start__ [shape=point];")

    dot.appendLine("}") // digraph G

    return dot.toString()
}