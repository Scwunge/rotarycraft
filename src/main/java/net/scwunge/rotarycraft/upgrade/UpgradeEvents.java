package net.scwunge.rotarycraft.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.item.EngineUpgradeItem;
import net.scwunge.rotarycraft.item.GearUpgradeItem;
import net.scwunge.rotarycraft.registry.RotaryBlocks;

/**
 * Using an upgrade item on the machine it fits: the performance upgrade turns a Gas Engine into a Performance Engine (its facing and what it holds kept), the
 * others go into machines that take them ({@link Upgradable}), and an integrated gearbox (full, and not running) into an engine ({@link Geared}). The item is used up
 * unless the player is in creative.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public final class UpgradeEvents {
    private UpgradeEvents() {}

    @SubscribeEvent
    public static void use(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof EngineUpgradeItem) && !(stack.getItem() instanceof GearUpgradeItem)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        boolean performance = stack.getItem() instanceof EngineUpgradeItem up && up.kind() == EngineUpgradeItem.Kind.PERFORMANCE && state.is(RotaryBlocks.GAS_ENGINE.get());
        boolean fits = performance || (be instanceof Upgradable u && u.canUpgradeWith(stack))
                || (stack.getItem() instanceof GearUpgradeItem && be instanceof Geared);
        if (!fits) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (level.isClientSide()) {
            return;
        }
        boolean done = false;
        if (performance) {
            done = swapForPerformance(level, pos, state, be);
        } else if (be instanceof Upgradable u && u.canUpgradeWith(stack)) {
            u.upgradeWith(stack);
            done = true;
        } else if (be instanceof Geared geared) {
            int ratio = GearUpgradeItem.ratioOf(stack);
            done = ratio != 0 && geared.applyIntegratedGear(ratio);
        }
        if (done && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private static boolean swapForPerformance(Level level, BlockPos pos, BlockState state, BlockEntity old) {
        CompoundTag tag = old == null ? new CompoundTag() : old.saveWithoutMetadata(level.registryAccess());
        tag.remove("items"); // the slots are spilled as the old engine goes
        level.setBlock(pos, RotaryBlocks.PERFORMANCE_ENGINE.get().defaultBlockState().setValue(MachineBlock.FACING, state.getValue(MachineBlock.FACING)), 3);
        BlockEntity engine = level.getBlockEntity(pos);
        if (engine != null) {
            engine.loadWithComponents(tag, level.registryAccess());
            engine.setChanged();
        }
        return true;
    }
}
