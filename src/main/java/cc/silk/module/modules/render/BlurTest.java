package cc.silk.module.modules.render;

import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ColorSetting;
import cc.silk.module.setting.NumberSetting;
import cc.silk.utils.render.blur.BlurRenderer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;

public class BlurTest extends Module {
    private final NumberSetting blurRadius = new NumberSetting("Blur Radius", 1, 30, 12, 1);
    private final NumberSetting cornerRadius = new NumberSetting("Corner Radius", 0, 20, 8, 1);
    private final NumberSetting width = new NumberSetting("Width", 50, 400, 200, 1);
    private final NumberSetting height = new NumberSetting("Height", 50, 300, 100, 1);
    private final NumberSetting xPos = new NumberSetting("X Position", 0, 1000, 100, 1);
    private final NumberSetting yPos = new NumberSetting("Y Position", 0, 1000, 100, 1);
    private final BooleanSetting centered = new BooleanSetting("Centered", true);
    private final ColorSetting tintColor = new ColorSetting("Tint Color", Color.WHITE);

    public BlurTest() {
        super("BlurTest", "Tests the blur rendering system", Category.RENDER);
        addSettings(blurRadius, cornerRadius, width, height, xPos, yPos, centered, tintColor);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.world == null) return;

        DrawContext context = event.getContext();
        float w = (float) width.getValue();
        float h = (float) height.getValue();
        float x, y;

        if (centered.getValue()) {
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            x = (screenWidth - w) / 2f;
            y = (screenHeight - h) / 2f;
        } else {
            x = (float) xPos.getValue();
            y = (float) yPos.getValue();
        }

        BlurRenderer.drawBlur(
                context.getMatrices(),
                x, y, w, h,
                (float) cornerRadius.getValue(),
                tintColor.getValue(),
                (float) blurRadius.getValue()
        );
    }
}
