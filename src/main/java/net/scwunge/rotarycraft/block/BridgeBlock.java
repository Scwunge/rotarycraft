package net.scwunge.rotarycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A piece of the walkable beam a Light Bridge makes: a thin plate near the bottom of its block (as the original's bounds, 1/16 to about 2.4/16) to walk
 * on, see-through, glowing, and gone when the bridge loses power. It points along the bridge.
 */
public class BridgeBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create("axis", Direction.Axis.class, Direction.Axis.X, Direction.Axis.Z);

    private static final VoxelShape PLATE = Block.box(0, 1, 0, 16, 2.428, 16);

    public BridgeBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).noOcclusion().strength(-1F, 3_600_000F).lightLevel(s -> 8).pushReaction(PushReaction.BLOCK)
                .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false).forceSolidOn());
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PLATE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1F;
    }
}
