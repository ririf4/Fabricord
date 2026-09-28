package net.ririfa.fabricord.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.ririfa.fabricord.Fabricord;
import net.ririfa.fabricord.config.FConfig;
import net.ririfa.fabricord.config.SendableEvent;
import net.ririfa.fabricord.discord.DiscordBridge;
import net.ririfa.fabricord.discord.DiscordEmbeds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void fabricord$onDeath(DamageSource source, CallbackInfo callback) {
        FConfig config = Fabricord.Companion.getConfig();
        if (!DiscordBridge.INSTANCE.isRunning()) return;
        if (config.willSends == null || !config.willSends.contains(SendableEvent.Death)) return;
        if (config.disableDeathMessages) return;

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Text message = player.getDamageTracker().getDeathMessage();
        DiscordEmbeds.sendDeath(player, message);
    }
}
