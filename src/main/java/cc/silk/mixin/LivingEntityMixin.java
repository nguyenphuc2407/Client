package cc.silk.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    // Đã xoá toàn bộ SwingSpeed hook.
    // Không inject vào getHandSwingDuration nữa.
    // Mixin trống để tránh lỗi compile và không gây crash.
}
