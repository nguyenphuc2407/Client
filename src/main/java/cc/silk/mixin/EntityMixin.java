package cc.silk.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract BlockPos getLandingPos();
    @Shadow public abstract boolean isOnGround();
    @Shadow public abstract World getWorld();
    @Shadow protected abstract void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition);
    @Shadow public abstract EntityDimensions getDimensions(net.minecraft.entity.EntityPose pose);

    // XOÁ OutlineESP hook
    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        // Không làm gì (xoá ESP hoàn toàn)
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"))
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        // để trống
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isRemoved()Z"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (getWorld().isClient) {
            BlockPos blockPos = getLandingPos();
            BlockState blockState = getWorld().getBlockState(blockPos);
            fall(movement.y, isOnGround(), blockState, blockPos);
        }
    }

    // XOÁ HitboxHelper + Hitboxes expansion
    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<Box> cir) {
        // Không chỉnh hitbox nữa
    }
}
