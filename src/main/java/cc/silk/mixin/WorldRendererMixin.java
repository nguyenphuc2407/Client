package cc.silk.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow @Final private MinecraftClient client;
    @Shadow private Framebuffer entityOutlineFramebuffer;

    // XÓA toàn bộ CustomOutlineESP → không hook outline nữa.
    // Giữ nguyên file cho Minecraft không crash, không làm gì thêm.
}
