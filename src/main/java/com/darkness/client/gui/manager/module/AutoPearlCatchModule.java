package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

public class AutoPearlCatchModule extends Module {
    // Prestige-Class Anti-Cheat Safe Pearl Catch Settings
    private final Setting<Double> rangeSetting = new Setting<>("ScanRange", 16.0);
    private final Setting<Double> smoothing = new Setting<>("Smoothing", 3.0); // Smooth camera glide speed
    private final Setting<Boolean> onlyTargetEnemy = new Setting<>("OnlyEnemyPearls", true);
    
    // Humanized Reaction & Randomization Delays
    private final Setting<Integer> minReactionDelay = new Setting<>("MinReactionMs", 80);
    private final Setting<Integer> maxReactionDelay = new Setting<>("MaxReactionMs", 200);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 92); // Anti-cheat randomization

    private long actionTriggerTime = 0;
    private boolean isWaitingToCatch = false;
    private EnderPearlEntity trackedPearl = null;

    public AutoPearlCatchModule() {
        super("Auto Pearl Catch", Category.COMBAT);
        addSetting(rangeSetting);
        addSetting(smoothing);
        addSetting(onlyTargetEnemy);
        addSetting(minReactionDelay);
        addSetting(maxReactionDelay);
        addSetting(successChance);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // 1. Scan for nearby active ender pearls
        EnderPearlEntity targetPearl = findBestPearl(client);
        
        if (targetPearl == null) {
            trackedPearl = null;
            isWaitingToCatch = false;
            return;
        }

        // If a new pearl is detected, initialize humanized reaction timer
        if (trackedPearl != targetPearl) {
            trackedPearl = targetPearl;
            if (Math.random() * 100.0 <= successChance.getValue()) {
                long randomReaction = minReactionDelay.getValue() + (long)(Math.random() * (maxReactionDelay.getValue() - minReactionDelay.getValue()));
                actionTriggerTime = System.currentTimeMillis() + randomReaction;
                isWaitingToCatch = true;
            }
        }

        // 2. Execute smooth camera tracking once the humanized reaction delay has elapsed
        if (isWaitingToCatch && System.currentTimeMillis() >= actionTriggerTime) {
            predictAndTrackLanding(client, trackedPearl);
        }
    }

    private void predictAndTrackLanding(MinecraftClient client, EnderPearlEntity pearl) {
        Vec3d pearlPos = pearl.getPos();
        Vec3d pearlVel = pearl.getVelocity();

        // Physics trajectory simulation (accounting for gravity and air resistance)
        Vec3d predictedLanding = calculateLandingPosition(pearlPos, pearlVel);

        Vec3d eyesPos = client.player.getEyePos();
        double diffX = predictedLanding.x - eyesPos.x;
        double diffY = predictedLanding.y - eyesPos.y;
        double diffZ = predictedLanding.z - eyesPos.z;
        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diffY, distXZ)));

        // Smoothly interpolate view angles to eliminate unnatural mechanical snaps
        float smooth = smoothing.getValue().floatValue();
        if (smooth < 1.0f) smooth = 1.0f;

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        float adjustedYaw = currentYaw + MathHelper.wrapDegrees(targetYaw - currentYaw) / smooth;
        float adjustedPitch = currentPitch + (targetPitch - currentPitch) / smooth;

        // Subtle micro-randomization to bypass heuristic anti-cheat view-tracking algorithms
        adjustedYaw += (float) ((Math.random() - 0.5) * 0.15);
        adjustedPitch += (float) ((Math.random() - 0.5) * 0.15);

        client.player.setYaw(adjustedYaw);
        client.player.setPitch(adjustedPitch);
    }

    private Vec3d calculateLandingPosition(Vec3d pos, Vec3d vel) {
        Vec3d currentPos = pos;
        Vec3d currentVel = vel;
        
        // Simulates up to 40 ticks (2 seconds) of projectile flight path
        for (int i = 0; i < 40; i++) {
            currentPos = currentPos.add(currentVel);
            currentVel = currentVel.multiply(0.99).subtract(0.0, 0.03, 0.0); // Gravity & drag estimation
            
            // Stop calculation if it hits ground level
            if (currentPos.y <= 0 || currentPos.y > 320) break;
        }
        return currentPos;
    }

    private EnderPearlEntity findBestPearl(MinecraftClient client) {
        return client.world.getEntitiesByClass(
            EnderPearlEntity.class,
            client.player.getBoundingBox().expand(rangeSetting.getValue()),
            entity -> entity.isAlive() && (!onlyTargetEnemy.getValue() || entity.getOwner() != client.player)
        )
        .stream()
        .min(Comparator.comparingDouble(client.player::distanceTo))
        .orElse(null);
    }
}
