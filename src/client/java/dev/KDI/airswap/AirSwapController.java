package dev.KDI.airswap;

import dev.KDI.airswap.config.AirSwapConfig;
import dev.KDI.airswap.mixin.SelectedSlotSyncInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public final class AirSwapController {
    private long tick;
    private long nextSwapTick;
    private PendingSwap pending;

    public boolean beforeAttack(PlayerEntity attacker, Entity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        AirSwapConfig config = AirSwapClient.config();
        if (player == null || attacker != player || client.interactionManager == null
                || client.world == null || client.currentScreen != null || !config.enabled
                || pending != null || tick < nextSwapTick || player.currentScreenHandler != player.playerScreenHandler
                || (config.playersOnly && !(target instanceof PlayerEntity))
                || player.isOnGround() || player.isTouchingWater() || player.isInLava()
                || player.isClimbing() || (player.isGliding() && !config.allowWhileGliding)
                || (config.disableWhenSneaking && player.isSneaking())
                || player.fallDistance < config.minimumFallDistance) {
            return false;
        }
        ItemStack weapon = player.getMainHandStack();
        boolean isSword = weapon.isIn(ItemTags.SWORDS);
        boolean isSpear = weapon.isIn(ItemTags.SPEARS);
        if ((!isSword && !(config.allowSpears && isSpear))
                || (isSword && !config.allSwords
                && !config.allowedSwords.contains(Registries.ITEM.getId(weapon.getItem()).toString()))) {
            return false;
        }
        PlayerInventory inventory = player.getInventory();
        int swordSlot = inventory.getSelectedSlot();
        int maceSlot = -1;
        for (int slot = 0; slot < PlayerInventory.HOTBAR_SIZE; slot++) {
            if (inventory.getStack(slot).isOf(Items.MACE)) {
                maceSlot = slot;
                break;
            }
        }
        int inventorySlot = -1;
        ItemStack storedSword = weapon.copy();
        if (maceSlot < 0 && !config.onlyHotbar) {
            for (int slot = PlayerInventory.HOTBAR_SIZE; slot < PlayerInventory.MAIN_SIZE; slot++) {
                if (inventory.getStack(slot).isOf(Items.MACE)) {
                    inventorySlot = slot;
                    break;
                }
            }
            if (inventorySlot >= 0) {
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                        inventorySlot, swordSlot, SlotActionType.SWAP, player);
                maceSlot = swordSlot;
            }
        }
        if (maceSlot < 0 || !inventory.getStack(maceSlot).isOf(Items.MACE)) {
            return false;
        }
        inventory.setSelectedSlot(maceSlot);
        nextSwapTick = tick + config.cooldownTicks;
        if (config.swapBack) {
            pending = new PendingSwap(player, swordSlot, maceSlot, inventorySlot,
                    storedSword, tick + config.swapBackDelay);
        }
        if (config.showSwapMessage) {
            player.sendMessage(Text.translatable("message.airswap.swapped", maceSlot + 1), true);
        }
        return true;
    }

    public void afterAttack(boolean swapped) {
        if (swapped && pending != null && tick >= pending.restoreTick) {
            restoreNow(MinecraftClient.getInstance());
        }
    }

    public void startTick(MinecraftClient client) {
        tick++;
        if (pending != null && (client.player != pending.player
                || client.currentScreen != null
                || client.player.getInventory().getSelectedSlot() != pending.maceSlot)) {
            pending = null;
        }
    }

    public void endTick(MinecraftClient client) {
        if (pending != null && (tick >= pending.restoreTick || !AirSwapClient.config().enabled)) {
            restoreNow(client);
        }
    }

    public void onSlotSelected(PlayerInventory inventory, int newSlot) {
        PendingSwap swap = pending;
        if (swap != null && inventory == swap.player.getInventory() && newSlot != swap.maceSlot) {
            pending = null;
        }
    }

    public void restoreNow(MinecraftClient client) {
        PendingSwap swap = pending;
        pending = null;
        if (swap == null || client.player != swap.player || client.interactionManager == null
                || client.world == null || client.currentScreen != null) {
            return;
        }
        ClientPlayerEntity player = client.player;
        PlayerInventory inventory = player.getInventory();
        if (inventory.getSelectedSlot() != swap.maceSlot
                || !inventory.getStack(swap.maceSlot).isOf(Items.MACE)) {
            return;
        }
        if (swap.inventorySlot >= 0) {
            if (player.currentScreenHandler != player.playerScreenHandler
                    || !ItemStack.areEqual(inventory.getStack(swap.inventorySlot), swap.sword)) {
                return;
            }
            client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                    swap.inventorySlot, swap.swordSlot, SlotActionType.SWAP, player);
        } else if (ItemStack.areEqual(inventory.getStack(swap.swordSlot), swap.sword)) {
            inventory.setSelectedSlot(swap.swordSlot);
            ((SelectedSlotSyncInvoker) client.interactionManager).airswap$syncSelectedSlot();
        }
    }

    public void reset() {
        pending = null;
        tick = 0;
        nextSwapTick = 0;
    }

    private record PendingSwap(ClientPlayerEntity player, int swordSlot, int maceSlot,
                               int inventorySlot, ItemStack sword, long restoreTick) {
    }
}