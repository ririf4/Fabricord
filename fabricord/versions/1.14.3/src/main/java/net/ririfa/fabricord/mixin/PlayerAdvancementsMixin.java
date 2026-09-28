package net.ririfa.fabricord.mixin;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ririfa.fabricord.Fabricord;
import net.ririfa.fabricord.config.FConfig;
import net.ririfa.fabricord.config.SendableEvent;
import net.ririfa.fabricord.discord.DiscordBridge;
import net.ririfa.fabricord.discord.DiscordEmbeds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    private void fabricord$onAward(
        Advancement advancement,
        String criterion,
        CallbackInfoReturnable<Boolean> callback
    ) {
        FConfig config = Fabricord.Companion.getConfig();
        if (!DiscordBridge.INSTANCE.isRunning() || !callback.getReturnValue()) return;
        if (config.willSends == null || !config.willSends.contains(SendableEvent.Advancement)) return;
        if (config.disableAdvancementMessages) return;
        if (config.ignoredAdvancements != null && config.ignoredAdvancements.contains(advancement.getId().toString())) return;

        PlayerAdvancementTracker self = (PlayerAdvancementTracker) (Object) this;
        if (!self.getProgress(advancement).isDone()) return;

        AdvancementDisplay display = advancement.getDisplay();
        if (display != null && display.shouldAnnounceToChat()) {
            DiscordEmbeds.sendAdvancement(owner, display.getTitle().getString());
        }
    }
}
