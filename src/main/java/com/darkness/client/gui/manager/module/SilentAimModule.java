package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;

public class SilentAimModule extends Module {
    // Advanced anti-cheat bypass & targeting settings
    private final Setting<Double> rangeSetting = new Setting<>("Range", 4.5);
    private final Setting<Double> fovSetting = new Setting<>("FOV", 90.0); // Blocks 360-degree snap flags
    private final Setting<Double> smoothingSetting = new Setting<>("Smoothing", 3.0); // Smooths out angle snaps
    private final Setting<Boolean> strictBypass = new Setting<>("StrictBypass", true); // Adds human jitter

    public SilentAimModule() {
        super("Silent Aim", Category.SWORD);
        addSetting(rangeSetting);
        addSetting(fovSetting);
        addSetting(smoothingSetting);
        addSetting(strictBypass);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        LivingEntity target = findBestTarget(client);
        if (target == null) return;

        // Aim directly at the center mass of the target box to look completely legitimate
        Vec3d eyesPos = client.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2.0, 0);
        
        double diffX = targetPos.x - eyesPos.x;
        double diffY = targetPos.y - eyesPos.y;
        double diffZ = targetPos.z - eyesPos.z;
        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diffY, distXZ)));

        // Strict FOV check: Anti-cheats instantly flag clients that snap to targets outside your screen view
        float yawDiff = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
        if (Math.abs(yawDiff) > fovSetting.getValue() / 2.0) {
            return;
        }

        // Smoothing calculation to prevent instant look-angle snaps
        float smooth = smoothingSetting.getValue().floatValue();
        if (smooth < 1.0f) smooth = 1.0f;

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        float adjustedYaw = currentYaw + MathHelper.wrapDegrees(targetYaw - currentYaw) / smooth;
        float adjustedPitch = currentPitch + (targetPitch - currentPitch) / smooth;

        // Micro-randomization: Tricks heuristic anti-cheat checks by adding subtle human cursor jitter
        if (strictBypass.getValue()) {
            adjustedYaw += (float) ((Math.random() - 0.5) * 0.35);
            adjustedPitch += (float) ((Math.random() - 0.5) * 0.35);
        }

        // Apply smooth target adjustments safely
        client.player.setYaw(adjustedYaw);
        client.player.setPitch(adjustedPitch);
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
