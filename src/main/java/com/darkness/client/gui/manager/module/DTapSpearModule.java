package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

/** D-Tap helper using normal client-side combat calls and vanilla cooldown state. */
public final class DTapSpearModule extends Module {
    private final Setting<Boolean> sequence = addSetting(new Setting<>("Sequence", true));
    private final Setting<Integer> delayMs = addSetting(new Setting<>("DelayMs", 70));
    private final Setting<Double> minFallDistance = addSetting(new Setting<>("MinFallDistance", 1.2));
    private final Setting<Double> range = addSetting(new Setting<>("Range", 4.3));
    private final Setting<Boolean> requireMace = addSetting(new Setting<>("RequireMace", true));
    private final Setting<Boolean> autoSwitch = addSetting(new Setting<>("AutoSwitch", true));
    private final Setting<Boolean> swapBack = addSetting(new Setting<>("SwapBack", true));
    private final Setting<Boolean> cooldownSync = addSetting(new Setting<>("CooldownSync", true));

    private State state = State.READY;
    private long firstHitAt;
    private int originalSlot = -1;
    private boolean swapped;

    private enum State { READY, WAITING }

    public DTapSpearModule() {
        super("D-Tap Mace", Category.MACE);
    }

    @Override
    public void onDisable() {
        reset(MinecraftClient.getInstance());
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!sequence.getValue() || client.player == null || client.world == null || client.interactionManager == null) {
            reset(client);
            return;
        }

        LivingEntity target = getTarget(client);
        if (target == null || client.player.fallDistance < minFallDistance.getValue()) {
            reset(client);
            return;
        }

        if (!prepareWeapon(client)) {
            reset(client);
            return;
        }

        long now = System.currentTimeMillis();
        if (state == State.READY) {
            if (!canAttack(client)) return;
            attack(client, target);
            firstHitAt = now;
            state = State.WAITING;
            return;
        }

        if (now - firstHitAt < clampDelay()) return;
        if (!canAttack(client)) return;

        attack(client, target);
        reset(client);
    }

    private LivingEntity getTarget(MinecraftClient client) {
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) return null;
        if (!(hit.getEntity() instanceof LivingEntity living)) return null;
        if (living == client.player || !living.isAlive()) return null;
        return client.player.distanceTo(living) <= range.getValue() ? living : null;
    }

    private boolean prepareWeapon(MinecraftClient client) {
        if (client.player.getMainHandStack().isOf(Items.MACE)) return true;
        if (!autoSwitch.getValue()) return !requireMace.getValue();

        int maceSlot = findMace(client);
        if (maceSlot < 0) return !requireMace.getValue();

        if (!swapped) {
            originalSlot = client.player.getInventory().selectedSlot;
            swapped = true;
        }
        client.player.getInventory().selectedSlot = maceSlot;
        return true;
    }

    private int findMace(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.MACE)) return i;
        }
        return -1;
    }

    private boolean canAttack(MinecraftClient client) {
        return !cooldownSync.getValue() || client.player.getAttackCooldownProgress(0.0f) >= 0.9f;
    }

    private void attack(MinecraftClient client, LivingEntity target) {
        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private long clampDelay() {
        return Math.max(0, Math.min(1000, delayMs.getValue()));
    }

    private void reset(MinecraftClient client) {
        state = State.READY;
        firstHitAt = 0L;
        if (swapBack.getValue() && swapped && client.player != null && originalSlot >= 0 && originalSlot < 9) {
            client.player.getInventory().selectedSlot = originalSlot;
        }
        originalSlot = -1;
        swapped = false;
    }
}
