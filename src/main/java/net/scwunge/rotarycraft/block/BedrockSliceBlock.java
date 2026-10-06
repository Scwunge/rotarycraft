package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Bedrock being ground away by a Bedrock Breaker: the bedrock block, thinning from the side the machine is on, sixteenths at a time. It is as
 * unbreakable as the bedrock it was, and gives nothing; only the machine takes it the last step, as dust.
 */
public class BedrockSliceBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final int STAGES = 16;
    /** How far it has been ground, in sixteenths of the block. */
    public static final IntegerProperty PROGRESS = IntegerProperty.create("progress", 0, STAGES - 1);

    private static final VoxelShape[][] SHAPES = new VoxelShape[6][STAGES];

    static {
        for (Direction dir : Direction.values()) {
            for (int p = 0; p < STAGES; p++) {
                double keep = 16 - p;
                double[] min = {0, 0, 0};
                double[] max = {16, 16, 16};
                int axis = dir.getAxis().ordinal();
                if (dir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    max[axis] = keep;
                } else {
                    min[axis] = 16 - keep;
                }
                SHAPES[dir.ordinal()][p] = Block.box(min[0], min[1], min[2], max[0], max[1], max[2]);
            }
        }
    }

    public BedrockSliceBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(PROGRESS, 0));
    }

    public static Properties props() {
        return BlockBehaviour.Properties.of().strength(-1, 3_600_000f).noOcclusion().noLootTable().sound(SoundType.STONE).pushReaction(PushReaction.BLOCK)
                .isValidSpawn((state, level, pos, type) -> false);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(BedrockSliceBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PROGRESS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()][state.getValue(PROGRESS)];
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
