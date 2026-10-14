package dev.KDI.airswap.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface SelectedSlotSyncInvoker {
    @Invoker("syncSelectedSlot")
    void airswap$syncSelectedSlot();
}