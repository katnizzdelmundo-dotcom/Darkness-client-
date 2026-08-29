package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;

public class AutoJumpResetModule extends Module {
    // Prestige-Class Anti-Cheat Safe Jump Reset Settings
    private final Setting<Integer> minDelay = new Setting<>("MinDelayMs", 25);
    private final Setting<Integer> maxDelay = new Setting<>("MaxDelayMs", 75);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 91); // Anti-cheat randomization percentage
    private final Setting<Boolean> onlyGrounded = new Setting<>("OnlyGrounded", true); // Prevents mid-air jump spam flags

    private int lastHurtTime = 0;
    private long jumpTriggerTime = 0;
    private boolean pendingJump = false;

    public AutoJumpResetModule() {
        super("Auto Jump Reset", Category.COMBAT);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(successChance);
        addSetting(onlyGrounded);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        int currentHurt = client.player.hurtTime;

        // Detect the exact frame damage is registered (hurtTime changes from 0 to max)
        if (currentHurt > 0 && lastHurtTime == 0) {
            // Anti-cheat stochastic randomization check (skips ~9% of triggers to look organic)
            if (Math.random() * 100.0 <= successChance.getValue()) {
                // Ground check to prevent weird motion flags while falling or flying
                if (!onlyGrounded.getValue() || client.player.isOnGround()) {
                    // Introduce a subtle humanized millisecond delay instead of instant frame snapping
                    long randomDelay = minDelay.getValue() + (long)(Math.random() * (maxDelay.getValue() - minDelay.getValue()));
                    jumpTriggerTime = System.currentTimeMillis() + randomDelay;
                    pendingJump = true;
                }
            }
        }
        lastHurtTime = currentHurt;

        // Execute the delayed jump reset safely once the random millisecond window is reached
        if (pendingJump && System.currentTimeMillis() >= jumpTriggerTime) {
            client.player.jump();
            pendingJump = false;
        }
    }
}
