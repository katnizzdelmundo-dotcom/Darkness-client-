package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

public class AutoSprintModule extends Module {

    private final Setting<Integer> modeSetting =
            new Setting<>("Mode", 0);

    private final Setting<Boolean> foodCheck =
            new Setting<>("FoodCheck", true);

    private final Setting<Boolean> checkBlindness =
            new Setting<>("CheckBlindness", true);

    private final Setting<Boolean> stopOnCollision =
            new Setting<>("StopOnCollision", true);

    public AutoSprintModule() {
        super("Auto Sprint", Category.MOVEMENT);

        addSetting(modeSetting);
        addSetting(foodCheck);
        addSetting(checkBlindness);
        addSetting(stopOnCollision);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        // Don't sprint while the player is not moving forward.
        if (!client.player.input.hasForwardMovement()) {
            client.player.setSprinting(false);
            return;
        }

        // Optional food check.
        if (foodCheck.getValue()
                && client.player.getHungerManager().getFoodLevel() <= 6) {
            client.player.setSprinting(false);
            return;
        }

        // Optional blindness check.
        if (checkBlindness.getValue()
                && client.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
            client.player.setSprinting(false);
            return;
        }

        // Optional collision check.
        if (stopOnCollision.getValue() && client.player.horizontalCollision) {
            client.player.setSprinting(false);
            return;
        }

        client.player.setSprinting(true);
    }
}
