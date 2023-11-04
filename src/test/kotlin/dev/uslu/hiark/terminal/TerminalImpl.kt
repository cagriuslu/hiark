package dev.uslu.hiark.terminal

import dev.uslu.hiark.Action
import dev.uslu.hiark.Signal
import dev.uslu.hiark.UserSignal
import dev.uslu.hiark.annotations.ActorImpl

@ActorImpl
class TerminalImpl(
    var boarded: Boolean,
    val autoBoard: Boolean
) : Terminal("Terminal") {

    // User signals
    class Certificate : UserSignal
    class Otp : UserSignal
    class ConfigResponse : UserSignal

    override fun unboarded(signal: Signal): Action<Terminal> = when (signal) {
        is Signal.Enter, is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::root)
    }

    override fun boarded(signal: Signal): Action<Terminal> = when (signal) {
        is Signal.Enter, is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::root)
    }

    override fun bootUp(signal: Signal): Action<Terminal> = when (signal) {
        is Signal.Enter -> {
            if (boarded) {
                BootUpTransition.Idle()
            } else {
                BootUpTransition.Boarding()
            }
        }

        is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::unboarded)
    }

    override fun idle(signal: Signal): Action<Terminal> = when (signal) {
        is Signal.Enter, is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::boarded)
    }

    override fun busyMaintenance(signal: Signal): Action<Terminal> = when (signal) {
        is Signal.Enter, is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::boarded)
    }

    override fun shopperInteraction(signal: Signal): Action<Terminal> = when(signal) {
        is Signal.Enter, is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::idle)
    }



    override fun boarding(signal: Signal): Action<Terminal> = when (signal) {
        is Signal.Enter -> {
            if (autoBoard) {
                BoardingTransition.FetchConfig()
                    .withSignalAfterTransition(Certificate())
            } else {
                BoardingTransition.AskOtp()
            }
        }

        is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::unboarded)
    }

    override fun askOtp(signal: Signal): Action<Terminal> = when (signal) {
        is Signal.Enter -> {
            println("ASK_OTP UI REQUEST")
            Action.Handled<Terminal>().withDeferredSignal(Otp())
        }

        is Otp -> AskOtpTransition.FetchConfig()
            .withSignalAfterTransition(Otp())

        is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::unboarded)
    }

    override fun fetchConfig(signal: Signal): Action<Terminal> = when (signal) {
        is Certificate -> {
            println("FETCH_CONFIG HTTP REQUEST w/ CERTIFICATE")
            Action.Handled<Terminal>().withDeferredSignal(ConfigResponse())
        }

        is Otp -> {
            println("FETCH_CONFIG HTTP REQUEST w/ OTP")
            Action.Handled<Terminal>().withDeferredSignal(ConfigResponse())
        }

        is ConfigResponse -> {
            boarded = true
            FetchConfigTransition.Idle()
        }

        is Signal.Enter, is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::unboarded)
    }

    override fun adminMenu(signal: Signal): Action<Terminal> = when (signal) {
        is Signal.Enter, is Signal.Exit -> Action.Handled()
        else -> Action.Super(Terminal::boarded)
    }

}