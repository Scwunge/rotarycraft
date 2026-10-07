package net.scwunge.rotarycraft.transmission;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The Multi-Clutch block: power comes in at the back, and leaves by the side its redstone strength picks (right-click for the screen). */
public class MultiClutchBlock extends MachineBlock {
    public MultiClutchBlock(Properties props, Supplier<? extends BlockEntityType<? extends PowerBlockEntity>> type,
                            BiFunction<BlockPos, BlockState, ? extends PowerBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new MultiClutchBlock(p, TransmissionRegistry.MULTI_CLUTCH_BE, MultiClutchBlockEntity::new));
    }
}
