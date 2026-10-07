package net.scwunge.rotarycraft.transmission;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Distribution Clutch block: it works on the four sides around it, so it is always placed facing along the ground. */
public class DistributionClutchBlock extends MachineBlock {
    public DistributionClutchBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                                   BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new DistributionClutchBlock(p, TransmissionRegistry.DISTRIBUTION_CLUTCH_BE, DistributionClutchBlockEntity::new));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }
}
