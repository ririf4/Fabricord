package net.ririfa.fabricord.mixin;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.ririfa.fabricord.config.SendableEvent;
import net.ririfa.fabricord.discord.DiscordBotManager;
import net.ririfa.fabricord.discord.DiscordEmbed;
import net.ririfa.fabricord.util.Aliases;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "onDeath",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/damage/DamageTracker;getDeathMessage()Lnet/minecraft/text/Text;",
                    shift = At.Shift.AFTER
            )
    )
    public void onPlayerDeath(DamageSource source, CallbackInfo ci) {
        var config = Aliases.getConfig();
        if (!DiscordBotManager.isBotInitialized || config.logChannels == null) return;
        if (!Objects.requireNonNull(config.willSends).contains(SendableEvent.Death)) return;
        if (config.disableDeathMessages) return;

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Text message = player.getDamageTracker().getDeathMessage();

        DiscordEmbed.sendPlayerDeathEmbed(player, message);
    }
}
