package net.ririfa.fabricord.i18n

import net.minecraft.text.Text
import net.ririfa.langman.MessageKey

sealed class FabricordMessageKey : MessageKey<FabricordMessageProvider, Text> {
    sealed class System : FabricordMessageKey() {
        sealed class Discord : System() {
            object ErrorDuringSendingModernMessage : Discord()
        }
    }

    sealed class Discord : FabricordMessageKey() {
        sealed class Config : Discord() {
            object LogChannelIDIsBlank : Config()
        }

        sealed class Bot : Discord() {
            object BotNowOnline : Bot()
            object BotNowOffline : Bot()

            object CannotStartBot : Bot()
            object CannotStopBot : Bot()
            object TimedOutForStoppingBot : Bot()

            object CannotLoginToBot : Bot()
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

    sealed class Command : FabricordMessageKey() {
        sealed class LC : Command() {
            object SwitchedLocalChatState : LC()

            sealed class State : LC() {
                object ON : State()
                object OFF : State()
            }
        }
    }

    sealed class Chat : FabricordMessageKey() {
        object LinkDiscordAccountFirst : Chat()
    }
}