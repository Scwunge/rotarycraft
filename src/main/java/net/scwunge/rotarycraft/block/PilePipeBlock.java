package net.scwunge.rotarycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A length of the pile a Pile Driver drives down: a thin upright pipe in the middle of the block, that breaks at once and drops nothing. */
public class PilePipeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(5.28, 0, 5.28, 10.72, 16, 10.72);

    public PilePipeBlock() {
        super(BlockBehaviour.Properties.of().strength(0).noOcclusion().noLootTable().sound(SoundType.METAL).forceSolidOff());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
