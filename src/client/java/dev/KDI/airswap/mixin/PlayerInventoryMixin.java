package dev.KDI.airswap.mixin;

import dev.KDI.airswap.AirSwapClient;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    @Inject(method = "setSelectedSlot(I)V", at = @At("HEAD"))
    private void airswap$manualSelection(int slot, CallbackInfo ci) {
        AirSwapClient.CONTROLLER.onSlotSelected((PlayerInventory) (Object) this, slot);
    }
}