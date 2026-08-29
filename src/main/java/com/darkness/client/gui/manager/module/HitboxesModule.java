package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.Box;

public class HitboxesModule extends Module {
    // Prestige & Doomsday Style Custom Dimensions
    private final Setting<Double> widthSetting = new Setting<>("Width", 1.2);   // Horizontal expansion (Vanilla player is 0.6)
    private final Setting<Double> heightSetting = new Setting<>("Height", 1.9); // Vertical expansion (Vanilla player is 1.8)
    private final Setting<Boolean> playersOnly = new Setting<>("PlayersOnly", true);
    
    // Smart Anti-Detection / Cobweb Stealth Feature
    private final Setting<Boolean> autoDisableInWeb = new Setting<>("AutoDisableInWeb", true); 

    public HitboxesModule() {
        super("Hitboxes", Category.COMBAT);
        addSetting(widthSetting);
        addSetting(heightSetting);
        addSetting(playersOnly);
        addSetting(autoDisableInWeb);
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        double expandW = widthSetting.getValue() / 2.0;
        double expandH = heightSetting.getValue();

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (entity == client.player) continue;
            if (playersOnly.getValue() && !(entity instanceof PlayerEntity)) continue;

            // Smart Stealth Check: If target player is stuck in a cobweb, bypass hitbox modification
            if (autoDisableInWeb.getValue() && isEntityInCobweb(living)) {
                continue; // Leaves target at vanilla size so behavior looks completely natural when immobilized
            }

            // Apply custom client-side bounding box expansion
            applyExpandedHitbox(living, expandW, expandH);
        }
    }

    private boolean isEntityInCobweb(LivingEntity entity) {
        // Checks blocks at the target's feet and torso positions for cobwebs
        return entity.getWorld().getBlockState(entity.getBlockPos()).isOf(Blocks.COBWEB) 
                || entity.getWorld().getBlockState(entity.getBlockPos().up()).isOf(Blocks.COBWEB);
    }

    private void applyExpandedHitbox(LivingEntity entity, double expandW, double expandH) {
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        
        Box expandedBox = new Box(
            x - expandW, y, z - expandW,
            x + expandW, y + expandH, z + expandW
        );
        entity.setBoundingBox(expandedBox);
    }
}
