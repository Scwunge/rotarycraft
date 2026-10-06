package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Cave Scanner's block: using it moves the point it is aimed at four blocks the way you look (sneaking, the other way). */
public class CaveScannerBlock extends MachineBlock {
    public CaveScannerBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                            BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CaveScannerBlockEntity scanner) {
            if (!level.isClientSide()) {
                Direction look = Direction.orderedByNearest(player)[0];
                scanner.moveSource(player.isShiftKeyDown() ? -CaveScannerBlockEntity.STEP : CaveScannerBlockEntity.STEP, look);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
