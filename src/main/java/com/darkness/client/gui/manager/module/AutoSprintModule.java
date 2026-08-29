package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.status.StatusEffects;

public class AutoSprintModule extends Module {
    // Prestige-Class Anti-Cheat & Performance Settings
    private final Setting<Integer> modeSetting = new Setting<>("Mode", 0); 
    // Mode 0: Legit (Strict forward-only sprinting, bypasses all strict checks)
    // Mode 1: Omni (Multi-directional sprinting with vector smoothing for Grim/Vulcan)

    private final Setting<Boolean> foodCheck = new Setting<>("FoodCheck", true); // Prevents sprinting when hunger <= 6
    private final Setting<Boolean> checkBlindness = new Setting<>("CheckBlindness", true); // Respects vanilla blindness restrictions
    private final Setting<Boolean> stopOnCollision = new Setting<>("StopOnCollision", true); // Prevents wall-grind speed flags

    public AutoSprintModule() {
        super("Auto Sprint", Category.MOVEMENT);
        addSetting(modeSetting);
        addSetting(foodCheck);
        addSetting(checkBlindness);
        addSetting(stopOnCollision);
    }

    @Override
    public void onTick(MinecraftClient client) { // Wait, ensure proper spelling: public void onTick
