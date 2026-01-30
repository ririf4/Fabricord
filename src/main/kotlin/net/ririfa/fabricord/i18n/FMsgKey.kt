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

            object ErrorDuringSendingModernMessage : Bot()
        }

        sealed class Modal : Discord() {
            sealed class LINK : Modal() {
                object Title : LINK()
                object LinkedSuccessfully : LINK()

                object Invalid : LINK()
            }
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

        sealed class Command : Bot() {
            sealed class Kick : Command() {
                object SentKickPacket : Kick()

                object NoPermission : Kick()
            }

            sealed class Ban : Command() {
                object SendBanPacket : Ban()

                object NoPermission : Ban()
            }

            sealed class Pardon : Command() {
                object SentPardonPacket : Pardon()

                object NoPermission : Pardon()
            }

            object PlayerNotFound : Command()
            object NoLinkedAccount : Command()
            object CannotGetPlayerPerm : Command()
        }
    }

    sealed class Command : FMsgKey() {
        sealed class LC : Command() {
            sealed class State : LC() {
                object ON : State()
                object OFF : State()
            }

            object SwitchedLocalChatState : LC()
        }

        sealed class LINK : Command() {
            object IssuedCode : LINK()
        }
    }

    sealed class Chat : FMsgKey() {
        object LinkDiscordAccountFirst : Chat()
    }
}