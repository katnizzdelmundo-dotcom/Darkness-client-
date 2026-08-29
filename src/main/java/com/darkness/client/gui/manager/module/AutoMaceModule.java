package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

public class AutoMaceModule extends Module {
    // Combat & Range Settings
    private final Setting<Double> rangeSetting = new Setting<>("Range", 4.3);
    private final Setting<Boolean> autoSwap = new Setting<>("AutoSwap", true);
    
    // Swap Timings & Swap-Back Settings
    private final Setting<Integer> swapDelay = new Setting<>("SwapDelayMs", 45);
    private final Setting<Boolean> swapBack = new Setting<>("SwapBack", true);
    private final Setting<Integer> swapBackDelay = new Setting<>("SwapBackDelayMs", 60);

    // Advanced Combat Features
    private final Setting<Boolean> shieldBypass = new Setting<>("ShieldBypass", true);
    private final Setting<Boolean> strictFallOnly = new Setting<>("StrictFallOnly", true);
    private final Setting<Double> minTargetHealth = new Setting<>("MinTargetHealth", 0.0);

    // Integrated Stun Slam (Fall-Damage Smash) & Customizable Tick Settings
    private final Setting<Boolean> stunSlamEnabled = new Setting<>("StunSlam", true);
    private final Setting<Double> minFallDistance = new Setting<>("MinFallDistance", 1.4);
    private final Setting<Integer> stunChance = new Setting<>("StunChance", 94);
    private final Setting<Integer> stunTickInterval = new Setting<>("StunTickInterval", 1); // 1 = Every tick, 2+ = Throttled ticks for anti-cheat

    // Integrated Legit Silent Aim & Smoothing Settings
    private final Setting<Boolean> silentAimEnabled = new Setting<>("SilentAim", true);
    private final Setting<Double> silentFOV = new Setting<>("SilentFOV", 70.0);
    private final Setting<Double> smoothing = new Setting<>("Smoothing", 2.2);
    private final Setting<Boolean> microJitter = new Setting<>("MicroJitter", true);

    // Anti-Cheat Pacing & Randomization
    private final Setting<Integer> minDelay = new Setting<>("MinDelayMs", 45);
    private final Setting<Integer> maxDelay = new Setting<>("MaxDelayMs", 110);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 95);

    private long nextAttackTime = 0;
    private long swapTimestamp = 0;
    private long swapBackTime = 0;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private boolean hasSwapped = false;
    private boolean waitingToHit = false;
    private boolean pendingSwapBack = false;

    public AutoMaceModule() {
        super("Auto Mace", Category.COMBAT);
        addSetting(rangeSetting);
        addSetting(autoSwap);
        addSetting(swapDelay);
        addSetting(swapBack);
        addSetting(swapBackDelay);
        addSetting(shieldBypass);
        addSetting(strictFallOnly);
        addSetting(minTargetHealth);
        addSetting(stunSlamEnabled);
        addSetting(minFallDistance);
        addSetting(stunChance);
        addSetting(stunTickInterval);
        addSetting(silentAimEnabled);
        addSetting(silentFOV);
        addSetting(smoothing);
        addSetting(microJitter);
        addSetting(minDelay);
        addSetting(maxDelay);
        addSetting(successChance);
    }

    @Override
    public void onDisable() {
        resetSlot(MinecraftClient.getInstance());
        waitingToHit = false;
        pendingSwapBack = false;
        tickCounter = 0;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.world == null) return;

        long currentTime = System.currentTimeMillis();

        // Handle smooth delayed swap back to your original item
        if (pendingSwapBack) {
            if (currentTime >= swapBackTime) {
                resetSlot(client);
                pendingSwapBack = false;
            }
            return;
        }

        LivingEntity target = findBestTarget(client);
        if (target == null || target.getHealth() < minTargetHealth.getValue()) {
            if (!waitingToHit && !pendingSwapBack) resetSlot(client);
            return;
        }

        // 1. Execute Integrated Legit Silent Aim Tracking
        if (silentAimEnabled.getValue()) {
            applyLegitSilentAim(client, target);
        }

        // 2. Customizable Stun Tick Throttle & Fall Distance Validation
        tickCounter++;
        if (tickCounter < stunTickInterval.getValue()) {
            return; // Throttles evaluation based on user setting (1, 2, or more ticks)
        }
        tickCounter = 0;

        boolean isFalling = client.player.fallDistance >= minFallDistance.getValue();
        if (strictFallOnly.getValue() && !isFalling) {
            return; 
        }

        if (stunSlamEnabled.getValue() && isFalling) {
            if (Math.random() * 100.0 > stunChance.getValue()) {
                return; 
            }
        }

        // 3. Determine Weapon Strategy (Shield Bypass vs Mace Smash)
        boolean targetShielding = target.isUsingItem() && target.getActiveItem().isOf(Items.SHIELD);
        boolean useAxe = shieldBypass.getValue() && targetShielding;

        int targetSlot = -1;
        if (useAxe) {
            targetSlot = findAxeHotbar(client);
        }
        if (targetSlot == -1) {
            targetSlot = findMaceHotbar(client);
        }

        // 4. Hotbar Management & Swap Delay Buffer
        if (autoSwap.getValue() && targetSlot != -1) {
            ItemStack currentStack = client.player.getMainHandStack();
            boolean holdingCorrect = useAxe ? (currentStack.getItem() instanceof AxeItem) : isHoldingMace(client);

            if (!holdingCorrect) {
                if (!hasSwapped) {
                    originalSlot = client.player.getInventory().selectedSlot;
                    hasSwapped = true;
                }
                client.player.getInventory().selectedSlot = targetSlot;
                swapTimestamp = currentTime;
                waitingToHit = true;
                return;
            }
        }

        // 5. Strike Execution after Swap Delay Buffer
        if (waitingToHit) {
            if (currentTime - swapTimestamp >= swapDelay.getValue()) {
                if (currentTime >= nextAttackTime) {
                    if (Math.random() * 100.0 <= successChance.getValue()) {
                        client.interactionManager.attackEntity(client.player, target);
                        client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

                        if (swapBack.getValue() && hasSwapped) {
                            swapBackTime = currentTime + swapBackDelay.getValue();
                            pendingSwapBack = true;
                        }
                    }
                    long randomDelay = minDelay.getValue() + (long)(Math.random() * (maxDelay.getValue() - minDelay.getValue()));
                    nextAttackTime = currentTime + randomDelay;
                }
                waitingToHit = false;
            }
            return;
        }

        // 6. Direct Attack Execution if Already Holding Item
        if (currentTime >= nextAttackTime) {
            if (Math.random() * 100.0 <= successChance.getValue()) {
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

                if (swapBack.getValue() && hasSwapped) {
                    swapBackTime = currentTime + swapBackDelay.getValue();
                    pendingSwapBack = true;
                }
            }

            long randomDelay = minDelay.getValue() + (long)(Math.random() * (maxDelay.getValue() - minDelay.getValue()));
            nextAttackTime = currentTime + randomDelay;
        }
    }

    private void applyLegitSilentAim(MinecraftClient client, LivingEntity target) {
        Vec3d eyesPos = client.player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2.0, 0);

        double diffX = targetPos.x - eyesPos.x;
        double diffY = targetPos.y - eyesPos.y;
        double diffZ = targetPos.z - eyesPos.z;
        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(diffY, distXZ)));

        float yawDiff = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
        if (Math.abs(yawDiff) > silentFOV.getValue() / 2.0) {
            return;
        }

        float smooth = smoothing.getValue().floatValue();
        if (smooth < 1.0f) smooth = 1.0f;

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        float adjustedYaw = currentYaw + MathHelper.wrapDegrees(targetYaw - currentYaw) / smooth;
        float adjustedPitch = currentPitch + (targetPitch - currentPitch) / smooth;

        if (microJitter.getValue()) {
            adjustedYaw += (float) ((Math.random() - 0.5) * 0.2);
            adjustedPitch += (float) ((Math.random() - 0.5) * 0.2);
        }

        client.player.setYaw(adjustedYaw);
        client.player.setPitch(adjustedPitch);
    }

    private boolean isHoldingMace(MinecraftClient client) {
        return client.player.getMainHandStack().isOf(Items.MACE);
    }

    private int findMaceHotbar(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.MACE)) return i;
        }
        return -1;
    }

    private int findAxeHotbar(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) return i;
        }
        return -1;
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
