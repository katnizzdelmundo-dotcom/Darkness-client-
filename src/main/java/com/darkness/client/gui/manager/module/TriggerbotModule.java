package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;

public class TriggerbotModule extends Module {

    private final Setting<Double> rangeSetting =
            new Setting<>("Range", 4.2);

    private final Setting<Integer> minDelay =
            new Setting<>("MinDelayMs", 45);

    private final Setting<Integer> maxDelay =
            new Setting<>("MaxDelayMs", 110);

    private final Setting<Integer> successChance =
            new Setting<>("SuccessChance", 94);

    private final Setting<Boolean> syncCooldown =
            new Setting<>("SyncCooldown", true);

    private final Setting<Boolean> raycastCheck =
            new Setting<>("RaycastCheck", true);

    private final Setting<Boolean> weaponOnly =
            new Setting<>("WeaponOnly", true);

    private final Setting<Boolean> silentAim =
            new Setting<>("SilentAim", true);

    private final Setting<Double> silentAimFOV =
            new Setting<>("SilentAimFOV", 80.0);

    private final Setting<Double> smoothing =
            new Setting<>("Smoothing", 2.0);

    private final Setting<Boolean> targetPrediction =
            new Setting<>("TargetPrediction", true);

    private long nextAttackTime = 0L;

    public TriggerbotModule() {
        super("Triggerbot", Category.COMBAT);

        addSetting(rangeSetting);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(successChance);
        addSetting(syncCooldown);
        addSetting(raycastCheck);
        addSetting(weaponOnly);
        addSetting(silentAim);
        addSetting(silentAimFOV);
        addSetting(smoothing);
        addSetting(targetPrediction);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null
                || client.interactionManager == null
                || client.world == null) {
            return;
        }

        if (weaponOnly.getValue()) {
            var mainStack = client.player.getMainHandStack();

            boolean isWeapon =
                    mainStack.isIn(ItemTags.SWORDS)
                            || mainStack.getItem() instanceof AxeItem
                            || mainStack.getItem() instanceof MaceItem
                            || mainStack.isOf(Items.MACE);

            if (!isWeapon) {
                return;
            }
        }

        LivingEntity target = null;

        if (silentAim.getValue()) {
            target = findBestTarget(client);

            if (target != null) {
                applyAdvancedAim(client, target);
            }
        } else if (client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof LivingEntity living) {
            target = living;
        }

        if (target == null
                || !target.isAlive()
                || client.player.distanceTo(target) > rangeSetting.getValue()) {
            return;
        }

        if (raycastCheck.getValue()
                && isBlockedByWall(client, target)) {
            return;
        }

        if (syncCooldown.getValue()
                && client.player.getAttackCooldownProgress(0.5f) < 0.9f) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (currentTime < nextAttackTime) {
            return;
        }

        if (Math.random() * 100.0 <= successChance.getValue()) {
            client.interactionManager.attackEntity(
                    client.player,
                    target
            );

            client.player.swingHand(Hand.MAIN_HAND);
        }

        int min = Math.max(0, minDelay.getValue());
        int max = Math.max(min, maxDelay.getValue());

        long randomDelay = min;

        if (max > min) {
            randomDelay += (long) (
                    Math.random() * (max - min + 1)
            );
        }

        nextAttackTime = currentTime + randomDelay;
    }

    private void applyAdvancedAim(
            MinecraftClient client,
            LivingEntity target) {

        Vec3d eyesPos = client.player.getEyePos();

        Vec3d targetPos = target.getPos().add(
                0.0,
                target.getHeight() / 2.0,
                0.0
        );

        if (targetPrediction.getValue()) {
            Vec3d velocity = target.getVelocity();

            targetPos = targetPos.add(
                    velocity.x * 1.5,
                    velocity.y * 0.5,
                    velocity.z * 1.5
            );
        }

        double diffX = targetPos.x - eyesPos.x;
        double diffY = targetPos.y - eyesPos.y;
        double diffZ = targetPos.z - eyesPos.z;

        double distXZ = Math.sqrt(
                diffX * diffX + diffZ * diffZ
        );

        float targetYaw = (float) (
                Math.toDegrees(
                        Math.atan2(diffZ, diffX)
                ) - 90.0
        );

        float targetPitch = (float) (
                -Math.toDegrees(
                        Math.atan2(diffY, distXZ)
                )
        );

        float yawDifference = MathHelper.wrapDegrees(
                targetYaw - client.player.getYaw()
        );

        if (Math.abs(yawDifference)
                > silentAimFOV.getValue() / 2.0) {
            return;
        }

        float smooth = smoothing.getValue().floatValue();

        if (smooth < 1.0f) {
            smooth = 1.0f;
        }

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        float adjustedYaw =
                currentYaw
                        + MathHelper.wrapDegrees(
                        targetYaw - currentYaw
                ) / smooth;

        float adjustedPitch =
                currentPitch
                        + (targetPitch - currentPitch) / smooth;

        client.player.setYaw(adjustedYaw);
        client.player.setPitch(adjustedPitch);
    }

    private boolean isBlockedByWall(
            MinecraftClient client,
            LivingEntity target) {

        Vec3d eyes = client.player.getEyePos();

        Vec3d targetCenter = target.getPos().add(
                0.0,
                target.getHeight() / 2.0,
                0.0
        );

        HitResult result = client.world.raycast(
                new RaycastContext(
                        eyes,
                        targetCenter,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        client.player
                )
        );

        return result.getType() == HitResult.Type.BLOCK;
    }

    private LivingEntity findBestTarget(MinecraftClient client) {
        return client.world.getEntitiesByClass(
                        LivingEntity.class,
                        client.player.getBoundingBox()
                                .expand(rangeSetting.getValue()),
                        entity ->
                                entity != client.player
                                        && entity.isAlive()
                                        && client.player.distanceTo(entity)
                                        <= rangeSetting.getValue()
                )
                .stream()
                .min(
                        Comparator.comparingDouble(
                                client.player::
