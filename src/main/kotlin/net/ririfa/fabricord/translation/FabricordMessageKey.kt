package net.ririfa.fabricord.translation

import net.minecraft.text.Text
import net.ririfa.langman.MessageKey

sealed class FabricordMessageKey : MessageKey<FabricordMessageProvider, Text> {
    sealed class System : FabricordMessageKey() {
        sealed class Initialization : System() {
            object FailedToCheckOrCreateRequiredDirOrFileBySec : Initialization()
            object FailedToCheckOrCreateRequiredDirOrFileByIO : Initialization()
            object FailedToCheckOrCreateRequiredDirOrFile : Initialization()

            sealed class DirectoriesAndFiles : Initialization() {
                object ModDirDoesNotExist : DirectoriesAndFiles()
            }
        }

        sealed class Discord : System() {
            object ErrorDuringSendingModernMessage : Discord()
        }

        sealed class GRP : System() {
            object GroupedChatMessageBase : GRP()
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

    sealed class Exception : FabricordMessageKey() {
        sealed class Config : Exception() {
            object RequiredPropertyIsNotConfigured : Config()
            object SoftRequiredPropertyIsNotConfigured : Config()
        }
    }

    sealed class Command : FabricordMessageKey() {
        object ThisCommandIsPlayerOnly : Command()

        sealed class LC : Command() {
            object SwitchedLocalChatState : LC()
        }
    }
}