package net.ririfa.fabricord.mixin;

import net.minecraft.network.packet.c2s.play.ClientSettingsC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ririfa.fabricord.FabricordLanguageCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onClientSettings", at = @At("HEAD"))
    private void onClientSettings(ClientSettingsC2SPacket packet, CallbackInfo ci) {
        FabricordLanguageCache.set(this.player, packet.language());
    }

//    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
//    private void interceptChatMessage(@NotNull ChatMessageC2SPacket packet, CallbackInfo ci) {
//        if (!DiscordBotManager.botIsInitialized || AliasKt.getConfig().logChannelIDIsNotSet || Boolean.TRUE.equals(AliasKt.getConfig().dontSendChatToDiscord))
//            return;
//
//        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
//        ServerPlayerEntity player = handler.player;
//        UUID playerUUID = player.getUuid();
//        String message = packet.chatMessage();
//
//        //TODO: Implement group chat
//        if (GroupManager.playerInGroupedChat.containsKey(playerUUID)) {
//            ci.cancel();
//
//            var groupID = GroupManager.playerInGroupedChat.get(playerUUID);
//            var group = GroupManager.getGroupById(groupID);
//
//            var groupMembers = Objects.requireNonNull(group).getMembers();
//            var ap = FabricordMessageProviderKt.adapt(player);
//
//            Text formattedMessage = ap.getMessage(FabricordMessageKey.System.GRP.GroupedChatMessageBase.INSTANCE, group.getName(), player.getDisplayName(),
//            message);
//
//            groupMembers.forEach(memberUUID -> {
//                ServerPlayerEntity member = Objects.requireNonNull(player.getServer()).getPlayerManager().getPlayer(memberUUID);
//                if (member != null) {
//                    member.sendMessage(formattedMessage);
//                }
//            });
//        }
//    }
}