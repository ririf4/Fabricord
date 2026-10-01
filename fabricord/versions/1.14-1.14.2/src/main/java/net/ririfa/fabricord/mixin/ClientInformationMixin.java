package net.ririfa.fabricord.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.ClientSettingsC2SPacket;
import net.ririfa.fabricord.i18n.PlayerLanguageCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ClientInformationMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onClientSettings", at = @At("HEAD"))
    private void fabricord$captureLanguage(ClientSettingsC2SPacket packet, CallbackInfo callback) {
        PlayerLanguageCache.set(player, ((ClientInformationPacketAccessor) packet).fabricord$getLanguage());
    }
}
