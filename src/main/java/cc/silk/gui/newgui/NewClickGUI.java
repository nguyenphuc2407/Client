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

        long currentTime
