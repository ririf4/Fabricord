package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.text.*
import net.ririfa.fabricord.Config
import net.ririfa.fabricord.FT
import net.ririfa.fabricord.Server
import net.ririfa.fabricord.util.playSoundToPlayerMaster
import net.ririfa.fabricord.util.replaceUUIDsWithMCIDs
import java.awt.Color
import java.net.URI

object DiscordMessageHandler {
	fun handleDiscordMessage(event: MessageReceivedEvent) {
		FT {
			val message: MutableText = createMessage(event, false, null) ?: return@FT
			sendToAllPlayers(message)
		}
	}

	fun handleMentionedDiscordMessage(event: MessageReceivedEvent, mentionedPlayers: List<ServerPlayerEntity>, foundUUID: Boolean) {
		FT {
			val updatedMessageContent = replaceUUIDsWithMCIDs(event.message.contentRaw, Server.playerManager.playerList)

			mentionedPlayers.forEach { player ->
				player.playSoundToPlayerMaster(SoundEvents.BLOCK_NOTE_BLOCK_PLING.comp_349(), 2.0f, 2.0f)
			}

			val mentionMessage: Text =
				createMessage(event, true, if (foundUUID) updatedMessageContent.first else event.message.contentRaw) ?: return@FT
			val generalMessage: Text =
				createMessage(event, false, if (foundUUID) updatedMessageContent.first else event.message.contentRaw) ?: return@FT

			mentionedPlayers.forEach { player ->
				player.sendMessage(mentionMessage, false)
			}

			val nonMentionedPlayers = if (foundUUID) updatedMessageContent.second else mentionedPlayers
			sendToAllPlayersExcept(generalMessage, nonMentionedPlayers)
		}
	}

	private fun sendToAllPlayers(message: Text) {
		Server.playerManager.playerList.forEach { player ->
			player.sendMessage(message, false)
		}
	}

	private fun sendToAllPlayersExcept(message: Text, excludePlayers: List<ServerPlayerEntity>) {
		Server.playerManager.playerList.forEach { player ->
			if (player !in excludePlayers) {
				player.sendMessage(message, false)
			}
		}
	}

	private fun createMessage(event: MessageReceivedEvent, isMention: Boolean, updatedContent: String?): MutableText? {
		val channelId: String = Config.logChannelID ?: return null
		if (event.channel.id != channelId || event.author.isBot) {
			return null
		}

		val guildName = event.guild.name
		val member = event.member
		val memberName = member?.effectiveName ?: member?.user?.globalName ?: member?.user?.name ?: "Unknown"
		val memberId = member?.user?.id ?: "00000000000000000000"
		val idSuggest = "<@$memberId>"
		val highestRole = member?.roles?.maxByOrNull { it.position }
		val roleName = highestRole?.name
		val rId = highestRole?.id ?: "00000000000000000000"
		val rIdSuggest = rId.let { "<@&$it>" }
		val roleColor = highestRole?.color ?: Color.WHITE
		val roleTextColor = TextColor.fromRgb((roleColor.red shl 16) or (roleColor.green shl 8) or roleColor.blue)

		val discordText = Text.literal("Discord")
			.styled {
				it.withColor(0x55CDFC)
					.withHoverEvent(HoverEvent.ShowText(Text.of(guildName)))
					.withClickEvent(ClickEvent.OpenUrl(URI.create("https://discord.com/channels/${event.guild.id}/${event.channel.id}")))
			}

		val roleText = roleName?.let {
			Text.literal(it)
				.styled {
					it.withColor(roleTextColor)
						.withHoverEvent(HoverEvent.ShowText(Text.of("ID: $rId")))
						.withClickEvent(ClickEvent.SuggestCommand(rIdSuggest))
				}
		}

		val memberText = Text.literal(memberName)
			.styled {
				it.withColor(0xFFFFFF)
					.withHoverEvent(HoverEvent.ShowText(Text.of("ID: $memberId")))
					.withClickEvent(ClickEvent.SuggestCommand(idSuggest))
			}

		val messageContent = updatedContent ?: event.message.contentDisplay
		val messageText = parseMessageWithLinks(messageContent, isMention)

		val text = Text.literal("[").styled { it.withColor(0xFFFFFF) }
			.append(discordText)

		if (roleText != null) {
			text.append(Text.literal(" | ").styled { it.withColor(0xFFFFFF) })
			text.append(roleText)
		}

		text.append(Text.literal("] ").styled { it.withColor(0xFFFFFF) })
			.append(memberText)
			.append(Text.literal(" » ").styled { it.withColor(0xFFFFFF) })
			.append(messageText)

		return text
	}

	private fun parseMessageWithLinks(message: String, isMention: Boolean): Text {
		val urlRegex = Regex("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+)")
		val text = Text.empty()

		var lastIndex = 0
		for (match in urlRegex.findAll(message)) {
			val url = match.value
			val startIndex = match.range.first

			if (lastIndex < startIndex) {
				text.append(Text.literal(message.substring(lastIndex, startIndex)))
			}

			val displayUrl = if (url.length > 30) url.take(30) + "…" else url

			val clickableUrl = Text.literal(displayUrl)
				.styled {
					it.withColor(0x55CDFC)
						.withUnderline(true)
						.withClickEvent(ClickEvent.OpenUrl(URI.create(url)))
						.withHoverEvent(HoverEvent.ShowText(Text.of(url)))
				}

			text.append(clickableUrl)
			lastIndex = match.range.last + 1
		}

		if (lastIndex < message.length) {
			text.append(Text.literal(message.substring(lastIndex)))
		}

		return if (isMention) text.styled { it.withBold(true) } else text
	}

}