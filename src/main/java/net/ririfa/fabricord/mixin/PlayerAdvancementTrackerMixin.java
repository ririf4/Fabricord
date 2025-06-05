package net.ririfa.fabricord.mixin;

import net.minecraft.advancement.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.ririfa.fabricord.Aliases;
import net.ririfa.fabricord.discord.DiscordBotManager;
import net.ririfa.fabricord.discord.DiscordEmbed;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {
    @Shadow
    private ServerPlayerEntity owner;

    @Inject(method = "grantCriterion", at = @At("RETURN"))
    public void onAdvancementGranted(AdvancementEntry advancementEntry, String string, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (!DiscordBotManager.isBotInitialized || Aliases.getConfig().isLogChannelIDNotSet) return;

        if (cir.getReturnValue()) {
            Advancement advancement = advancementEntry.value();
            Optional<AdvancementDisplay> display = advancement.comp_1913();

            if (display.isPresent() && display.get().shouldAnnounceToChat()) {
                AdvancementProgress progress = ((PlayerAdvancementTracker) (Object) this).getProgress(advancementEntry);
                if (progress.isDone()) {
                    String title = display.get().getTitle().getString();
                    DiscordEmbed.sendPlayerGrantCriterionEmbed(owner, title);
                }
            }
        }
    }
}