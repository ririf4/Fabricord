package net.ririfa.fabricord.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.ririfa.fabricord.AliasKt;
import net.ririfa.fabricord.GroupManager;
import net.ririfa.fabricord.discord.DiscordBotManager;
import net.ririfa.fabricord.translation.FabricordMessageKey;
import net.ririfa.fabricord.translation.FabricordMessageProviderKt;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.UUID;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void interceptChatMessage(@NotNull ChatMessageC2SPacket packet, CallbackInfo ci) {
        if (!DiscordBotManager.botIsInitialized || AliasKt.getConfig().logChannelIDIsNotSet || Boolean.TRUE.equals(AliasKt.getConfig().dontSendChatToDiscord))
            return;

        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.player;
        UUID playerUUID = player.getUuid();
        String message = packet.chatMessage();

        if (GroupManager.playerInGroupedChat.containsKey(playerUUID)) {
            ci.cancel();

            var groupID = GroupManager.playerInGroupedChat.get(playerUUID);
            var group = GroupManager.getGroupById(groupID);

            if (group == null) return;

            var groupMembers = group.members;
            var ap = FabricordMessageProviderKt.adapt(player);

            Text formattedMessage = ap.getMessage(
                    FabricordMessageKey.System.GRP.GroupedChatMessageBase.INSTANCE, group.name, player.getDisplayName(), message
            );

            groupMembers.forEach(memberUUID -> {
                ServerPlayerEntity member = Objects.requireNonNull(player.getServer()).getPlayerManager().getPlayer(memberUUID);
                if (member != null) {
                    member.sendMessage(formattedMessage);
                }
            });
        }
    }
}