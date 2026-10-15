package dev.KDI.airswap;

import dev.KDI.airswap.config.AirSwapConfig;
import dev.KDI.airswap.mixin.SelectedSlotSyncInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
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
                || player.fallDistance < config.minimumFallDistance
                || (config.onlyWhileFalling && player.getVelocity().y >= 0.0)
                || (config.requireAttackCooldown && player.getAttackCooldownProgress(0.0f) < 0.9f)
                || (config.requireLineOfSight && !player.canSee(target))) {
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
        int firstMaceSlot = -1;
        int breachMaceSlot = -1;
        int mostDurableMaceSlot = -1;
        int mostDurableBreachMaceSlot = -1;
        int mostDurableMace = Integer.MIN_VALUE;
        int mostDurableBreachMace = Integer.MIN_VALUE;
        var breach = client.world.getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.BREACH);
        int searchLimit = config.onlyHotbar ? PlayerInventory.HOTBAR_SIZE : PlayerInventory.MAIN_SIZE;
        for (int slot = 0; slot < searchLimit; slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isOf(Items.MACE) || (config.avoidNearlyBrokenMaces && isNearlyBroken(stack))) {
                continue;
            }
            if (firstMaceSlot < 0) {
                firstMaceSlot = slot;
            }
            int durability = remainingDurability(stack);
            if (durability > mostDurableMace) {
                mostDurableMace = durability;
                mostDurableMaceSlot = slot;
            }
            if (EnchantmentHelper.getLevel(breach, stack) > 0) {
                if (breachMaceSlot < 0) {
                    breachMaceSlot = slot;
                }
                if (durability > mostDurableBreachMace) {
                    mostDurableBreachMace = durability;
                    mostDurableBreachMaceSlot = slot;
                }
            }
        }
        int fallbackMaceSlot = config.preferMostDurableMace ? mostDurableMaceSlot : firstMaceSlot;
        int selectedBreachMaceSlot = config.preferMostDurableMace
                ? mostDurableBreachMaceSlot : breachMaceSlot;
        int maceSourceSlot = config.requireBreachMace
                ? selectedBreachMaceSlot
                : config.preferBreachMace && selectedBreachMaceSlot >= 0
                ? selectedBreachMaceSlot : fallbackMaceSlot;
        if (maceSourceSlot < 0) {
            return false;
        }
        int inventorySlot = maceSourceSlot >= PlayerInventory.HOTBAR_SIZE ? maceSourceSlot : -1;
        int maceSlot = maceSourceSlot;
        ItemStack storedSword = weapon.copy();
        PendingSwap swap = new PendingSwap(player, swordSlot,
                inventorySlot >= 0 ? swordSlot : maceSlot, inventorySlot,
                storedSword, tick + config.swapBackDelay);
        if (config.swapBack) {
            pending = swap;
        }
        if (inventorySlot >= 0) {
            client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                    inventorySlot, swordSlot, SlotActionType.SWAP, player);
            maceSlot = swordSlot;
        }
        if (maceSlot < 0 || !inventory.getStack(maceSlot).isOf(Items.MACE)) {
            if (inventorySlot >= 0 && matchesOriginalWeapon(inventory.getStack(inventorySlot), storedSword)) {
                client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                        inventorySlot, swordSlot, SlotActionType.SWAP, player);
            }
            pending = null;
            return false;
        }
        inventory.setSelectedSlot(maceSlot);
        nextSwapTick = tick + config.cooldownTicks;
        if (config.showSwapMessage) {
            player.sendMessage(Text.translatable("message.airswap.swapped", maceSlot + 1), true);
        }
        return true;
    }

    public void afterAttack() {
        if (pending != null && tick >= pending.restoreTick) {
            restoreNow(MinecraftClient.getInstance());
        }
    }

    public void startTick(MinecraftClient client) {
        tick++;
        if (pending != null && (client.player != pending.player
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
        if (swap == null) {
            return;
        }
        if (client.player != swap.player || client.interactionManager == null || client.world == null) {
            pending = null;
            return;
        }
        ClientPlayerEntity player = client.player;
        if (client.currentScreen != null || player.currentScreenHandler != player.playerScreenHandler) {
            return;
        }
        PlayerInventory inventory = player.getInventory();
        if (inventory.getSelectedSlot() != swap.maceSlot
                || !inventory.getStack(swap.maceSlot).isOf(Items.MACE)) {
            pending = null;
            return;
        }
        if (swap.inventorySlot >= 0) {
            if (!matchesOriginalWeapon(inventory.getStack(swap.inventorySlot), swap.sword)) {
                pending = null;
                return;
            }
            client.interactionManager.clickSlot(player.playerScreenHandler.syncId,
                    swap.inventorySlot, swap.swordSlot, SlotActionType.SWAP, player);
            if (!matchesOriginalWeapon(inventory.getStack(swap.swordSlot), swap.sword)
                    || !inventory.getStack(swap.inventorySlot).isOf(Items.MACE)) {
                return;
            }
        } else if (matchesOriginalWeapon(inventory.getStack(swap.swordSlot), swap.sword)) {
            inventory.setSelectedSlot(swap.swordSlot);
            ((SelectedSlotSyncInvoker) client.interactionManager).airswap$syncSelectedSlot();
        }
        pending = null;
    }

    public void reset() {
        pending = null;
        tick = 0;
        nextSwapTick = 0;
    }

    private static boolean matchesOriginalWeapon(ItemStack candidate, ItemStack original) {
        if (candidate.isEmpty() || original.isEmpty()) {
            return false;
        }
        ItemStack candidateWithoutDamage = candidate.copy();
        ItemStack originalWithoutDamage = original.copy();
        candidateWithoutDamage.remove(DataComponentTypes.DAMAGE);
        originalWithoutDamage.remove(DataComponentTypes.DAMAGE);
        return ItemStack.areEqual(candidateWithoutDamage, originalWithoutDamage);
    }

    private static boolean isNearlyBroken(ItemStack stack) {
        if (!stack.isDamageable()) {
            return false;
        }
        int maxDamage = stack.getMaxDamage();
        return maxDamage - stack.getDamage() <= Math.max(1, maxDamage / 20);
    }

    private static int remainingDurability(ItemStack stack) {
        return stack.isDamageable() ? stack.getMaxDamage() - stack.getDamage() : Integer.MAX_VALUE;
    }

    private record PendingSwap(ClientPlayerEntity player, int swordSlot, int maceSlot,
                               int inventorySlot, ItemStack sword, long restoreTick) {
    }
}