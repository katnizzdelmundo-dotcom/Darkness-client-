package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

public class FastExpModule extends Module {
    // Prestige-Class Anti-Cheat Safe & Timing Settings
    private final Setting<Integer> minDelay = new Setting<>("MinDelayMs", 60);
    private final Setting<Integer> maxDelay = new Setting<>("MaxDelayMs", 130);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 93); // Anti-cheat randomization
    
    // Automation Features
    private final Setting<Boolean> autoPitch = new Setting<>("AutoPitchDown", true); // Glides camera down so bottles break at your feet
    private final Setting<Boolean> autoSwap = new Setting<>("AutoSwapExp", true);   // Automatically finds bottles in your hotbar

    private long nextThrowTime = 0;

    public FastExpModule() {
        super("Fast EXP", Category.PLAYER);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(successChance);
        addSetting(autoPitch);
        addSetting(autoSwap);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.world == null) return;

        // 1. Hotbar Management: Ensure you are holding experience bottles
        if (autoSwap.getValue() && !isHoldingExp(client)) {
            int expSlot = findExpHotbar(client);
            if (expSlot != -1) {
                client.player.getInventory().selectedSlot = expSlot;
            } else {
                return; // Pauses if no bottles are left in the hotbar
            }
        }

        if (!isHoldingExp(client)) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime < nextThrowTime) return;

        // 2. Anti-Cheat Stochastic Randomization (skips ~7% of ticks to look organic)
        if (Math.random() * 100.0 > successChance.getValue()) {
            return;
        }

        // 3. Smooth Auto-Pitch Down: Elegantly tilts your camera to 90 degrees so bottles hit your feet instantly
        if (autoPitch.getValue()) {
            float targetPitch = 90.0f;
            float currentPitch = client.player.getPitch();
            float smoothPitch = currentPitch + (targetPitch - currentPitch) / 2.5f; // Glides smoothly
            client.player.setPitch(smoothPitch);
        }

        // 4. Clean Interaction Packet Execution
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        client.player.swingHand(Hand.MAIN_HAND);

        // 5. Fluctuate throw intervals dynamically to bypass rate-limit heuristic checks
        long randomDelay = minDelay.getValue() + (long)(Math.random() * (maxDelay.getValue() - minDelay.getValue()));
        nextThrowTime = currentTime + randomDelay;
    }

    private boolean isHoldingExp(MinecraftClient client) {
        return client.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE);
    }

    private int findExpHotbar(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.EXPERIENCE_BOTTLE)) {
                return i;
            }
        }
        return -1;
    }
}
