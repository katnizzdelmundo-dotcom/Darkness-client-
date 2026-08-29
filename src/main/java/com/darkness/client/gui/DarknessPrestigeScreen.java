package com.darkness.client.gui;

import com.darkness.client.DarknessClient;
import com.darkness.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Dynamic Darkness Prestige interface backed by the real ModuleManager. */
public final class DarknessPrestigeScreen extends Screen {
    private static final int BG = 0xFF08070D;
    private static final int PANEL = 0xFF110D18;
    private static final int PANEL_ALT = 0xFF17101F;
    private static final int BORDER = 0xFF2C1B3E;
    private static final int PURPLE = 0xFF9B3CFF;
    private static final int PURPLE_DARK = 0xFF5B1E91;
    private static final int WHITE = 0xFFF2EFFF;
    private static final int MUTED = 0xFF93899D;
    private static final int GREEN = 0xFF55E68A;

    private final List<Module> visible = new ArrayList<>();
    private Module selected;
    private Module.Category category;
    private String search = "";
    private int scroll;

    public DarknessPrestigeScreen() {
        super(Text.literal("Darkness Prestige"));
        category = Module.Category.MACE;
    }

    @Override
    protected void init() {
        rebuildVisible();
    }

    private void rebuildVisible() {
        visible.clear();
        if (DarknessClient.getModuleManager() == null) return;
        String query = search.toLowerCase(Locale.ROOT);
        for (Module module : DarknessClient.getModuleManager().getModules()) {
            if (module.getCategory() != category) continue;
            if (!query.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(query)) continue;
            visible.add(module);
        }
        if (selected == null || !visible.contains(selected)) {
            selected = visible.isEmpty() ? null : visible.get(0);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, BG);
        drawHeader(ctx);
        drawSidebar(ctx);
        drawModules(ctx, mouseX, mouseY);
        drawInspector(ctx);
        drawFooter(ctx);
    }

    private void drawHeader(DrawContext ctx) {
        ctx.fill(0, 0, width, 31, 0xFF0B0911);
        ctx.fill(0, 29, width, 31, PURPLE_DARK);
        text(ctx, "DARKNESS", 18, 7, PURPLE, true);
        text(ctx, "PRESTIGE", 20, 20, WHITE, false);
        text(ctx, "1.21.11", 110, 13, MUTED, false);
        text(ctx, search.isEmpty() ? "SEARCH  /  type here" : "SEARCH  /  " + search, 285, 12, search.isEmpty() ? MUTED : WHITE, false);
    }

    private void drawSidebar(DrawContext ctx) {
        ctx.fill(0, 31, 205, height - 24, 0xFF0C0A12);
        ctx.fill(204, 31, 205, height - 24, BORDER);
        text(ctx, "MODULES", 18, 52, MUTED, true);

        int y = 78;
        for (Module.Category value : Module.Category.values()) {
            boolean active = value == category;
            if (active) {
                ctx.fill(12, y - 7, 193, y + 20, 0xFF28103F);
                ctx.fill(12, y - 7, 15, y + 20, PURPLE);
            }
            text(ctx, value.name(), 27, y, active ? WHITE : MUTED, true);
            text(ctx, Integer.toString(count(value)), 171, y, active ? PURPLE : MUTED, false);
            y += 34;
        }

        int enabled = DarknessClient.getModuleManager() == null ? 0 : DarknessClient.getModuleManager().enabledCount();
        panel(ctx, 12, height - 142, 181, 104);
        text(ctx, "RUNTIME", 24, height - 125, PURPLE, true);
        text(ctx, "Enabled", 24, height - 98, MUTED, false);
        text(ctx, Integer.toString(enabled), 126, height - 98, GREEN, true);
        text(ctx, "FPS", 24, height - 77, MUTED, false);
        text(ctx, Integer.toString(MinecraftClient.getInstance().getCurrentFps()), 126, height - 77, WHITE, false);
    }

    private int count(Module.Category value) {
        int n = 0;
        if (DarknessClient.getModuleManager() == null) return 0;
        for (Module module : DarknessClient.getModuleManager().getModules()) if (module.getCategory() == value) n++;
        return n;
    }

    private void drawModules(DrawContext ctx, int mouseX, int mouseY) {
        int x0 = 220;
        int y0 = 62 - scroll;
        int cardW = 245;
        int cardH = 82;
        int gap = 10;
        text(ctx, category.name(), x0, 45, PURPLE, true);

        for (int i = 0; i < visible.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            int x = x0 + col * (cardW + gap);
            int y = y0 + row * (cardH + gap);
            if (y + cardH < 50 || y > height - 40) continue;

            Module module = visible.get(i);
            boolean selectedCard = module == selected;
            panel(ctx, x, y, cardW, cardH, selectedCard ? 0xFF1D0D2B : PANEL);
            if (selectedCard) ctx.fill(x, y, x + 3, y + cardH, PURPLE);
            text(ctx, module.getName(), x + 15, y + 17, WHITE, true);
            text(ctx, module.isEnabled() ? "ENABLED" : "DISABLED", x + 15, y + 37, module.isEnabled() ? GREEN : MUTED, false);
            text(ctx, "settings: " + module.getSettings().size(), x + 15, y + 57, MUTED, false);
            drawToggle(ctx, x + cardW - 52, y + 13, module.isEnabled());
        }
    }

    private void drawInspector(DrawContext ctx) {
        int x = 730;
        int w = Math.max(230, width - x - 14);
        panel(ctx, x, 62, w, height - 101);
        text(ctx, "INSPECTOR", x + 15, 81, PURPLE, true);

        if (selected == null) {
            text(ctx, "No module selected", x + 15, 110, MUTED, false);
            return;
        }

        text(ctx, selected.getName(), x + 15, 107, WHITE, true);
        text(ctx, selected.getCategory().name(), x + 15, 124, MUTED, false);
        drawToggle(ctx, x + w - 65, 99, selected.isEnabled());

        int y = 154;
        for (Module.Setting<?> setting : selected.getSettings()) {
            if (y > height - 55) break;
            text(ctx, setting.getName(), x + 15, y, WHITE, false);
            text(ctx, String.valueOf(setting.getValue()), x + 15, y + 17, MUTED, false);
            ctx.fill(x + 15, y + 36, x + w - 15, y + 38, 0xFF33263D);
            ctx.fill(x + 15, y + 36, x + (w - 15) / 2, y + 38, PURPLE);
            y += 54;
        }
    }

    private void drawFooter(DrawContext ctx) {
        ctx.fill(0, height - 24, width, height, 0xFF0B0911);
        text(ctx, "RMB: inspect    LMB: toggle    ESC: close    BACKSPACE: search", 18, height - 16, MUTED, false);
        text(ctx, "DARKNESS PRESTIGE", width - 150, height - 16, PURPLE, true);
    }

    private void drawToggle(DrawContext ctx, int x, int y, boolean on) {
        ctx.fill(x, y, x + 39, y + 19, on ? PURPLE_DARK : 0xFF29222F);
        ctx.fill(on ? x + 22 : x + 4, y + 4, on ? x + 35 : x + 17, y + 16, on ? PURPLE : MUTED);
    }

    private void panel(DrawContext ctx, int x, int y, int w, int h) { panel(ctx, x, y, w, h, PANEL); }
    private void panel(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
        ctx.fill(x, y, x + w, y + 1, BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER);
    }

    private void text(DrawContext ctx, String value, int x, int y, int color, boolean shadow) {
        ctx.drawText(textRenderer, Text.literal(value), x, y, color, shadow);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX < 205 && mouseY >= 70) {
                int index = (int) ((mouseY - 70) / 34);
                Module.Category[] categories = Module.Category.values();
                if (index >= 0 && index < categories.length) {
                    category = categories[index];
                    scroll = 0;
                    rebuildVisible();
                    return true;
                }
            }

            int x0 = 220;
            int y0 = 62 - scroll;
            int cardW = 245;
            int cardH = 82;
            int gap = 10;
            for (int i = 0; i < visible.size(); i++) {
                int col = i % 2;
                int row = i / 2;
                int x = x0 + col * (cardW + gap);
                int y = y0 + row * (cardH + gap);
                if (inside(mouseX, mouseY, x, y, cardW, cardH)) {
                    selected = visible.get(i);
                    selected.toggle();
                    return true;
                }
            }
        }
        if (button == 1 && selected != null) {
            int x = 730;
            int w = Math.max(230, width - x - 14);
            if (inside(mouseX, mouseY, x + w - 90, 90, 80, 35)) {
                selected.toggle();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { close(); return true; }
        if (keyCode == 259 && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            rebuildVisible();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (Character.isLetterOrDigit(chr) || chr == ' ' || chr == '-') {
            search += chr;
            rebuildVisible();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = MathHelper.clamp(scroll - (int) (verticalAmount * 25), 0, Math.max(0, visible.size() * 45));
        return true;
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean shouldPause() { return false; }
}
