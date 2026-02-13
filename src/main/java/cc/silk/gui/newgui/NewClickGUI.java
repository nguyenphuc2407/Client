package cc.silk.gui.newgui;

import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.gui.newgui.components.CategoryPanel;
import cc.silk.gui.newgui.components.ConfigPanel;
import cc.silk.gui.effects.SnowEffect;
import cc.silk.utils.render.nanovg.NanoVGRenderer;
import cc.silk.utils.render.GuiGlowHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NewClickGUI extends Screen {

    private final List<CategoryPanel> panels = new ArrayList<>();
    private ConfigPanel configPanel;
    private static final int PANEL_WIDTH = 110;
    private static final int PANEL_SPACING = 5;

    private float animationProgress = 0f;
    private boolean closing = false;
    private long lastFrameTime = System.currentTimeMillis();

    private final SnowEffect snowEffect = new SnowEffect();

    private String searchQuery = "";
    private boolean searchFocused = false;
    private float searchExpandProgress = 0f;
    private float searchBarY = 0;

    private static final int SEARCH_BAR_WIDTH = 300;
    private static final int SEARCH_ICON_SIZE = 35;
    private static final java.awt.Color SEARCH_BG = new java.awt.Color(28, 28, 32, 255);
    private static final java.awt.Color SEARCH_TEXT = new java.awt.Color(240, 240, 245, 255);
    private static final java.awt.Color SEARCH_PLACEHOLDER = new java.awt.Color(130, 130, 140, 255);

    public NewClickGUI() {
        super(Text.literal("ClickGUI"));
        initPanels();
    }

    private void initPanels() {
        NanoVGRenderer.init();

        int x = 20;
        int y = 20;

        for (Category category : Category.values()) {
            if (category == Category.CONFIG)
                continue;

            List<Module> modules = cc.silk.SilkClient.INSTANCE.getModuleManager().getModulesByCategory(category);
            if (!modules.isEmpty()) {
                panels.add(new CategoryPanel(category, x, y, PANEL_WIDTH));
                x += PANEL_WIDTH + PANEL_SPACING;
            }
        }

        configPanel = new ConfigPanel(x, y, PANEL_WIDTH);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastFrameTime) / 1000f;
        lastFrameTime = currentTime;

        if (closing) {
            animationProgress -= deltaTime * 4f;
            if (animationProgress <= 0f) {
                animationProgress = 0f;
                if (client != null) client.setScreen(null);
                return;
            }
        } else {
            animationProgress += deltaTime * 4f;
            if (animationProgress > 1f) animationProgress = 1f;
        }

        float scale = easeOutCubic(animationProgress);
        float alpha = animationProgress;
        float guiScale = getGuiScaleMultiplier();

        float targetExpand = (searchFocused || !searchQuery.isEmpty()) ? 1f : 0f;
        searchExpandProgress += (targetExpand - searchExpandProgress) * deltaTime * 8f;
        searchExpandProgress = Math.max(0f, Math.min(1f, searchExpandProgress));

        int centerX = width / 2;
        int centerY = height / 2;
        int transformedMouseX = (int) ((mouseX - centerX) / (scale * guiScale) + centerX);
        int transformedMouseY = (int) ((mouseY - centerY) / (scale * guiScale) + centerY);

        for (CategoryPanel panel : panels) {
            panel.update(deltaTime);
            panel.setSearchQuery(searchQuery);
        }

        if (configPanel != null) configPanel.update(deltaTime);

        CategoryPanel draggedPanel = null;
        for (CategoryPanel panel : panels) {
            if (panel.isDragging()) draggedPanel = panel;
        }

        boolean configDragging = configPanel != null && configPanel.isDragging();

        NanoVGRenderer.beginFrame();
        NanoVGRenderer.save();

        NanoVGRenderer.translate(centerX, centerY);
        NanoVGRenderer.scale(scale * guiScale, scale * guiScale);
        NanoVGRenderer.translate(-centerX, -centerY);

        renderSearchBar(alpha);

        for (CategoryPanel panel : panels) {
            if (panel != draggedPanel) panel.render(transformedMouseX, transformedMouseY, alpha, scale, centerX, centerY);
        }

        if (configPanel != null && !configDragging)
            configPanel.render(transformedMouseX, transformedMouseY, alpha, scale, centerX, centerY);

        if (draggedPanel != null)
            draggedPanel.render(transformedMouseX, transformedMouseY, alpha, scale, centerX, centerY);

        if (configPanel != null && configDragging)
            configPanel.render(transformedMouseX, transformedMouseY, alpha, scale, centerX, centerY);

        for (CategoryPanel panel : panels)
            panel.renderSettingsPanel(transformedMouseX, transformedMouseY, alpha, width, height);

        NanoVGRenderer.restore();
        NanoVGRenderer.endFrame();
    }

    private void renderSearchBar(float alpha) {
        searchBarY = height - SEARCH_ICON_SIZE - 20;

        float currentWidth = SEARCH_ICON_SIZE +
                (SEARCH_BAR_WIDTH - SEARCH_ICON_SIZE) * easeOutCubic(searchExpandProgress);

        float searchBarX = (width - currentWidth) / 2f;
        int bgAlpha = (int) (255 * alpha);

        Color bgColor = new Color(SEARCH_BG.getRed(), SEARCH_BG.getGreen(), SEARCH_BG.getBlue(), bgAlpha);

        Color accentColor = cc.silk.module.modules.client.NewClickGUIModule.getAccentColor();
        Color borderColor = new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(),
                searchFocused ? bgAlpha : (int) (100 * alpha));

        NanoVGRenderer.drawRoundedRect(searchBarX, searchBarY, currentWidth, SEARCH_ICON_SIZE, 6f, bgColor);

        NanoVGRenderer.drawRoundedRectOutline(searchBarX, searchBarY, currentWidth,
                SEARCH_ICON_SIZE, 6f, searchFocused ? 2f : 1f, borderColor);

        float iconCenterX = searchBarX + SEARCH_ICON_SIZE / 2f;
        float iconCenterY = searchBarY + SEARCH_ICON_SIZE / 2f;

        Color iconColor = new Color(SEARCH_TEXT.getRed(), SEARCH_TEXT.getGreen(),
                SEARCH_TEXT.getBlue(), bgAlpha);

        float circleRadius = 5f;
        NanoVGRenderer.drawCircle(iconCenterX - 1, iconCenterY - 1, circleRadius + 0.75f, iconColor);

        Color bgCircle = new Color(SEARCH_BG.getRed(), SEARCH_BG.getGreen(), SEARCH_BG.getBlue(), bgAlpha);
        NanoVGRenderer.drawCircle(iconCenterX - 1, iconCenterY - 1, circleRadius - 0.75f, bgCircle);

        float handleAngle = (float) Math.toRadians(45);
        float handleStartX = iconCenterX - 1 + circleRadius * (float) Math.cos(handleAngle);
        float handleStartY = iconCenterY - 1 + circleRadius * (float) Math.sin(handleAngle);

        NanoVGRenderer.drawLine(handleStartX, handleStartY,
                handleStartX + 5 * (float) Math.cos(handleAngle),
                handleStartY + 5 * (float) Math.sin(handleAngle),
                1.5f, iconColor);

        if (searchExpandProgress > 0.3f) {
            String displayText = searchQuery.isEmpty() ? "Search modules..." : searchQuery;

            Color textColor = searchQuery.isEmpty()
                    ? new Color(SEARCH_PLACEHOLDER.getRed(), SEARCH_PLACEHOLDER.getGreen(), SEARCH_PLACEHOLDER.getBlue(),
                    (int) (bgAlpha * searchExpandProgress))
                    : new Color(SEARCH_TEXT.getRed(), SEARCH_TEXT.getGreen(), SEARCH_TEXT.getBlue(),
                    (int) (bgAlpha * searchExpandProgress));

            float fontSize = 12f;
            float textX = searchBarX + SEARCH_ICON_SIZE + 5;

            NanoVGRenderer.drawText(displayText, textX,
                    searchBarY + (SEARCH_ICON_SIZE - fontSize) / 2f, fontSize, textColor);
        }
    }

    private float easeOutCubic(float t) {
        return 1f - (float) Math.pow(1f - t, 3);
    }

    @Override
    public void close() {
        for (CategoryPanel panel : panels) panel.saveState();
        closing = true;
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        float guiScale = getGuiScaleMultiplier();
        float scale = easeOutCubic(animationProgress);

        int centerX = width / 2;
        int centerY = height / 2;

        double mx = (mouseX - centerX) / (scale * guiScale) + centerX;
        double my = (mouseY - centerY) / (scale * guiScale) + centerY;

        for (CategoryPanel panel : panels) {
            if (panel.hasActiveSettingsPanel() &&
                panel.getSettingsPanel().mouseClicked(mx, my, button)) {

                searchFocused = false;
                return true;
            }
        }

        if (configPanel != null && configPanel.mouseClicked(mx, my, button)) {
            searchFocused = false;
            return true;
        }

        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(mx, my, button)) {
                searchFocused = false;
                return true;
            }
        }

        float currentWidth = SEARCH_ICON_SIZE +
                (SEARCH_BAR_WIDTH - SEARCH_ICON_SIZE) * easeOutCubic(searchExpandProgress);

        float searchBarX = (width - currentWidth) / 2f;

        if (mx >= searchBarX && mx <= searchBarX + currentWidth &&
                my >= searchBarY && my <= searchBarY + SEARCH_ICON_SIZE) {

            searchFocused = true;
            return true;
        }

        searchFocused = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (configPanel != null && configPanel.charTyped(chr, modifiers)) return true;

        for (CategoryPanel panel : panels) {
            if (panel.hasActiveSettingsPanel() &&
                panel.getSettingsPanel().charTyped(chr, modifiers)) {
                return true;
            }
        }

        if (searchFocused) {
            searchQuery += chr;
            return true;
        }

        if (cc.silk.module.modules.client.ClientSettingsModule.isAutoFocusSearchEnabled()) {
            boolean anyActive = false;

            for (CategoryPanel panel : panels) {
                if (panel.hasActiveSettingsPanel()) {
                    anyActive = true;
                    break;
                }
            }

            if (!anyActive && Character.isLetterOrDigit(chr)) {
                searchFocused = true;
                searchQuery += chr;
                return true;
            }
        }

        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (configPanel != null && configPanel.keyPressed(keyCode, scanCode, modifiers))
            return true;

        for (CategoryPanel panel : panels) {
            if (panel.keyPressed(keyCode, scanCode, modifiers)) return true;
        }

        if (searchFocused) {
            if (keyCode == 259) {
                if (!searchQuery.isEmpty())
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                searchFocused = false;
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        float guiScale = getGuiScaleMultiplier();
        float scale = easeOutCubic(animationProgress);

        int centerX = width / 2;
        int centerY = height / 2;

        double mx = (mouseX - centerX) / (scale * guiScale) + centerX;
        double my = (mouseY - centerY) / (scale * guiScale) + centerY;

        if (configPanel != null)
            configPanel.mouseReleased(mx, my, button);

        for (CategoryPanel panel : panels)
            panel.mouseReleased(mx, my, button);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {

        float guiScale = getGuiScaleMultiplier();
        float scale = easeOutCubic(animationProgress);

        int centerX = width / 2;
        int centerY = height / 2;

        double mx = (mouseX - centerX) / (scale * guiScale) + centerX;
        double my = (mouseY - centerY) / (scale * guiScale) + centerY;

        if (configPanel != null && configPanel.mouseDragged(mx, my, button, dx, dy))
            return true;

        for (CategoryPanel panel : panels)
            if (panel.mouseDragged(mx, my, button, dx, dy)) return true;

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double h, double v) {

        float guiScale = getGuiScaleMultiplier();
        float scale = easeOutCubic(animationProgress);

        int centerX = width / 2;
        int centerY = height / 2;

        double mx = (mouseX - centerX) / (scale * guiScale) + centerX;
        double my = (mouseY - centerY) / (scale * guiScale) + centerY;

        if (configPanel != null && configPanel.mouseScrolled(mx, my, v))
            return true;

        for (CategoryPanel panel : panels)
            if (panel.mouseScrolled(mx, my, v)) return true;

        return super.mouseScrolled(mouseX, mouseY, h, v);
    }

    private float getGuiScaleMultiplier() {
        int scale = cc.silk.module.modules.client.ClientSettingsModule.getGuiScale();

        if (scale == 0) return 0.75f;
        if (scale == 1) return 1.0f;
        if (scale == 2) return 1.25f;
        if (scale == 3) return 1.5f;

        return 1.0f;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int x, int y, float d) {
        if (cc.silk.module.modules.client.ClientSettingsModule.isGuiBlurEnabled())
            super.renderBackground(ctx, x, y, d);
    }
}
