package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.util.hit.HitResult;

import java.util.Comparator;

public class AutoHitCrystalModule extends Module {
    // Prestige-Class Anti-Cheat Safe Crystal Breaker Settings
    private final Setting<Double> rangeSetting = new Setting<>("Range", 4.5);
    private final Setting<Integer> minDelay = new Setting<>("MinDelayMs", 35);
    private final Setting<Integer> maxDelay = new Setting<>("MaxDelayMs", 95);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 93); // Anti-cheat randomization percentage
    
    // Safety & Anti-Detection Features
    private final Setting<Boolean> raycastCheck = new Setting<>("RaycastCheck", true); // Blocks wall-hitting flags
    private final Setting<Boolean> antiSuicide = new Setting<>("AntiSuicide", true); // Pauses breaker if low health & close proximity

    private long nextHitTime = 0;

    public AutoHitCrystalModule() {
        super("Auto Hit Crystal", Category.COMBAT);
        addSetting(rangeSetting);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(successChance);
        addSetting(raycastCheck);
        addSetting(antiSuicide);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.world == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime < nextHitTime) return;

        // Anti-cheat stochastic randomization check (skips 7% of ticks to avoid robotic bot detection)
        if (Math.random() * 100.0 > successChance.getValue()) {
            return;
        }

        // Locate the closest active End Crystal within range
        EndCrystalEntity targetCrystal = findBestCrystal(client);
        if (targetCrystal == null) return;

        // Anti-Suicide Proximity Safety: Stops breaking close-range crystals if you are low on HP
        if (antiSuicide.getValue() && client.player.distanceTo(targetCrystal) <= 2.5 && client.player.getHealth() <= 12.0f) {
            return;
        }

        // Raycast Line-of-Sight Check: Ensures a solid block isn't obstructing the strike
        if (raycastCheck.getValue() && isBlockedByWall(client, targetCrystal)) {
            return;
        }

        // Execute clean, anti-cheat safe attack packet
        client.interactionManager.attackEntity(client.player, targetCrystal);
        client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        // Fluctuate the next hit interval dynamically with randomized millisecond delays
        long randomDelay = minDelay.getValue() + (long)(Math.random() * (maxDelay.getValue() - minDelay.getValue()));
        nextHitTime = currentTime + randomDelay;
    }

    private EndCrystalEntity findBestCrystal(MinecraftClient client) {
        return client.world.getEntitiesByClass(
            EndCrystalEntity.class,
            client.player.getBoundingBox().expand(rangeSetting.getValue()),
            entity -> entity.isAlive() && client.player.distanceTo(entity) <= rangeSetting.getValue()
        )
        .stream()
        .min(Comparator.comparingDouble(client.player::distanceTo))
        .orElse(null);
    }

    private boolean isBlockedByWall(MinecraftClient client, Entity target) {
        Vec3d eyes = client.player.getEyePos();
        Vec3d targetPos = target.getPos();
        
        HitResult result = client.world.raycast(new RaycastContext(
            eyes, targetPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            client.player
        ));
        
        return result.getType() == HitResult.Type.BLOCK;
    }
}
