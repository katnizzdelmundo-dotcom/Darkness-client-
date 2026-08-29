
package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Comparator;

public class AutoAnchorModule extends Module {
    // Prestige-Class Range & Anti-Cheat Timing Settings
    private final Setting<Double> rangeSetting = new Setting<>("Range", 4.5);
    private final Setting<Integer> minDelay = new Setting<>("MinDelayMs", 70);
    private final Setting<Integer> maxDelay = new Setting<>("MaxDelayMs", 150);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 92); // Anti-cheat randomization
    
    // Safety & Self-Harm Prevention
    private final Setting<Boolean> selfHarmSafety = new Setting<>("SelfHarmSafety", true);
    private final Setting<Double> minSelfDistance = new Setting<>("MinSelfDistance", 2.8); // Pauses if you are too close to the blast zone

    // Hotbar Management & Seamless Swap-Back
    private final Setting<Boolean> autoSwap = new Setting<>("AutoSwap", true);
    private final Setting<Boolean> swapBack = new Setting<>("SwapBack", true); // Returns to your original item automatically
    private final Setting<Integer> swapBackDelay = new Setting<>("SwapBackDelayMs", 65); // Millisecond delay before swapping back

    private long nextActionTime = 0;
    private long swapBackTime = 0;
    private int originalSlot = -1;
    private boolean hasSwapped = false;
    private boolean pendingSwapBack = false;

    public AutoAnchorModule() {
        super("Auto Anchor", Category.COMBAT);
        addSetting(rangeSetting);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(successChance);
        addSetting(selfHarmSafety);
        addSetting(minSelfDistance);
        addSetting(autoSwap);
        addSetting(swapBack);
        addSetting(swapBackDelay);
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

        // Handle smooth delayed swap back to your original item (totem, sword, etc.)
        if (pendingSwapBack) {
            if (currentTime >= swapBackTime) {
                resetSlot(client);
                pendingSwapBack = false;
            }
            return;
        }

        LivingEntity target = findBestTarget(client);
        if (target == null) return;

        // Self-Harm Safety Check: Pauses execution if you are too close to prevent blowing yourself up
        if (selfHarmSafety.getValue() && client.player.distanceTo(target) < minSelfDistance.getValue()) {
            return;
        }

        if (currentTime < nextActionTime) return;

        // Anti-cheat stochastic randomization check (skips ~8% of ticks to look organic)
        if (Math.random() * 100.0 > successChance.getValue()) {
            return;
        }

        // Handle Hotbar management for anchors / glowstone
        if (autoSwap.getValue()) {
            ensureCombatItems(client);
        }

        // Execute block interaction under crosshair context
        if (client.crosshairTarget instanceof BlockHitResult blockHit) {
            client.interactionManager.interactBlock(client.player, net.minecraft.util.Hand.MAIN_HAND, blockHit);
            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

            // Schedule the return swap back to your preferred item
            if (swapBack.getValue() && hasSwapped) {
                swapBackTime = currentTime + swapBackDelay.getValue();
                pendingSwapBack = true;
            }

            // Fluctuate millisecond intervals organically
            long randomDelay = minDelay.getValue() + (long)(Math.random() * (maxDelay.getValue() - minDelay.getValue()));
            nextActionTime = currentTime + randomDelay;
        }
    }

    private void ensureCombatItems(MinecraftClient client) {
        boolean hasAnchorOrGlow = client.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR) 
                || client.player.getMainHandStack().isOf(Items.GLOWSTONE);
        
        if (!hasAnchorOrGlow) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = client.player.getInventory().getStack(i);
                if (stack.isOf(Items.RESPAWN_ANCHOR) || stack.isOf(Items.GLOWSTONE)) {
                    if (!hasSwapped) {
                        originalSlot = client.player.getInventory().selectedSlot;
                        hasSwapped = true;
                    }
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
