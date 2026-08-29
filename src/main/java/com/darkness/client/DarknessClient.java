package com.darkness.client;

import com.darkness.client.gui.DarknessPrestigeScreen;
import com.darkness.client.manager.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DarknessClient implements ClientModInitializer {
    public static final String MOD_ID = "darkness";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static ModuleManager moduleManager;
    private static KeyBinding guiKeyBinding;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Darkness] Initializing Fabric 1.21.11 client.");

        guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.darkness.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.darkness.main"
        ));

        moduleManager = new ModuleManager();
        moduleManager.initializeModules();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKeyBinding.wasPressed()) {
                client.setScreen(new DarknessPrestigeScreen());
            }

            if (client.player == null || client.world == null) return;

            for (var module : moduleManager.getModules()) {
                if (module.isEnabled()) module.onTick(client);
            }
        });
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }
}
