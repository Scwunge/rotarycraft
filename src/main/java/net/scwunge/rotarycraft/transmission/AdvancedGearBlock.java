package net.scwunge.rotarycraft.transmission;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;

/** An advanced gear block: a worm gear, a CVT, an energy coil or a 256x gear. They work along the ground, so they are always placed facing level. */
public class AdvancedGearBlock extends MachineBlock {
    /** The four kinds the original made of one block. */
    public enum Kind {
        WORM,
        CVT,
        COIL,
        HIGH;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final Kind kind;

    public AdvancedGearBlock(Properties props, Kind kind) {
        super(props, TransmissionRegistry.ADVANCED_GEAR_BE, AdvancedGearBlockEntity::new);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new AdvancedGearBlock(p, kind));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }
}
