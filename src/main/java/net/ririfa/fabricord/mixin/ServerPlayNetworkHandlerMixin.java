package net.ririfa.fabricord.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ririfa.fabricord.command.CommandManager;
import net.ririfa.fabricord.database.DataBase;
import net.ririfa.fabricord.discord.DiscordBotManager;
import net.ririfa.fabricord.discord.DiscordPlayerEventHandler;
import net.ririfa.fabricord.i18n.FMsgKey;
import net.ririfa.fabricord.i18n.FMsgProvider;
import net.ririfa.fabricord.i18n.FMsgProviderKt;
import net.ririfa.fabricord.util.Aliases;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void interceptChatMessage(@NotNull ChatMessageC2SPacket packet, CallbackInfo ci) {
        if (!DiscordBotManager.isBotInitialized || Aliases.getConfig().logChannelID == null || !Aliases.getConfig().sendChat()) return;

        UUID playerUUID = player.getUuid();
        FMsgProvider ap = FMsgProviderKt.adapt(player);

        // If `useUserPermissionsForMention` is enabled, check if the user has linked their Discord account first
        if (!DataBase.isUserLinked(playerUUID) && Aliases.getConfig().useUserPermissionForMentions) {
            player.sendMessage(ap.getMessage(FMsgKey.Chat.LinkDiscordAccountFirst.INSTANCE));
            ci.cancel();
            return;
        }

        // And then, continue for handling the chat message
        String message = packet.chatMessage();

        if (!CommandManager.localChatToggled.contains(playerUUID)) {
            DiscordPlayerEventHandler.INSTANCE.handleMCMessage(player, message);
        }
    }
}