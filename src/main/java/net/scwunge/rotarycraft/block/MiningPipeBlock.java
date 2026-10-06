package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * What the Borer leaves behind: a thin pipe through each block it cuts (along the way it bored, east-west or north-south), and a solid collar
 * of full blocks at the first slice. It drops nothing and breaks at once; breaking one clears the whole pipe line it belongs to, up to 64 blocks
 * either way.
 */
public class MiningPipeBlock extends Block {
    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    public static final int LINE_RANGE = 64;

    public enum Kind implements StringRepresentable {
        X("x", Direction.Axis.X), Z("z", Direction.Axis.Z), COLLAR("collar", null);

        private final String id;
        private final Direction.Axis axis;

        Kind(String id, Direction.Axis axis) {
            this.id = id;
            this.axis = axis;
        }

        public Direction.Axis axis() {
            return axis;
        }

        public static Kind along(Direction.Axis axis) {
            return axis == Direction.Axis.X ? X : Z;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    private static final VoxelShape SHAPE_X = Block.box(0, 5.28, 5.28, 16, 10.72, 10.72);
    private static final VoxelShape SHAPE_Z = Block.box(5.28, 5.28, 0, 10.72, 10.72, 16);

    public MiningPipeBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.X));
    }

    public static Properties props() {
        return BlockBehaviour.Properties.of().strength(0).noOcclusion().noLootTable().sound(net.minecraft.world.level.block.SoundType.METAL).forceSolidOff();
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(MiningPipeBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(KIND)) {
            case X -> SHAPE_X;
            case Z -> SHAPE_Z;
            case COLLAR -> Shapes.block();
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(KIND) != Kind.COLLAR;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1;
    }

    /** Breaking one pipe clears the rest of its line. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        Direction.Axis axis = state.getValue(KIND).axis();
        if (!level.isClientSide() && axis != null) {
            Direction dir = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            for (Direction d : new Direction[] {dir, dir.getOpposite()}) {
                for (int i = 1; i <= LINE_RANGE; i++) {
                    BlockPos p = pos.relative(d, i);
                    if (!level.isLoaded(p) || !level.getBlockState(p).is(this) || level.getBlockState(p).getValue(KIND) != state.getValue(KIND)) {
                        break;
                    }
                    level.destroyBlock(p, false);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
