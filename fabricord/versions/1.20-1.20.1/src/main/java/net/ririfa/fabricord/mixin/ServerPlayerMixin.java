package net.ririfa.fabricord.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.ririfa.fabricord.Fabricord;
import net.ririfa.fabricord.config.FConfig;
import net.ririfa.fabricord.config.SendableEvent;
import net.ririfa.fabricord.discord.DiscordBridge;
import net.ririfa.fabricord.discord.DiscordEmbeds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void fabricord$onDeath(DamageSource source, CallbackInfo callback) {
        FConfig config = Fabricord.Companion.getConfig();
        if (!DiscordBridge.INSTANCE.isRunning()) return;
        if (config.willSends == null || !config.willSends.contains(SendableEvent.Death)) return;
        if (config.disableDeathMessages) return;

        ServerPlayer player = (ServerPlayer) (Object) this;
        Component message = player.getCombatTracker().getDeathMessage();
        DiscordEmbeds.sendDeath(player, message);
    }
}
