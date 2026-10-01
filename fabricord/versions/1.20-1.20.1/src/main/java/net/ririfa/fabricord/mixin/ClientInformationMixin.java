package net.ririfa.fabricord.mixin;

import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.ririfa.fabricord.i18n.PlayerLanguageCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ClientInformationMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleClientInformation", at = @At("HEAD"))
    private void fabricord$captureLanguage(ServerboundClientInformationPacket packet, CallbackInfo callback) {
        PlayerLanguageCache.set(player, packet.language());
    }
}
