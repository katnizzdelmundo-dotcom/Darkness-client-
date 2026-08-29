package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class LungeModule extends Module {
    // Universal Peripheral Binding Settings
    private final Setting<Integer> inputType = new Setting<>("InputDevice", 1); // 0 = Keyboard, 1 = Mouse Button
    private final Setting<Integer> keyCode = new Setting<>("KeyOrButtonCode", GLFW.GLFW_MOUSE_BUTTON_4); 
    // Note: For Mouse: 0 = M1 (Left), 1 = M2 (Right), 2 = M3 (Middle), 3 = M4 (Side), 4 = M5 (Side)
    // For Keyboard: Use standard GLFW key codes (e.g., GLFW.GLFW_KEY_C for 'C', GLFW.GLFW_KEY_SPACE, etc.)

    // Anti-Cheat Safe & Performance Settings
    private final Setting<Double> lungePower = new Setting<>("LungePower", 1.35);
    private final Setting<Integer> boostDuration = new Setting<>("DurationTicks", 5);
    private final Setting<Integer> cooldownMs = new Setting<>("CooldownMs", 300);
    private final Setting<Integer> successChance = new Setting<>("SuccessChance", 94); // Anti-cheat randomization

    private long lastLungeTime = 0;
    private int activeBoostTicks = 0;

    public LungeModule() {
        super("Lunge Macro", Category.MOVEMENT);
        addSetting(inputType);
        addSetting(keyCode);
        addSetting(lungePower);
        addSetting(boostDuration);
        addSetting(cooldownMs);
        addSetting(successChance);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        long currentTime = System.currentTimeMillis();
        boolean isInputActive = checkUniversalInput(client);

        if (isInputActive) {
            if (currentTime - lastLungeTime >= cooldownMs.getValue()) {
                // Randomization check prevents anti-cheat pattern detection
                if (Math.random() * 100.0 <= successChance.getValue()) {
                    triggerLungeImpulse(client);
                    lastLungeTime = currentTime;
                    activeBoostTicks = boostDuration.getValue();
                }
            }
        }

        // Smoothly handle momentum over the active ticks to prevent instant vector change speed flags
        if (activeBoostTicks > 0) {
            applyMomentumSmoothing(client);
            activeBoostTicks--;
        }
    }

    private boolean checkUniversalInput(MinecraftClient client) {
        long window = client.getWindow().getHandle();
        int code = keyCode.getValue();

        // 0 checks keyboard keys, 1 checks mouse buttons (M1, M2, M3, side buttons)
        if (inputType.getValue() == 0) {
            return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
        } else {
            return GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
        }
    }

    private void triggerLungeImpulse(MinecraftClient client) {
        float playerYaw = client.player.getYaw() * (float) (Math.PI / 180.0);
        double multiplier = lungePower.getValue() * 0.12;

        // Apply a forward directional push aligned smoothly with client view angles
        client.player.addVelocity(-Math.sin(playerYaw) * multiplier, 0.06, Math.cos(playerYaw) * multiplier);
        client.player.velocityModified = true;
    }

    private void applyMomentumSmoothing(MinecraftClient client) {
        // Damping factor prevents server-side position rollback glitches
        client.player.setVelocity(
            client.player.getVelocity().x * 1.025,
            client.player.getVelocity().y,
            client.player.getVelocity().z * 1.025
        );
    }
}
