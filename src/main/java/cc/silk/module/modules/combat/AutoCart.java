package cc.silk.module.modules.combat;

import cc.silk.event.impl.input.HandleInputEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class AutoCart extends Module {

    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", true);

    private boolean isActive = false;
    private BlockPos targetPos = null;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private int placeDelay = 0;

    private boolean hasRail = false;
    private boolean hasTntCart = false;

    public AutoCart() {
        super("Auto Cart", "Places TNT minecarts on rails when shooting arrows", -1, Category.COMBAT);
        this.addSettings(autoSwitch);
    }

    @EventHandler
    private void onTickEvent(HandleInputEvent event) {
        if (isNull()) return;

        // Khi đang kéo cung
        if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() == Items.BOW) {
            if (!isActive) {
                startPlacing();
            }
        }

        if (!isActive) return;

        tickCounter++;

        // Tick 1 → đặt rail
        if (tickCounter == 1) {
            placeRail();
        }

        // Tick 3–5 → đặt TNT cart
        if (tickCounter == placeDelay) {
            placeTntCart();
            stopPlacing();
        }
    }

    private void startPlacing() {
        if (isActive) return;

        if (mc.player.getMainHandStack().getItem() != Items.BOW) return;

        this.targetPos = getTargetPosition();
        if (this.targetPos == null) return;

        if (!mc.world.getBlockState(targetPos).isReplaceable()) return;

        isActive = true;
        tickCounter = 0;
        placeDelay = 3 + mc.player.getRandom().nextInt(3); // 3–5 tick
        originalSlot = mc.player.getInventory().selectedSlot;

        hasRail = false;
        hasTntCart = false;
    }

    private void stopPlacing() {
        if (!isActive) return;

        if (autoSwitch.getValue() && originalSlot != -1) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }

        resetState();
    }

    private void resetState() {
        isActive = false;
        targetPos = null;
        originalSlot = -1;
        tickCounter = 0;
        placeDelay = 0;
        hasRail = false;
        hasTntCart = false;
    }

    private void placeRail() {
        if (hasRail || targetPos == null) return;

        int railSlot = findAnyRailInHotbar();
        if (railSlot == -1) return;

        mc.player.getInventory().selectedSlot = railSlot;

        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(targetPos),
                mc.player.getHorizontalFacing(),
                targetPos,
                false
        );

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hitResult);
        hasRail = true;
    }

    private void placeTntCart() {
        if (hasTntCart || targetPos == null) return;

        int tntCartSlot = findTntCartInHotbar();
        if (tntCartSlot == -1) return;

        mc.player.getInventory().selectedSlot = tntCartSlot;

        BlockHitResult hitResult = new BlockHitResult(
                Vec3d.ofCenter(targetPos),
                mc.player.getHorizontalFacing(),
                targetPos,
                false
        );

        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, hitResult);
        hasTntCart = true;
    }

    private BlockPos getTargetPosition() {
        HitResult hitResult = mc.crosshairTarget;
        if (hitResult == null) return null;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            return blockHit.getBlockPos().offset(blockHit.getSide());
        }

        return mc.player.getBlockPos().up();
    }

    private int findTntCartInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.TNT_MINECART) {
                return i;
            }
        }
        return -1;
    }

    private int findAnyRailInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && isRail(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isRail(Item item) {
        return item == Items.RAIL ||
               item == Items.POWERED_RAIL ||
               item == Items.DETECTOR_RAIL ||
               item == Items.ACTIVATOR_RAIL;
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        if (isActive) stopPlacing();
    }
}
