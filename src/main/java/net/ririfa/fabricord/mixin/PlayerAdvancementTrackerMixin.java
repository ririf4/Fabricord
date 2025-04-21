package net.ririfa.fabricord.mixin;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
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
    public void onAdvancementGranted(AdvancementEntry advancementEntry, String string, @NotNull CallbackInfoReturnable<Boolean> cir) {
//        if (!DiscordBotManager.botIsInitialized || AliasKt.getConfig().logChannelIDIsNotSet) return;
//
//        if (cir.getReturnValue()) {
//            Advancement advancement = advancementEntry.value();
//            Optional<AdvancementDisplay> display = advancement.comp_1913();
//
//            if (display.isPresent() && display.get().shouldAnnounceToChat()) {
//                AdvancementProgress progress = ((PlayerAdvancementTracker) (Object) this).getProgress(advancementEntry);
//                if (progress.isDone()) {
//                    String title = display.get().getTitle().getString();
//                    DiscordEmbed.sendPlayerGrantCriterionEmbed(owner, title);
//                }
//            }
//        }
    }
}