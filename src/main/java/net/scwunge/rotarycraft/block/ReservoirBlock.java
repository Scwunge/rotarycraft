package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scwunge.rotarycraft.blockentity.ReservoirBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/** The Reservoir block: an open tank you can step into (covered: a closed box). */
public class ReservoirBlock extends BaseEntityBlock {
    public static final BooleanProperty COVERED = BooleanProperty.create("covered");
    private static final VoxelShape OPEN = Shapes.join(Shapes.block(), Block.box(1, 1, 1, 15, 16, 15), BooleanOp.ONLY_FIRST);

    public ReservoirBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(COVERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ReservoirBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COVERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(COVERED) ? Shapes.block() : OPEN;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ReservoirBlockEntity r) {
            r.entityInside(entity);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReservoirBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != RotaryBlockEntities.RESERVOIR.get()) {
            return null;
        }
        return (l, p, s, be) -> ((ReservoirBlockEntity) be).serverTick();
    }
}
