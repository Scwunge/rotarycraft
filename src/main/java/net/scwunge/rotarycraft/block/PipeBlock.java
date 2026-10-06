package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.scwunge.rotarycraft.blockentity.PipeBlockEntity;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

import java.util.EnumMap;
import java.util.Map;

/** A pipe of one kind. It joins pipes it can work with and any block that holds fluid. */
public class PipeBlock extends BaseEntityBlock {
    public static final Map<Direction, BooleanProperty> PROPERTIES = new EnumMap<>(Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.EAST, BlockStateProperties.EAST, Direction.WEST, BlockStateProperties.WEST,
            Direction.UP, BlockStateProperties.UP, Direction.DOWN, BlockStateProperties.DOWN));
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Map.of(
            Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4), Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16),
            Direction.WEST, Block.box(0, 4, 4, 4, 12, 12), Direction.EAST, Block.box(12, 4, 4, 16, 12, 12),
            Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12), Direction.UP, Block.box(4, 12, 4, 12, 16, 12)));

    private final PipeType type;

    public PipeBlock(Properties props, PipeType type) {
        super(props);
        this.type = type;
        BlockState s = stateDefinition.any();
        for (BooleanProperty p : PROPERTIES.values()) {
            s = s.setValue(p, false);
        }
        registerDefaultState(s);
    }

    public PipeType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new PipeBlock(p, type));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        PROPERTIES.values().forEach(builder::add);
    }

    private boolean joins(LevelAccessor level, BlockPos pos, Direction d) {
        BlockPos n = pos.relative(d);
        BlockState other = level.getBlockState(n);
        if (other.getBlock() instanceof PipeBlock p) {
            return type.connectsTo(p.type());
        }
        return level instanceof Level l && l.getCapability(Capabilities.FluidHandler.BLOCK, n, d.getOpposite()) != null;
    }

    private BlockState withConnections(BlockState state, LevelAccessor level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPERTIES.get(d), joins(level, pos, d));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTIES.get(dir), joins(level, pos, dir));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        // machines placed next to a pipe don't always change its shape, so check again once their capabilities exist
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        BlockState updated = withConnections(state, level, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CORE;
        for (Direction d : Direction.values()) {
            if (state.getValue(PROPERTIES.get(d))) {
                shape = Shapes.or(shape, ARMS.get(d));
            }
        }
        return shape;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != RotaryBlockEntities.PIPE.get()) {
            return null;
        }
        return (l, p, s, be) -> ((PipeBlockEntity) be).serverTick();
    }
}
