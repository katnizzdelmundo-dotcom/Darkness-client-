package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

public class AutoSwordModule extends Module {
    // Prestige-Class Anti-Cheat Safe & Swap Settings
    private final Setting<Double> rangeSetting = new Setting<>("Range", 4.2);
    private final Setting<Boolean> swapBack = new Setting<>("SwapBack", true); // Returns to your previous item automatically
    private final Setting<Integer> swapBackDelay = new Setting<>("SwapBackDelayMs", 65); // Millisecond delay before swapping back
    private final Setting<Boolean> syncCooldown = new Setting<>("SyncCooldown", true); // Waits for sword attack recharge bar
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 95); // Anti-cheat randomization percentage

    private int originalSlot = -1;
    private boolean hasSwapped = false;
    private long swapBackTime = 0;
    private boolean pendingSwapBack = false;

    public AutoSwordModule() {
        super("Auto Sword", Category.COMBAT);
        addSetting(rangeSetting);
        addSetting(swapBack);
        addSetting(swapBackDelay);
        addSetting(syncCooldown);
        addSetting(successChance);
    }

    @Override
    public void onDisable() {
        resetSlot(MinecraftClient.getInstance());
        pendingSwapBack = false;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.world == null) return;

        long currentTime = System.currentTimeMillis();

        // Handle delayed swap back to your original item (crystals, obsidian, etc.)
        if (pendingSwapBack) {
            if (currentTime >= swapBackTime) {
                resetSlot(client);
                pendingSwapBack = false;
            }
            return;
        }

        // Check if Left Click (M1) is currently being pressed down
        boolean isLeftClickPressed = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        if (!isLeftClickPressed) return;

        // If you are already holding a sword, let vanilla gameplay handle attacks naturally
        if (isHoldingSword(client)) return;

        // Identify target under crosshair or within range
        LivingEntity target = null;
        if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
            if (client.player.distanceTo(living) <= rangeSetting.getValue() && living.isAlive()) {
                target = living;
            }
        }

        if (target == null) {
            target = findBestTarget(client);
        }

        if (target == null) return;

        // Attack Cooldown Sync: Prevents rapid-click speed exploit flags on Grim/Vulcan
        if (syncCooldown.getValue() && client.player.getAttackCooldownProgress(0.5f) < 0.9f) {
            return;
        }

        // Anti-cheat stochastic randomization check (skips ~5% of triggers to prevent macro pattern detection)
        if (Math.random() * 100.0 > successChance.getValue()) {
            return;
        }

        int swordSlot = findSwordHotbar(client);
        if (swordSlot == -1) return; // No sword found in hotbar

        // Save your current utility slot, switch to sword, and strike
        originalSlot = client.player.getInventory().selectedSlot;
        hasSwapped = true;
        client.player.getInventory().selectedSlot = swordSlot;

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        // Schedule the smooth return swap back to your utility item
        if (swapBack.getValue()) {
            swapBackTime = currentTime + swapBackDelay.getValue();
            pendingSwapBack = true;
        }
    }

    private boolean isHoldingSword(MinecraftClient client) {
        return client.player.getMainHandStack().getItem() instanceof SwordItem;
    }

    private int findSwordHotbar(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof SwordItem) {
                return i;
            }
        }
        return -1;
    }

    private void resetSlot(MinecraftClient client) {
        if (hasSwapped && originalSlot != -1 && client.player != null) {
            client.player.getInventory().selectedSlot = originalSlot;
            originalSlot = -1;
            hasSwapped = false;
        }
    }

    private LivingEntity findBestTarget(MinecraftClient client) {
        return client.world.getEntitiesByClass(
            LivingEntity.class,
            client.player.getBoundingBox().expand(rangeSetting.getValue()),
            entity -> entity != client.player && entity.isAlive() && client.player.distanceTo(entity) <= rangeSetting.getValue()
        )
        .stream()
        .min(Comparator.comparingDouble(client.player::distanceTo))
        .orElse(null);
    }
}
