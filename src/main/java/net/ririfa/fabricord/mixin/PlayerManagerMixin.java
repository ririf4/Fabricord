package net.ririfa.fabricord.mixin;

import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.PlayerManager;
import net.ririfa.fabricord.database.DataBase;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Inject(method = "addToOperators*", at = @At("TAIL"))
    private void onAddToOperators(@NonNull PlayerConfigEntry entry, CallbackInfo ci) {
        DataBase.INSTANCE.insertPlayer(entry.comp_4422(), true);
    }

    @Inject(method = "removeFromOperators", at = @At("TAIL"))
    private void onRemoveFromOperators(@NonNull PlayerConfigEntry entry, CallbackInfo ci) {
        DataBase.INSTANCE.insertPlayer(entry.comp_4422(), false);
    }
}