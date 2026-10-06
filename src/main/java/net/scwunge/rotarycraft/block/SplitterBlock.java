package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.blockentity.SplitterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Shaft junction. FACING is the straight output, the back is the straight input, BENT is the side branch (an input when
 * merging, an output when splitting). Right-click to cycle the split ratio, sneak-right-click to switch merge/split.
 * Screwdriver: right-click turns the straight axis, sneak-right-click turns the branch.
 */
public class SplitterBlock extends MachineBlock {
    public static final DirectionProperty BENT = DirectionProperty.create("bent");

    public SplitterBlock(Properties props) {
        super(props, RotaryBlockEntities.SPLITTER, SplitterBlockEntity::new);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(BENT, Direction.EAST));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(SplitterBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BENT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction out = ctx.getNearestLookingDirection();
        Direction bent = ctx.getClickedFace().getOpposite();
        if (bent.getAxis() == out.getAxis()) {
            bent = out.getAxis() == Direction.Axis.Y ? Direction.NORTH : out.getClockWise();
        }
        return defaultBlockState().setValue(FACING, out).setValue(BENT, bent);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING))).setValue(BENT, rotation.rotate(state.getValue(BENT)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING))).setValue(BENT, mirror.mirror(state.getValue(BENT)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SplitterBlockEntity splitter) {
            if (player.isShiftKeyDown()) {
                splitter.toggleMode();
            } else if (splitter.isSplitting()) {
                splitter.cycleRatio();
            }
            Component msg = !splitter.isSplitting()
                    ? Component.translatable("message.rotarycraft.splitter.merge")
                    : splitter.ratio() == 1
                    ? Component.translatable("message.rotarycraft.splitter.even")
                    : Component.translatable(splitter.favorsBent() ? "message.rotarycraft.splitter.favor_bent" : "message.rotarycraft.splitter.favor_straight",
                    splitter.ratio() - 1, splitter.ratio());
            player.displayClientMessage(msg, true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
