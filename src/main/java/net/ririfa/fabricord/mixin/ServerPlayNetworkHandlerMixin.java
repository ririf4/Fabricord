package net.ririfa.fabricord.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ririfa.fabricord.AliasKt;
import net.ririfa.fabricord.CommandManager;
import net.ririfa.fabricord.database.tables.DiscordMinecraftLink;
import net.ririfa.fabricord.discord.DiscordBotManager;
import net.ririfa.fabricord.discord.DiscordPlayerEventHandler;
import net.ririfa.fabricord.translation.FabricordMessageKey;
import net.ririfa.fabricord.translation.FabricordMessageProvider;
import net.ririfa.fabricord.translation.FabricordMessageProviderKt;
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
        if (!DiscordBotManager.isBotInitialized || AliasKt.getConfig().isLogChannelIDNotSet || Boolean.TRUE.equals(AliasKt.getConfig().dontSendChatToDiscord))
            return;

        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.player;
        FabricordMessageProvider ap = FabricordMessageProviderKt.adapt(player);
        UUID playerUUID = player.getUuid();

        if (!DiscordMinecraftLink.isUserLinked(playerUUID) && Boolean.TRUE.equals(AliasKt.getConfig().useUserPermissionForMention)) {
            // Discordユーザー権限を使用する設定にも関わらずりんくされていない場合はリンクしてくださいという旨のメッセージを送信。
            ci.cancel();
            var message = ap.getMessage(FabricordMessageKey.Chat.LinkDiscordAccountFirst.INSTANCE);
            player.sendMessage(message);
        } else {
            String message = packet.chatMessage();

            if (CommandManager.localChatToggled.contains(playerUUID)) {
                /* Do nothing(To skip handleMCMessage) **/
            } else {
                DiscordPlayerEventHandler.INSTANCE.handleMCMessage(player, message);
            }
        }
    }
}