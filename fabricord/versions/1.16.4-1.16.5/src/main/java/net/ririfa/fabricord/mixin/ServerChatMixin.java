package net.ririfa.fabricord.mixin;

import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.ririfa.fabricord.Fabricord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerChatMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat*", at = @At("HEAD"), cancellable = true)
    private void fabricord$onChat(ServerboundChatPacket packet, CallbackInfo callback) {
        if (!Fabricord.Companion.handleChat(player, packet.getMessage())) {
            callback.cancel();
        }
    }
}
