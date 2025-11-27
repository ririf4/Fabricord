package net.ririfa.fabricord.discord

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundEvents
import net.minecraft.text.*
import net.ririfa.fabricord.util.*
import java.awt.Color
import java.net.URI

object DiscordMessageHandler {
    private val urlRegex = Regex("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+)")

    fun handleDiscordMessage(event: MessageReceivedEvent) {
        FT {
            createMessage(event, isMention = false)?.let { sendToAllPlayers(it) }
        }
    }

    fun handleMentionedDiscordMessage(
        event: MessageReceivedEvent,
        mentionedPlayers: List<ServerPlayerEntity>,
        foundUUID: Boolean
    ) {
        FT {
            val rawMsg = event.message.contentRaw
            val (newContent, nonMentioned) =
                if (foundUUID) replaceUUIDsWithMCIDs(rawMsg, Server.playerManager.playerList)
                else rawMsg to emptyList()

            // Sound feedback
            mentionedPlayers.forEach { it.playSoundToPlayerMaster(SoundEvents.BLOCK_NOTE_BLOCK_PLING.comp_349(), 2f, 2f) }

            val mentionMessage = createMessage(event, isMention = true, contentOverride = newContent)
                ?: return@FT

            val generalMessage = createMessage(event, isMention = false, contentOverride = newContent)
                ?: return@FT

            mentionedPlayers.forEach { it.sendMessage(mentionMessage, false) }
            val excluded = if (foundUUID) nonMentioned else mentionedPlayers
            sendToAllPlayersExcept(generalMessage, excluded)
        }
    }

    private fun sendToAllPlayers(message: Text) {
        Server.playerManager.playerList.forEach { it.sendMessage(message, false) }
    }

    private fun sendToAllPlayersExcept(message: Text, excluded: List<ServerPlayerEntity>) {
        Server.playerManager.playerList.forEach { if (it !in excluded) it.sendMessage(message, false) }
    }

    private fun createMessage(
        event: MessageReceivedEvent,
        isMention: Boolean,
        contentOverride: String? = null
    ): MutableText? {
        val channelId = Config.logChannelID ?: return null
        if (event.channel.id != channelId || event.author.isBot) return null

        val guild = event.guild
        val member = event.member
        val memberName =
            member?.effectiveName ?: member?.user?.globalName ?: member?.user?.name ?: "Unknown"

        val role = member?.roles?.maxByOrNull { it.position }
        val roleColor = role?.color?.let(::toTextColor) ?: TextColor.fromRgb(0xFFFFFF)

        val base = prefix(
            guildName = guild.name,
            guildId = guild.id,
            channelId = event.channel.id,
            roleName = role?.name,
            roleColor = roleColor,
            roleId = role?.id,
            memberName = memberName,
            memberId = member?.user?.id
        )

        val content = parseMsgLinks(contentOverride ?: event.message.contentDisplay)

        if (isMention) content.styled { it.withBold(true) }
        return base.append(content)
    }

    private fun prefix(
        guildName: String,
        guildId: String,
        channelId: String,
        roleName: String?,
        roleColor: TextColor,
        roleId: String?,
        memberName: String,
        memberId: String?
    ): MutableText {
        val discord = literal("Discord", 0x55CDFC)
            .hover("Server: $guildName")
            .click(ClickEvent.OpenUrl(URI.create("https://discord.com/channels/$guildId/$channelId")))

        val role = roleName?.let {
            literal(it, roleColor)
                .hover("ID: $roleId")
                .click(ClickEvent.SuggestCommand("<@&$roleId>"))
        }

        val user = literal(memberName, 0xFFFFFF)
            .hover("ID: $memberId")
            .click(ClickEvent.SuggestCommand("<@$memberId>"))

        val text = Text.literal("[ ").styled { it.withColor(0xFFFFFF) }
            .append(discord)

        role?.let {
            text.append(Text.literal(" | ").styled { it.withColor(0xFFFFFF) })
                .append(it)
        }

        return text.append(Text.literal(" ] ").styled { it.withColor(0xFFFFFF) })
            .append(user)
            .append(Text.literal(" » ").styled { it.withColor(0xFFFFFF) })
    }

    private fun parseMsgLinks(msg: String): MutableText {
        val result = Text.empty()
        var last = 0

        for (match in urlRegex.findAll(msg)) {
            val url = match.value
            val start = match.range.first
            if (last < start) result.append(Text.literal(msg.substring(last, start)))

            val shown = if (url.length > 30) url.take(30) + "…" else url
            result.append(
                literal(shown, 0x55CDFC)
                    .underline()
                    .hover(url)
                    .click(ClickEvent.OpenUrl(URI.create(url)))
            )
            last = match.range.last + 1
        }

        if (last < msg.length) result.append(Text.literal(msg.substring(last)))
        return result
    }

    private fun toTextColor(color: Color): TextColor =
        TextColor.fromRgb((color.red shl 16) or (color.green shl 8) or color.blue)
}

/* small extension helpers */
private fun literal(str: String, color: Int) = Text.literal(str).styled { it.withColor(color) }
private fun literal(str: String, color: TextColor) = Text.literal(str).styled { it.withColor(color) }
private fun MutableText.hover(str: String) = styled { it.withHoverEvent(HoverEvent.ShowText(Text.literal(str))) }
private fun MutableText.click(event: ClickEvent) = styled { it.withClickEvent(event) }
private fun MutableText.underline() = styled { it.withUnderline(true) }
