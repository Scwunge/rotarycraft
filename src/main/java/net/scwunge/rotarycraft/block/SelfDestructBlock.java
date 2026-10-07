package net.scwunge.rotarycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.blockentity.SelfDestructBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMachineBlock;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Self Destruct block: it shrugs off the blasts of its own count-down (the original put itself back after each), so it lives to the last one. */
public class SelfDestructBlock extends LayoutMachineBlock {
    public SelfDestructBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                             BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        if (level.getBlockEntity(pos) instanceof SelfDestructBlockEntity bomb && bomb.isCounting()) {
            return;
        }
        super.onBlockExploded(state, level, pos, explosion);
    }
}
