package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import java.util.Comparator;

public class AimAssistModule extends Module {
    // Customizable anti-cheat safe settings
    private final Setting<Double> rangeSetting = new Setting<>("Range", 4.5);
    private final Setting<Double> fovSetting = new Setting<>("FOV", 60.0); // Strict view check for legit play
    private final Setting<Double> speedSetting = new Setting<>("Speed", 3.0); // Pull smoothness multiplier
    private final Setting<Boolean> clickOnlySetting = new Setting<>("ClickOnly", false); // Only assist when fighting

    public AimAssistModule() {
        super("Aim Assist", Category.SWORD);
        addSetting(rangeSetting);
        addSetting(fovSetting);
        addSetting(speedSetting);
        addSetting(clickOnlySetting);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        // If ClickOnly is enabled, ensure player is holding left click or attacking
        if (clickOnlySetting.getValue() && !client.options.attackKey.isPressed()) {
            return;
        }

        LivingEntity target = findBestTarget(client);
        if (target == null) return;

        // Calculate angles to the target's center mass
        Vec3d eyesPos = client.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2.0, 0);

        double diffX = targetPos.x - eyesPos.x;
        double diffZ = targetPos.z - eyesPos.z;

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float currentYaw = client.player.getYaw();
        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);

        // Strict FOV enforcement: Only pull if target is close to your crosshair view
        if (Math.abs(yawDiff) > fovSetting.getValue() / 2.0) {
            return;
        }

        // Smooth interpolation pull to mimic natural human mouse tracking
        float speed = speedSetting.getValue().floatValue();
        if (speed < 1.0f) speed = 1.0f;

        float adjustedYaw = currentYaw + (yawDiff / speed);

        // Micro-jitter randomization: Throws off server heuristic anti-cheats that check for robotic straight lines
        adjustedYaw += (float) ((Math.random() - 0.5) * 0.2);

        client.player.setYaw(adjustedYaw);
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
