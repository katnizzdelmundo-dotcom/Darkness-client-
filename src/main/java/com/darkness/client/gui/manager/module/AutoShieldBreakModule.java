package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;

public final class AutoShieldBreakModule extends Module {

    private final Setting<Double> rangeSetting =
            new Setting<>("Range", 4.2);

    private final Setting<Boolean> requireLineOfSight =
            new Setting<>("LineOfSight", true);

    private final Setting<Boolean> requireTargetLooking =
            new Setting<>("TargetLookingAtYou", true);

    private final Setting<Double> maxLookAngle =
            new Setting<>("MaxLookAngle", 60.0);

    private final Setting<Boolean> requireFullCooldown =
            new Setting<>("FullCooldown", true);

    private final Setting<Boolean> autoSwap =
            new Setting<>("AutoSwapAxe", true);

    private final Setting<Boolean> swapBack =
            new Setting<>("SwapBack", true);

    private final Setting<Boolean> preferDurability =
            new Setting<>("PreferDurability", true);

    private final Setting<Integer> actionDelayMs =
            new Setting<>("ActionDelayMs", 150);

    private long nextActionTime;

    private int previousSlot = -1;
    private int axeSlot = -1;

    private boolean swappedForAttack;
    private boolean waitingForSwap;

    private long swapTime;

    public AutoShieldBreakModule() {
        super("Auto Shield Break", Category.COMBAT);

        addSetting(rangeSetting);
        addSetting(requireLineOfSight);
        addSetting(requireTargetLooking);
        addSetting(maxLookAngle);
        addSetting(requireFullCooldown);
        addSetting(autoSwap);
        addSetting(swapBack);
        addSetting(preferDurability);
        addSetting(actionDelayMs);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!isReady(client)) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now < nextActionTime) {
            return;
        }

        if (waitingForSwap) {
            handleSwapState(client, now);
            return;
        }

        LivingEntity target = findBestTarget(client);

        if (target == null) {
            restoreOriginalSlot(client);
            return;
        }

        if (!passesTargetChecks(client, target)) {
            return;
        }

        if (!isHoldingAxe(client)) {
            if (!autoSwap.getValue()) {
                return;
            }

            int bestAxe = findBestAxe(client);

            if (bestAxe == -1) {
                return;
            }

            beginAxeSwap(client, bestAxe, now);
            return;
        }

        performAttack(client, target, now);
    }

    @Override
    public void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            restoreOriginalSlot(client);
        }

        waitingForSwap = false;
        nextActionTime = 0L;
    }

    private boolean isReady(MinecraftClient client) {
        return client.player != null
                && client.world != null
                && client.interactionManager != null;
    }

    private void handleSwapState(MinecraftClient client, long now) {
        if (!waitingForSwap) {
            return;
        }

        if (now - swapTime < actionDelayMs.getValue()) {
            return;
        }

        waitingForSwap = false;

        LivingEntity target = findBestTarget(client);

        if (target == null || !passesTargetChecks(client, target)) {
            restoreOriginalSlot(client);
            return;
        }

        performAttack(client, target, now);
    }

    private void beginAxeSwap(
            MinecraftClient client,
            int targetSlot,
            long now
    ) {
        if (!swappedForAttack) {
            previousSlot = client.player.getInventory().selectedSlot;
            swappedForAttack = true;
        }

        axeSlot = targetSlot;
        client.player.getInventory().selectedSlot = targetSlot;

        swapTime = now;
        waitingForSwap = true;
    }

    private void performAttack(
            MinecraftClient client,
            LivingEntity target,
            long now
    ) {
        if (!isHoldingAxe(client)) {
            restoreOriginalSlot(client);
            return;
        }

        if (requireFullCooldown.getValue()
                && client.player.getAttackCooldownProgress(0.5f) < 0.9f) {
            return;
        }

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);

        nextActionTime = now + Math.max(0, actionDelayMs.getValue());

        if (swapBack.getValue()) {
            restoreOriginalSlot(client);
        }
    }

    private boolean passesTargetChecks(
            MinecraftClient client,
            LivingEntity target
    ) {
        if (requireTargetLooking.getValue()
                && !isTargetLookingAtPlayer(client, target)) {
            return false;
        }

        if (requireLineOfSight.getValue()
                && isBlockedByWall(client, target)) {
            return false;
        }

        return true;
    }

    private LivingEntity findBestTarget(MinecraftClient client) {
        double range = Math.max(0.1, rangeSetting.getValue());

        return client.world.getEntitiesByClass(
                        LivingEntity.class,
                        client.player
                                .getBoundingBox()
                                .expand(range),
                        entity ->
                                entity != client.player
                                        && entity.isAlive()
                                        && entity.isUsingItem()
                                        && entity.getActiveItem().isOf(Items.SHIELD)
                                        && client.player.distanceTo(entity) <= range
                )
                .stream()
                .min(
                        Comparator
                                .comparingDouble(
                                        (LivingEntity entity) ->
                                                client.player.distanceTo(entity)
                                )
                                .thenComparing(
                                        LivingEntity::getHealth
                                )
                )
                .orElse(null);
    }

    private boolean isTargetLookingAtPlayer(
            MinecraftClient client,
            LivingEntity target
    ) {
        Vec3d targetEyes = target.getPos().add(
                0.0,
                target.getEyeHeight(target.getPose()),
                0.0
        );

        Vec3d playerEyes = client.player.getEyePos();

        Vec3d directionToPlayer =
                playerEyes.subtract(targetEyes).normalize();

        Vec3d targetLook =
                target.getRotationVec(1.0F).normalize();

        double dot = MathHelper.clamp(
                targetLook.dotProduct(directionToPlayer),
                -1.0,
                1.0
        );

        double angle = Math.toDegrees(Math.acos(dot));

        return angle <= Math.max(0.0, maxLookAngle.getValue());
    }

    private boolean isBlockedByWall(
            MinecraftClient client,
            LivingEntity target
    ) {
        Vec3d start = client.player.getEyePos();

        Vec3d end = target.getPos().add(
                0.0,
                target.getHeight() * 0.5,
                0.0
        );

        HitResult result = client.world.raycast(
                new RaycastContext(
                        start,
                        end,
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        client.player
                )
        );

        return result.getType() == HitResult.Type.BLOCK;
    }

    private boolean isHoldingAxe(MinecraftClient client) {
        return client.player.getMainHandStack().getItem()
                instanceof AxeItem;
    }

    private int findBestAxe(MinecraftClient client) {
        int bestSlot = -1;
        int bestScore = Integer.MIN_VALUE;

        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack =
                    client.player.getInventory().getStack(slot);

            if (!(stack.getItem() instanceof AxeItem)) {
                continue;
            }

            int score = getAxeScore(stack);

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }

        return bestSlot;
    }

    private int getAxeScore(ItemStack stack) {
        int score = 0;

        if (preferDurability.getValue()) {
            int maxDamage = stack.getMaxDamage();
            int remainingDamage = maxDamage - stack.getDamage();

            score += remainingDamage;
        }

        return score;
    }

    private void restoreOriginalSlot(MinecraftClient client) {
        if (!swappedForAttack || previousSlot < 0) {
            return;
        }

        if (client.player != null) {
            client.player.getInventory().selectedSlot =
                    previousSlot;
        }

        previousSlot = -1;
        axeSlot = -1;
        swappedForAttack = false;
        waitingForSwap = false;
    }
}