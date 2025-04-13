package net.ririfa.fabricord.mixin;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ririfa.fabricord.AliasKt;
import net.ririfa.fabricord.discord.DiscordBotManager;
import net.ririfa.fabricord.discord.DiscordEmbed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    public void onAdvancementGranted(Advancement advancement, String string, CallbackInfoReturnable<Boolean> cir) {
        if (!DiscordBotManager.botIsInitialized || AliasKt.getConfig().logChannelIDIsNotSet) return;

        if (cir.getReturnValue()) {
            AdvancementDisplay display = advancement.getDisplay();
            if (display == null || !display.shouldAnnounceToChat()) return;

            AdvancementProgress progress = ((PlayerAdvancementTracker) (Object) this).getProgress(advancement);
            if (progress.isDone()) {
                String title = display.getTitle().getString();
                DiscordEmbed.sendPlayerGrantCriterionEmbed(owner, title);
            }
        }
    }
}