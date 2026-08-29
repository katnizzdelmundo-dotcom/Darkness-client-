package com.darkness.client.module;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    private final String name;
    private final Category category;
    private boolean enabled;
    private int key = -1;
    private final List<Setting<?>> settings = new ArrayList<>();

    public enum Category {
        CRYSTAL, SWORD, MACE, UTILITY, SPEAR
    }

    protected Module(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    protected final <T> Setting<T> addSetting(Setting<T> setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    public boolean isEnabled() { return enabled; }
    public String getName() { return name; }
    public Category getCategory() { return category; }
    public int getKey() { return key; }
    public void setKey(int key) { this.key = key; }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(MinecraftClient client) {}

    public static final class Setting<T> {
        private final String name;
        private final T defaultValue;
        private T value;

        public Setting(String name, T defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.value = defaultValue;
        }

        public String getName() { return name; }
        public T getValue() { return value; }
        public T getDefaultValue() { return defaultValue; }
        public void setValue(T value) { this.value = value; }
        public void reset() { this.value = defaultValue; }
    }
}
