package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A directional shaft-power block. FACING is the output side; power comes in from the opposite side. When placed, the
 * block faces where the player is looking, so a line of shafts points away from you.
 */
public class MachineBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type;
    private final BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory;

    public MachineBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                        BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props);
        this.type = type;
        this.factory = factory;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // Block codecs are only used to serialize block types, which modded blocks don't need; keep a valid one anyway.
        return simpleCodec(p -> new MachineBlock(p, type, factory));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != type.get()) {
            return null;
        }
        return (l, p, s, be) -> ((PowerBlockEntity) be).serverTick();
    }
}
