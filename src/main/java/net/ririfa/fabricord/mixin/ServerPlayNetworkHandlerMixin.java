package net.ririfa.fabricord.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void interceptChatMessage(@NotNull ChatMessageC2SPacket packet, CallbackInfo ci) {
//        if (!DiscordBotManager.isBotInitialized ||
//                Aliases.getConfig().isLogChannelIDNotSet ||
//                !Aliases.getConfig().sendChat()) return;
//
//        UUID playerUUID = player.getUuid();
//        FabricordMessageProvider ap = FabricordMessageProviderKt.adapt(player);
//
//        if (!DiscordMinecraftLink.isUserLinked(playerUUID) && Aliases.getConfig().useUserPermissionForMention) {
//            player.sendMessage(ap.getMessage(FabricordMessageKey.Chat.LinkDiscordAccountFirst.INSTANCE));
//            ci.cancel();
//            return;
//        }
//
//        String message = packet.chatMessage();
//
//        if (!CommandManager.localChatToggled.contains(playerUUID)) {
//            DiscordPlayerEventHandler.INSTANCE.handleMCMessage(player, message);
//        }
    }
}