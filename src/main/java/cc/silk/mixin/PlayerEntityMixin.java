package cc.silk.mixin;

import cc.silk.SilkClient;
import cc.silk.module.modules.movement.KeepSprint;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    // XÓA toàn bộ FastMine vì module đã bị Remove → không được gọi nữa.
    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void modifyBlockBreakingSpeed(BlockState block, CallbackInfoReturnable<Float> cir) {
        // Không làm gì để tránh lỗi.
    }

    // KeepSprint vẫn giữ lại vì còn tồn tại
    @Inject(method = "attack", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
    ), cancellable = true)
    private void attackInject(Entity target, CallbackInfo ci) {
        var keep = SilkClient.INSTANCE.getModuleManager().getModule(KeepSprint.class);
        if (keep.isPresent() && keep.get().isEnabled()) {
            ci.cancel();
        }
    }
}
