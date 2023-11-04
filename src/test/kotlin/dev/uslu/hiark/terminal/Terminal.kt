package dev.uslu.hiark.terminal

import dev.uslu.hiark.*
import dev.uslu.hiark.annotations.ActorDecl
import dev.uslu.hiark.annotations.StateDecl
import dev.uslu.hiark.annotations.TransitionDecl

// This file can be auto-analyzed

typealias TerminalState = State<Terminal>

@ActorDecl("bootUp")
abstract class Terminal(name: String) : Actor<Terminal>(Terminal::bootUp, name) {

    @StateDecl
    abstract fun unboarded(signal: Signal) : Action<Terminal>

    @StateDecl
    abstract fun boarded(signal: Signal) : Action<Terminal>

    @StateDecl
    abstract fun bootUp(signal: Signal) : Action<Terminal>
    @TransitionDecl("bootUp")
    sealed class BootUpTransition(state: TerminalState) : Action.Transition<Terminal>(state) {
        class Idle : BootUpTransition(Terminal::idle)
        class Boarding : BootUpTransition(Terminal::boarding)
    }

    @StateDecl
    abstract fun idle(signal: Signal) : Action<Terminal>
    @TransitionDecl("idle")
    sealed class IdleTransition(state: TerminalState) : Action.Transition<Terminal>(state) {
        class ShopperInteraction : IdleTransition(Terminal::shopperInteraction)
        class AdminMenu : IdleTransition(Terminal::adminMenu)
    }

    @StateDecl
    abstract fun busyMaintenance(signal: Signal) : Action<Terminal>
    @TransitionDecl("busyMaintenance")
    sealed class BusyMaintenanceTransition(state: TerminalState) : Action.Transition<Terminal>(state) {
        class Idle : BusyMaintenanceTransition(Terminal::idle)
    }

    @StateDecl
    abstract fun shopperInteraction(signal: Signal) : Action<Terminal>

    @StateDecl
    abstract fun boarding(signal: Signal) : Action<Terminal>
    @TransitionDecl("boarding")
    sealed class BoardingTransition(state: TerminalState) : Action.Transition<Terminal>(state) {
        class FetchConfig : BoardingTransition(Terminal::fetchConfig)
        class AskOtp : BoardingTransition(Terminal::askOtp)
    }

    @StateDecl
    abstract fun askOtp(signal: Signal) : Action<Terminal>
    @TransitionDecl("askOtp")
    sealed class AskOtpTransition(state: TerminalState) : Action.Transition<Terminal>(state) {
        class FetchConfig : AskOtpTransition(Terminal::fetchConfig)
    }

    @StateDecl
    abstract fun fetchConfig(signal: Signal) : Action<Terminal>
    @TransitionDecl("fetchConfig")
    sealed class FetchConfigTransition(state: TerminalState) : Action.Transition<Terminal>(state) {
        class Idle : FetchConfigTransition(Terminal::idle)
    }

    @StateDecl
    abstract fun adminMenu(signal: Signal) : Action<Terminal>
    @TransitionDecl("adminMenu")
    sealed class AdminMenuTransition(state: TerminalState) : Action.Transition<Terminal>(state) {
        class Idle : AdminMenuTransition(Terminal::idle)
    }
}