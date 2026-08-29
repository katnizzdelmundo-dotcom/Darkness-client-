package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

public class AutoCrystalModule extends Module {
    private final Setting<Double> rangeSetting = new Setting<>("Range", 4.5);
    private final Setting<Boolean> optimizedCrystal = new Setting<>("OptimizedCrystal", true);
    
    // Auto Obsidian & Swap-Back Settings
    private final Setting<Boolean> autoObsidianOnKey = new Setting<>("AutoObsidianM2", true);
    private final Setting<Integer> triggerKey = new Setting<>("TriggerButton", GLFW.GLFW_MOUSE_BUTTON_2);
    private final Setting<Boolean> swapBack = new Setting<>("SwapBack", true); // Returns to your original item after placing obsidian
    private final Setting<Integer> swapBackDelay = new Setting<>("SwapBackDelayMs", 60);

    // Anti-Cheat Randomization
    private final Setting<Integer> minDelay = new Setting<>("MinDelayMs", 40);
    private final Setting<Integer> maxDelay = new Setting<>("MaxDelayMs", 100);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 93);

    private long nextActionTime = 0;
    private long swapBackTime = 0;
    private int originalSlot = -1;
    private boolean hasSwapped = false;
    private boolean pendingSwapBack = false;

    public AutoCrystalModule() {
        super("Auto Crystal", Category.COMBAT);
        addSetting(rangeSetting);
        addSetting(optimizedCrystal);
        addSetting(autoObsidianOnKey);
        addSetting(triggerKey);
        addSetting(swapBack);
        addSetting(swapBackDelay);
        addSetting(minDelay);
        addSetting(maxDelay);
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

        // Handle delayed swap back to original item after placing obsidian
        if (pendingSwapBack) {
            if (currentTime >= swapBackTime) {
                resetSlot(client);
                pendingSwapBack = false;
            }
            return;
        }

        // Handle Auto-Obsidian placement when M2 is held
        if (autoObsidianOnKey.getValue() && isTriggerPressed(client)) {
            handleAutoObsidian(client, currentTime);
        }

        if (currentTime < nextActionTime) return;
        if (Math.random() * 100.0 > successChance.getValue()) return;

        LivingEntity target = findBestTarget(client);
        if (target == null) return;

        if (optimizedCrystal.getValue()) {
            executeOptimizedCrystalPvP(client, target);
        } else {
            executeStandardCrystalPvP(client, target);
        }

        long randomDelay = minDelay.getValue() + (long)(Math.random() * (maxDelay.getValue() - minDelay.getValue()));
        nextActionTime = currentTime + randomDelay;
    }

    private boolean isTriggerPressed(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        return GLFW.glfwGetMouseButton(window, triggerKey.getValue()) == GLFW.GLFW_PRESS;
    }

    private void handleAutoObsidian(MinecraftClient client, long currentTime) {
        if (client.crosshairTarget instanceof BlockHitResult hit && hit.getType() == BlockHitResult.Type.BLOCK) {
            BlockPos placePos = hit.getBlockPos().offset(hit.getSide());
            
            if (!isHoldingObsidian(client)) {
                int obsSlot = findObsidianHotbar(client);
                if (obsSlot != -1) {
                    if (!hasSwapped) {
                        originalSlot = client.player.getInventory().selectedSlot;
                        hasSwapped = true;
                    }
                    client.player.getInventory().selectedSlot = obsSlot;
                }
            }

            if (isHoldingObsidian(client)) {
                client.interactionManager.interactBlock(client.player, net.minecraft.util.Hand.MAIN_HAND, new BlockHitResult(
                    new Vec3d(placePos.getX() + 0.5, placePos.getY(), placePos.getZ() + 0.5),
                    hit.getSide(), placePos, false
                ));
                client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

                if (swapBack.getValue() && hasSwapped) {
                    swapBackTime = currentTime + swapBackDelay.getValue();
                    pendingSwapBack = true;
                }
            }
        }
    }

    private void executeOptimizedCrystalPvP(MinecraftClient client, LivingEntity target) {
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            ensureItemInHand(client, Items.END_CRYSTAL);
            if (client.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
                client.interactionManager.interactBlock(client.player, net.minecraft.util.Hand.MAIN_HAND, hit);
                client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
        }
    }

    private void executeStandardCrystalPvP(MinecraftClient client, LivingEntity target) {
        if (client.crosshairTarget instanceof BlockHitResult hit) {
            client.interactionManager.interactBlock(client.player, net.minecraft.util.Hand.MAIN_HAND, hit);
            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
    }

    private boolean isHoldingObsidian(MinecraftClient client) {
        return client.player.getMainHandStack().isOf(Items.OBSIDIAN);
    }

    private int findObsidianHotbar(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.OBSIDIAN)) return i;
        }
        return -1;
    }

    private void ensureItemInHand(MinecraftClient client, net.minecraft.item.Item item) {
        if (!client.player.getMainHandStack().isOf(item)) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = client.player.getInventory().getStack(i);
                if (stack.isOf(item)) {
                    client.player.getInventory().selectedSlot = i;
                    break;
                }
            }
        }
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
