package cc.silk.module.modules.combat;

import cc.silk.event.impl.player.ItemUseEvent;
import cc.silk.event.impl.player.TickEvent;
import cc.silk.mixin.MinecraftClientAccessor;
import cc.silk.module.Category;
import cc.silk.module.Module;
import cc.silk.module.setting.BooleanSetting;
import cc.silk.module.setting.ModeSetting;
import cc.silk.module.setting.NumberSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.block.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class XbowCart extends Module {

    private final BooleanSetting manualMode = new BooleanSetting("Manual", false);
    private final NumberSetting manualDelay = new NumberSetting("Manual Delay", 0, 10, 1, 1);
    private final ModeSetting firstAction = new ModeSetting("First", "Fire", "Fire", "Rail", "None");
    private final ModeSetting secondAction = new ModeSetting("Second", "Rail", "Fire", "Rail", "None");
    private final ModeSetting thirdAction = new ModeSetting("Third", "None", "Fire", "Rail", "None");
    private final NumberSetting delay = new NumberSetting("Delay", 0, 10, 2, 1);

    private boolean active = false;
    private int tickCounter = 0;
    private int actionIndex = 0;

    private final List<String> sequence = new ArrayList<>();

    // AUTO Rail → TNT logic
    private int railPhase = 0;   // 0 = place rail, 1 = delay, 2 = TNT
    private int railDelay = 0;   // countdown

    // Manual mode
    private int manualStep = 0;
    private boolean shouldSwitch = false;
    private boolean shouldExecute = false;
    private int executeDelay = 0;

    public XbowCart() {
        super("Xbow Cart", "Rail → TNT Cart automation", -1, Category.COMBAT);
        this.addSettings(manualMode, manualDelay, firstAction, secondAction, thirdAction, delay);
    }

    @Override
    public void onEnable() {
        if (isNull()) {
            setEnabled(false);
            return;
        }

        active = true;
        tickCounter = 0;
        actionIndex = 0;

        railPhase = 0;
        railDelay = 0;

        if (!manualMode.getValue()) {
            sequence.clear();
            if (!firstAction.isMode("None")) sequence.add(firstAction.getMode());
            if (!secondAction.isMode("None")) sequence.add(secondAction.getMode());
            if (!thirdAction.isMode("None")) sequence.add(thirdAction.getMode());
        } else {
            manualStep = 0;
            executeDelay = 0;
            shouldExecute = false;
            shouldSwitch = false;
        }
    }

    /* ===========================================================
       =====================  AUTO MODE  ==========================
       =========================================================== */

    @EventHandler
    private void onTick(TickEvent event) {
        if (!active || isNull()) return;

        if (manualMode.getValue()) {
            handleManualTick();
        } else {
            handleAutoTick();
        }
    }

    private void handleAutoTick() {
        if (actionIndex >= sequence.size()) {
            switchToItem(Items.CROSSBOW);
            active = false;
            return;
        }

        String action = sequence.get(actionIndex);

        // FIX CHÍNH: luôn chạy mỗi tick
        executeAction(action);

        tickCounter++;
        if (tickCounter > delay.getValueInt()) {
            tickCounter = 0;
            actionIndex++;
        }
    }

    /* ===========================================================
       ==================  EXECUTE ACTION  ========================
       =========================================================== */

    private void executeAction(String action) {

        /* ---------- FIRE ACTION ---------- */
        if (action.equals("Fire")) {
            if (switchToItem(Items.FLINT_AND_STEEL)) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
            }
            return;
        }

        /* ---------- RAIL → 2 TICK → TNT CART ---------- */
        if (action.equals("Rail")) {

            // STEP 1 — place rail
            if (railPhase == 0) {
                if (switchToItem(Items.RAIL) ||
                    switchToItem(Items.POWERED_RAIL) ||
                    switchToItem(Items.DETECTOR_RAIL) ||
                    switchToItem(Items.ACTIVATOR_RAIL)) {

                    ((MinecraftClientAccessor) mc).invokeDoItemUse();

                    railPhase = 1;
                    railDelay = 2;  // fixed 2 tick delay for TNT
                }
                return;
            }

            // STEP 2 — wait ticks
            if (railPhase == 1) {
                if (railDelay > 0) {
                    railDelay--;
                    return;
                }
                railPhase = 2;
            }

            // STEP 3 — place TNT minecart
            if (railPhase == 2) {
                if (switchToItem(Items.TNT_MINECART)) {
                    ((MinecraftClientAccessor) mc).invokeDoItemUse();
                }
                railPhase = 0; // reset
            }
        }
    }

    /* ===========================================================
       =====================  MANUAL MODE  ========================
       =========================================================== */

    @EventHandler
    private void onItemUse(ItemUseEvent event) {
        if (!manualMode.getValue() || !active || isNull()) return;

        ItemStack stack = mc.player.getMainHandStack();
        if (stack.isEmpty()) return;

        switch (manualStep) {
            case 0:
                if (isRailItem(stack.getItem())) {
                    manualStep = 1;
                    shouldSwitch = true;
                }
                break;
            case 1:
                if (stack.getItem() == Items.TNT_MINECART) {
                    manualStep = 2;
                    shouldSwitch = true;
                }
                break;
            case 2:
                if (stack.getItem() == Items.FLINT_AND_STEEL) {
                    manualStep = 3;
                    shouldSwitch = true;
                }
                break;
            case 3:
                if (isRailItem(stack.getItem())) {
                    manualStep = 1;
                    shouldSwitch = true;
                }
                break;
        }
    }

    private void handleManualTick() {
        if (executeDelay > 0) {
            executeDelay--;
            return;
        }

        if (shouldSwitch) {
            shouldSwitch = false;
            executeDelay = manualDelay.getValueInt();

            switch (manualStep) {
                case 1:
                    switchToItem(Items.TNT_MINECART);
                    shouldExecute = true;
                    break;
                case 2:
                    switchToItem(Items.FLINT_AND_STEEL);
                    shouldExecute = true;
                    break;
                case 3:
                    switchToItem(Items.CROSSBOW);
                    manualStep = 0;
                    break;
            }
            return;
        }

        if (shouldExecute) {
            shouldExecute = false;
            ((MinecraftClientAccessor) mc).invokeDoItemUse();
        }

        if (manualStep == 2 && mc.player.getVehicle() == null) {
            ItemStack stack = mc.player.getMainHandStack();

            if (!stack.isEmpty() && stack.getItem() == Items.FLINT_AND_STEEL) {
                HitResult hit = mc.crosshairTarget;

                if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) hit;
                    BlockState state = mc.world.getBlockState(bhr.getBlockPos());

                    if (!isRailBlock(state.getBlock())) {
                        ((MinecraftClientAccessor) mc).invokeDoItemUse();
                        manualStep = 3;
                        shouldSwitch = true;
                    }
                }
            }
        }
    }

    /* ===========================================================
       ======================== UTILS =============================
       =========================================================== */

    private boolean isRailBlock(net.minecraft.block.Block block) {
        return block == net.minecraft.block.Blocks.RAIL ||
               block == net.minecraft.block.Blocks.POWERED_RAIL ||
               block == net.minecraft.block.Blocks.DETECTOR_RAIL ||
               block == net.minecraft.block.Blocks.ACTIVATOR_RAIL;
    }

    private boolean isRailItem(net.minecraft.item.Item item) {
        return item == Items.RAIL ||
               item == Items.POWERED_RAIL ||
               item == Items.DETECTOR_RAIL ||
               item == Items.ACTIVATOR_RAIL;
    }

    private boolean switchToItem(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() == item) {
                mc.player.getInventory().selectedSlot = i;
                return true;
            }
        }
        return false;
    }
}
