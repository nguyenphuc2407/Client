package cc.silk.module.modules.combat;

import cc.silk.event.impl.input.HandleInputEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public final class AutoCart extends Module {

    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);

    private boolean wasUsingBow = false;

    public AutoCart() {
        super("Auto Cart", "Places TNT cart when releasing bow and looking at rail", -1, Category.COMBAT);
        addSettings(autoSwitch);
    }

    @EventHandler
    private void onTick(HandleInputEvent event) {
        if (isNull()) return;

        boolean usingBow = mc.player.isUsingItem() &&
                mc.player.getActiveItem().getItem() == Items.BOW;

        // Khi đang giữ chuột để nạp cung
        if (usingBow) {
            wasUsingBow = true;
            return;
        }

        // Khi vừa thả chuột (nhả cung)
        if (wasUsingBow && !usingBow) {
            wasUsingBow = false;

            // Kiểm tra xem có nhìn thấy rail không
            if (isLookingAtRail()) {
                placeTntCart();
            }
        }
    }

    private boolean isLookingAtRail() {
        if (mc.crosshairTarget == null) return false;
        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return false;

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        Block block = mc.world.getBlockState(hit.getBlockPos()).getBlock();

        return block == Blocks.RAIL ||
               block == Blocks.POWERED_RAIL ||
               block == Blocks.DETECTOR_RAIL ||
               block == Blocks.ACTIVATOR_RAIL;
    }

    private void placeTntCart() {
        int tntSlot = findTntCart();
        if (tntSlot == -1) return;

        int oldSlot = mc.player.getInventory().selectedSlot;

        // Auto switch
        if (autoSwitch.getValue())
            mc.player.getInventory().selectedSlot = tntSlot;

        // Đặt TNT cart
        ((MinecraftClientAccessor) mc).invokeDoItemUse();

        // Trả slot cũ
        if (autoSwitch.getValue())
            mc.player.getInventory().selectedSlot = oldSlot;
    }

    private int findTntCart() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.TNT_MINECART)
                return i;
        }
        return -1;
    }
}
