package net.ririfa.fabricord.translation

import net.minecraft.text.Text
import net.ririfa.fabricord.annotations.Indexed
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

			object WebHookUrlIsNotConfiguredOrInvalid : Config()
		}
	}

	sealed class Command : FabricordMessageKey() {
		object ThisCommandIsPlayerOnly : Command()

		object Page : Help()

		sealed class LC : Command() {
			object SwitchedLocalChatState : LC()
		}

		sealed class Group : Command() {
			sealed class Create : Group() {
				object GroupAlreadyExists : Create()
				object Success : Create()
			}
		}

		@Suppress("unused")
		sealed class Help : Command() {
			sealed class Group : Help() {
				// /grp create <name|str> <open|bool> [(OPTIONAL) players|ServerPlayerEntity]
				@Indexed(1)
				sealed class Create : Group() {
					object About : Create()
					object Usage : Create()
				}

				// /grp delete <name or ID|str>
				@Indexed(2)
				sealed class Delete : Group() {
					object About : Delete()
					object Usage : Delete()
				}

				// /grp join <name or ID|str>
				@Indexed(3)
				sealed class Join : Group() {
					object About : Join()
					object Usage : Join()
				}

				// /grp leave <name or ID|str>
				@Indexed(4)
				sealed class Leave : Group() {
					object About : Leave()
					object Usage : Leave()
				}

				// /grp list
				@Indexed(5)
				sealed class List : Group() {
					object About : List()
					object Usage : List()
				}

				// /grp joinedList
				@Indexed(6)
				sealed class JoinedList : Group() {
					object About : JoinedList()
					object Usage : JoinedList()
				}

				// /grp pendingList <(OPTIONAL) name or ID|str>
				@Indexed(7)
				sealed class PendingList : Group() {
					object About : PendingList()
					object Usage : PendingList()
				}

				// /grp info <name or ID|str>
				@Indexed(8)
				sealed class Info : Group() {
					object About : Info()
					object Usage : Info()
				}

				// /grp setDefault <name or ID|str>
				@Indexed(9)
				sealed class SetDefaultGrp : Group() {
					object About : SetDefaultGrp()
					object Usage : SetDefaultGrp()
				}

				// /grp switch <[OPTIONAL] name or ID|str>
				@Indexed(10)
				sealed class Switch : Group() {
					object About : Switch()
					object Usage : Switch()
				}

				// /grp invite <name or ID|str> <player|ServerPlayerEntity>
				@Indexed(11)
				sealed class Invite : Group() {
					object About : Invite()
					object Usage : Invite()
				}

				// /grp kick <name or ID|str> <player|ServerPlayerEntity>
				@Indexed(12)
				sealed class Kick : Group() {
					object About : Kick()
					object Usage : Kick()
				}

				// /grp /grp acceptRequest <pendingID|str>
				@Indexed(13)
				sealed class AcceptRequest : Group() {
					object About : AcceptRequest()
					object Usage : AcceptRequest()
				}

				// /grp /grp denyRequest <pendingID|str>
				@Indexed(14)
				sealed class DenyRequest : Group() {
					object About : DenyRequest()
					object Usage : DenyRequest()
				}

				// /grp setPublic <name or ID|str> <public|bool>
				@Indexed(15)
				sealed class SetPublic : Group() {
					object About : SetPublic()
					object Usage : SetPublic()
				}

				// /grp transferOwner <name or ID|str> <MCID or UUID|str>
				@Indexed(16)
				sealed class TransferOwner : Group() {
					object About : TransferOwner()
					object Usage : TransferOwner()
				}
			}
		}
	}
}