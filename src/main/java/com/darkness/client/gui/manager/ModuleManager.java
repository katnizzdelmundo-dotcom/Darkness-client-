package com.darkness.client.manager;

import com.darkness.client.module.AutoAnchorModule;
import com.darkness.client.module.AutoCrystalModule;
import com.darkness.client.module.AutoHitCrystalModule;
import com.darkness.client.module.AutoJumpResetModule;
import com.darkness.client.module.AutoMaceModule;
import com.darkness.client.module.AutoPearlCatchModule;
import com.darkness.client.module.AutoShieldBreakModule;
import com.darkness.client.module.AutoSprintModule;
import com.darkness.client.module.AutoSwordModule;
import com.darkness.client.module.AimAssistModule;
import com.darkness.client.module.DTapSpearModule;
import com.darkness.client.module.FastExpModule;
import com.darkness.client.module.HitboxesModule;
import com.darkness.client.module.HoverTotemModule;
import com.darkness.client.module.LungeModule;
import com.darkness.client.module.Module;
import com.darkness.client.module.SilentAimModule;
import com.darkness.client.module.TriggerbotModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {

    private final List<Module> modules = new ArrayList<>(32);

    public void initializeModules() {
        if (!modules.isEmpty()) {
            return;
        }

        modules.add(new AutoCrystalModule());
        modules.add(new AutoHitCrystalModule());
        modules.add(new AutoAnchorModule());
        modules.add(new HoverTotemModule());
        modules.add(new TriggerbotModule());
        modules.add(new AutoShieldBreakModule());
        modules.add(new SilentAimModule());
        modules.add(new AimAssistModule());
        modules.add(new HitboxesModule());
        modules.add(new AutoMaceModule());
        modules.add(new AutoPearlCatchModule());
        modules.add(new AutoJumpResetModule());
        modules.add(new FastExpModule());
        modules.add(new AutoSprintModule());
        modules.add(new LungeModule());
        modules.add(new DTapSpearModule());
        modules.add(new AutoSwordModule());
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public Module getByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }

        return null;
    }

    public int enabledCount() {
        int count = 0;

        for (Module module : modules) {
            if (module.isEnabled()) {
                count++;
            }
        }

        return count;
    }
}
