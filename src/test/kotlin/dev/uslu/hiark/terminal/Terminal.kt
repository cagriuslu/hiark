package dev.uslu.hiark.terminal

import dev.uslu.hiark.*
import dev.uslu.hiark.annotations.ActorDescription
import dev.uslu.hiark.annotations.InitialState
import dev.uslu.hiark.terminal.Terminal.AskForOtp.Companion.askForOtp
import dev.uslu.hiark.terminal.Terminal.BoardedIdle.Companion.boardedIdle
import dev.uslu.hiark.terminal.Terminal.BoardedRoot.Companion.boardedRoot
import dev.uslu.hiark.terminal.Terminal.FetchConfig.Companion.fetchConfig
import dev.uslu.hiark.terminal.Terminal.Transaction.Companion.transaction
import dev.uslu.hiark.terminal.Terminal.TryBoard.Companion.tryBoard
import dev.uslu.hiark.terminal.Terminal.UnboardedIdle.Companion.unboardedIdle

@ActorDescription
class Terminal {

    @InitialState
    class UnboardedIdle : State() {
        sealed class Transition(stateDesc: StateDescriptor) : Action.Transition(stateDesc) {
            class BoardedIdle : UnboardedIdle.Transition(boardedIdle)
            class TryBoard : UnboardedIdle.Transition(tryBoard)
            class AskForOtp : UnboardedIdle.Transition(askForOtp)
        }

        companion object {
            val unboardedIdle = StateDescriptor(::UnboardedIdle, null)
        }
    }

    class TryBoard : State() {
        sealed class Transition(stateDesc: StateDescriptor) : Action.Transition(stateDesc) {
            class UnboardedIdle : TryBoard.Transition(unboardedIdle)
            class FetchConfig : TryBoard.Transition(fetchConfig)
        }

        companion object {
            val tryBoard = StateDescriptor(::TryBoard, unboardedIdle)
        }
    }

    class AskForOtp : State() {
        sealed class Transition(stateDesc: StateDescriptor) : Action.Transition(stateDesc) {
            class UnboardedIdle : AskForOtp.Transition(unboardedIdle)
            class FetchConfig : AskForOtp.Transition(fetchConfig)
        }

        companion object {
            val askForOtp = StateDescriptor(::AskForOtp, unboardedIdle)
        }
    }

    class BoardedRoot : State() {
        sealed class Transition(stateDesc: StateDescriptor) : Action.Transition(stateDesc)

        companion object {
            val boardedRoot = StateDescriptor(::BoardedRoot, null)
        }
    }

    class FetchConfig : State() {
        sealed class Transition(stateDesc: StateDescriptor) : Action.Transition(stateDesc) {
            class UnboardedIdle : FetchConfig.Transition(unboardedIdle)
            class BoardedIdle : FetchConfig.Transition(boardedIdle)
        }

        companion object {
            val fetchConfig = StateDescriptor(::FetchConfig, null)
        }
    }

    class BoardedIdle : State() {
        sealed class Transition(stateDesc: StateDescriptor) : Action.Transition(stateDesc) {
            class Transaction : BoardedIdle.Transition(transaction)
        }

        companion object {
            val boardedIdle = StateDescriptor(::BoardedIdle, boardedRoot)
        }
    }

    class Transaction : State() {
        sealed class Transition(stateDesc: StateDescriptor) : Action.Transition(stateDesc) {
            class BoardedIdle : Transaction.Transition(boardedIdle)
        }

        companion object {
            val transaction = StateDescriptor(::Transaction, boardedIdle)
        }
    }
}