package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class HoverTotemModule extends Module {
    // Prestige-Class Anti-Cheat & Health Trigger Settings
    private final Setting<Double> healthThreshold = new Setting<>("HealthThreshold", 12.0); // Triggers when health drops to 6 hearts or below
    private final Setting<Boolean> alwaysTotem = new Setting<>("AlwaysTotem", false); // If true, keeps offhand filled at all times regardless of health
    
    // Anti-Cheat Safe Timing & Reaction Delays
    private final Setting<Integer> minSwapDelay = new Setting<>("MinSwapDelayMs", 40);
    private final Setting<Integer> maxSwapDelay = new Setting<>("MaxSwapDelayMs", 110);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 95); // Anti-cheat randomization percentage

    private long nextSwapTime = 0;
    private boolean pendingSwap = false;
    private int targetTotemSlot = -1;

    public HoverTotemModule() {
        super("Hover Totem", Category.COMBAT);
        addSetting(healthThreshold);
        addSetting(alwaysTotem);
        addSetting(minSwapDelay);
        addSetting(maxSwapDelay);
        addSetting(successChance);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.world == null) return;

        // Check if offhand already holds a Totem of Undying
        boolean hasTotemInOffhand = client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);

        // Determine if totem equipping is required based on health or mode settings
        boolean shouldEquip = alwaysTotem.getValue() || client.player.getHealth() <= healthThreshold.getValue();

        if (hasTotemInOffhand || !shouldEquip) {
            pendingSwap = false;
            return;
        }

        long currentTime = System.currentTimeMillis();

        // If a swap is already queued and waiting for its humanized reaction delay
        if (pendingSwap) {
            if (currentTime >= nextSwapTime && targetTotemSlot != -1) {
                executeSafeTotemSwap(client, targetTotemSlot);
                pendingSwap = false;
                targetTotemSlot = -1;
            }
            return;
        }

        if (currentTime < nextSwapTime) return;

        // Find a Totem of Undying anywhere in the player's inventory
        int totemSlot = findTotemSlot(client);
        if (totemSlot == -1) return; // No totems left in inventory

        // Anti-cheat stochastic randomization check (skips ~5% of triggers to prevent macro pattern detection)
        if (Math.random() * 100.0 > successChance.getValue()) {
            return;
        }

        // Queue the swap with a randomized millisecond reaction delay
        targetTotemSlot = totemSlot;
        long randomDelay = minSwapDelay.getValue() + (long)(Math.random() * (maxSwapDelay.getValue() - minSwapDelay.getValue()));
        nextSwapTime = currentTime + randomDelay;
        pendingSwap = true;
    }

    private int findTotemSlot(MinecraftClient client) {
        // Search through main inventory and hotbar (Slots 9 to 44 in container view)
        for (int i = 9; i < 45; i++) {
            ItemStack stack = client.player.playerScreenHandler.getSlot(i).getStack();
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }

    private void executeSafeTotemSwap(MinecraftClient client, int slot) {
        // Slot 45 represents the offhand slot in Minecraft's default player inventory container
        int offhandSlot = 45;

        // Pick up the totem from its inventory slot
        client.interactionManager.clickSlot(
            client.player.playerScreenHandler.syncId,
            slot,
            0,
            SlotActionType.PICKUP,
            client.player
        );

        // Place it into the offhand slot
        client.interactionManager.clickSlot(
            client.player.playerScreenHandler.syncId,
            offhandSlot,
            0,
            SlotActionType.PICKUP,
            client.player
        );

        // Return any item previously in the offhand back to the inventory grid
        client.interactionManager.clickSlot(
            client.player.playerScreenHandler.syncId,
            slot,
            0,
            SlotActionType.PICKUP,
            client.player
        );
    }
}
