package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.scwunge.rotarycraft.blockentity.BevelGearBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Bevel gears: power enters on INPUT and leaves on FACING. When placed, the output faces where you look and the input faces
 * the block you placed it against (or the opposite of the output if that would be the same side).
 * Screwdriver: right-click cycles the output, sneak-right-click cycles the input.
 */
public class BevelGearBlock extends MachineBlock {
    public static final DirectionProperty INPUT = DirectionProperty.create("input");

    public BevelGearBlock(Properties props) {
        super(props, RotaryBlockEntities.BEVEL_GEAR, BevelGearBlockEntity::new);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(INPUT, Direction.SOUTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(BevelGearBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(INPUT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction out = ctx.getNearestLookingDirection();
        Direction in = ctx.getClickedFace().getOpposite();
        if (in == out) {
            in = out.getOpposite();
        }
        return defaultBlockState().setValue(FACING, out).setValue(INPUT, in);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING))).setValue(INPUT, rotation.rotate(state.getValue(INPUT)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(INPUT, mirror.mirror(state.getValue(INPUT)));
    }
}
