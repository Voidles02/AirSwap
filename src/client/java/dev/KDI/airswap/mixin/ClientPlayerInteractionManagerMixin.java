package dev.KDI.airswap.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.KDI.airswap.AirSwapClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @WrapMethod(method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V")
    private void airswap$attack(PlayerEntity player, Entity target, Operation<Void> original) {
        try {
            AirSwapClient.CONTROLLER.beforeAttack(player, target);
        } catch (RuntimeException exception) {
            AirSwapClient.LOGGER.warn("Air Swap could not prepare the slot swap; allowing the attack to continue", exception);
        }
        try {
            original.call(player, target);
        } finally {
            try {
                AirSwapClient.CONTROLLER.afterAttack();
            } catch (RuntimeException exception) {
                AirSwapClient.LOGGER.warn("Air Swap could not finish restoring the selected slot", exception);
            }
        }
    }
}