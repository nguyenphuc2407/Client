package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.event.impl.render.Render3DEvent;
import cc.silk.utils.render.W2SUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    // ⭐ DELETE toàn bộ AspectRatio → không sửa aspect ratio nữa
    // ⭐ Nếu bạn muốn thêm Custom FOV sau này, tôi có thể viết lại module mới.

    @Inject(
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
            opcode = Opcodes.GETFIELD,
            ordinal = 0
        ),
        method = "renderWorld"
    )
    private void renderHand(RenderTickCounter tickCounter, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer.getCamera();
        MatrixStack matrixStack = new MatrixStack();

        RenderSystem.getModelViewStack().pushMatrix().mul(matrixStack.peek().getPositionMatrix());
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        W2SUtil.matrixProject.set(RenderSystem.getProjectionMatrix());
        W2SUtil.matrixModel.set(RenderSystem.getModelViewMatrix());
        W2SUtil.matrixWorldSpace.set(matrixStack.peek().getPositionMatrix());

        Camera blockCamera = mc.getBlockEntityRenderDispatcher().camera;
        if (blockCamera != null) {
            matrixStack.push();
            Vec3d vec3d = blockCamera.getPos();
            matrixStack.translate(-vec3d.x, -vec3d.y, -vec3d.z);
        }

        SilkClient.INSTANCE.getSilkEventBus().post(new Render3DEvent(matrixStack));

        RenderSystem.getModelViewStack().popMatrix();
    }
}
