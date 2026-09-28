package net.ririfa.fabricord.mixin;

import net.minecraft.server.network.packet.ClientSettingsC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientSettingsC2SPacket.class)
public interface ClientInformationPacketAccessor {
    @Accessor("language")
    String fabricord$getLanguage();
}
