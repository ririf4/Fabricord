package net.ririfa.fabricord.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.ririfa.fabricord.Fabricord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void fabricord$onJoin(Connection connection, ServerPlayer player, CallbackInfo callback) {
        Fabricord.Companion.handleJoin(player);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void fabricord$onDisconnect(ServerPlayer player, CallbackInfo callback) {
        Fabricord.Companion.handleDisconnect(player);
    }
}
