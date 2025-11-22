package net.ririfa.fabricord.i18n

import net.minecraft.text.Text
import net.ririfa.langman.MessageKey

sealed class FMsgKey : MessageKey<FMsgProvider, Text> {
    sealed class Discord : FMsgKey() {
        sealed class Bot : Discord() {
            object BotNowOnline : Bot()
            object BotNowOffline : Bot()

            object CannotLoginToBot : Bot()
            object CannotStartBot : Bot()

            object TimedOutForStoppingBot : Bot()
            object CannotStopBot : Bot()
        }

        sealed class Embed : Discord() {
            sealed class PlayerList : Embed() {
                object Title : PlayerList()
                object Description : PlayerList()

                object ThereAreNoPlayersOnline : PlayerList()
            }

            sealed class ServerStatus : Embed() {
                object Title : ServerStatus()
                sealed class Description : ServerStatus() {
                    object MemoryUsage : Description()
                }
            }
        }
    }
}