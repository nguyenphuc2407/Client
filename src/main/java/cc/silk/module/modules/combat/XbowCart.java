package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.ItemUseEvent;
import cc.silk.event.impl.render.Render2DEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.ArrayList;
import java.util.List;

public final class XbowCart extends Module {

    private final BooleanSetting manualMode = new BooleanSetting("Manual", false);
    private final NumberSetting manualDelay = new NumberSetting("Manual Delay", 0, 10, 1, 1);
    private final ModeSetting firstAction = new ModeSetting("First", "Fire", "Fire", "Rail", "None");
    private final ModeSetting secondAction = new ModeSetting("Second", "Rail", "Fire", "Rail", "None");
    private final ModeSetting thirdAction = new ModeSetting("Third", "None", "Fire", "Rail", "None");
    private final NumberSetting delay = new NumberSetting("Delay", 0, 10, 2, 1);

    private int manualStep = 0;
    private int manualTickDelay = 0;
    private boolean active = false;

    private final List<String> sequence = new ArrayList<>();
    private int tickCounter = 0;
    private int actionIndex = 0;

    public XbowCart() {
        super("Xbow cart", "Customizable cart placement module", -1, Category.COMBAT);
        this.addSettings(manualMode, manualDelay, firstAction, secondAction, thirdAction, delay);
    }

    @Override
    public void onEnable() {
        if (isNull()) {
            setEnabled(false);
            return;
        }

        active = true;
        manualStep = 0;
        manualTickDelay = 0;
        tickCounter = 0;
        actionIndex = 0;

        sequence.clear();
        if (!firstAction.isMode("None")) sequence.add(firstAction.getMode());
        if (!secondAction.isMode("None")) sequence.add(secondAction.getMode());
        if (!thirdAction.isMode("None")) sequence.add(thirdAction.getMode());
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!active || isNull()) return;

        if (manualMode.getValue()) {
            handleManual();
        } else {
            handleAuto();
        }
    }

    @EventHandler
    private void onItemUse(ItemUseEvent event) {
        if (!manualMode.getValue() || !active) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return;

        Item item = stack.getItem();

        if (manualStep == 0 && isRailItem(item)) {
            manualStep = 1;
            manualTickDelay = manualDelay.getValueInt();
        }
    }

    private void handleManual() {
        if (manualTickDelay > 0) {
            manualTickDelay--;
            return;
        }

        switch (manualStep) {

            // Step 1: đặt TNT cart ngay sau rail
            case 1:
                if (isLookingAtRail() && switchToItem(Items.TNT_MINECART)) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                    manualStep = 2;
                    manualTickDelay = manualDelay.getValueInt();
                } else {
                    manualStep = 0;
                }
                break;

            // Step 2: switch sang flint
            case 2:
                if (switchToItem(Items.FLINT_AND_STEEL)) {
                    manualStep = 3;
                } else {
                    manualStep = 0;
                }
                break;

            // Step 3: ignite khi click block không phải rail
            case 3:
                if (mc.crosshairTarget instanceof BlockHitResult hit) {
                    BlockState state = mc.world.getBlockState(hit.getBlockPos());
                    if (!isRailBlock(state.getBlock())) {
                        ((MinecraftClientAccessor) mc).invokeDoItemUse();
                        switchToItem(Items.CROSSBOW);
                        manualStep = 0;
                    }
                }
                break;
        }
    }

    private void handleAuto() {
        if (actionIndex < sequence.size()) {

            if (tickCounter == 0) {
                executeAction(sequence.get(actionIndex));
            }

            tickCounter++;

            if (tickCounter > delay.getValueInt()) {
                tickCounter = 0;
                actionIndex++;
            }

        } else {
            switchToItem(Items.CROSSBOW);
            active = false;
        }
    }

    private void executeAction(String action) {
        if (action.equals("Fire")) {
            if (switchToItem(Items.FLINT_AND_STEEL)) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
            }
        }

        if (action.equals("Rail")) {

            boolean placedRail =
                    switchToItem(Items.RAIL) ||
                    switchToItem(Items.POWERED_RAIL) ||
                    switchToItem(Items.DETECTOR_RAIL) ||
                    switchToItem(Items.ACTIVATOR_RAIL);

            if (placedRail) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
            }

            if (switchToItem(Items.TNT_MINECART)) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
            }
        }
    }

    private boolean isRailItem(Item item) {
        return item == Items.RAIL ||
               item == Items.POWERED_RAIL ||
               item == Items.DETECTOR_RAIL ||
               item == Items.ACTIVATOR_RAIL;
    }

    private boolean isRailBlock(Block block) {
        return block == Blocks.RAIL ||
               block == Blocks.POWERED_RAIL ||
               block == Blocks.DETECTOR_RAIL ||
               block == Blocks.ACTIVATOR_RAIL;
    }

    private boolean isLookingAtRail() {
        if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return false;
        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        return isRailBlock(state.getBlock());
    }

    private boolean switchToItem(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }
}
