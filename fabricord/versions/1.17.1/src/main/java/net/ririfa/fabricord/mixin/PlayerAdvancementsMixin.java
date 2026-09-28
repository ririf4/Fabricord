package net.ririfa.fabricord.mixin;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
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

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
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

        PlayerAdvancements self = (PlayerAdvancements) (Object) this;
        if (!self.getOrStartProgress(advancement).isDone()) return;

        DisplayInfo display = advancement.getDisplay();
        if (display != null && display.shouldAnnounceChat()) {
            DiscordEmbeds.sendAdvancement(player, display.getTitle().getString());
        }
    }
}
