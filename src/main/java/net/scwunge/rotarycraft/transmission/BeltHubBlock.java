package net.scwunge.rotarycraft.transmission;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;

/** A belt, chain or split belt pulley block. Its facing is the side its shaft is on; the belt item joins two of them. */
public class BeltHubBlock extends MachineBlock {
    /** The three kinds: the torque and the speed they take without trouble, and the belt item that joins two of them. */
    public enum Kind {
        BELT(8192, 8192, "belt"),
        CHAIN(16384, 65536, "chain"),
        SPLIT(8192, 8192, "belt");

        public final int maxTorque;
        public final int maxSpeed;
        private final String item;

        Kind(int maxTorque, int maxSpeed, String item) {
            this.maxTorque = maxTorque;
            this.maxSpeed = maxSpeed;
            this.item = item;
        }

        public Item beltItem() {
            return RotaryParts.part(item).get();
        }
    }

    private final Kind kind;

    public BeltHubBlock(Properties props, Kind kind) {
        super(props, TransmissionRegistry.BELT_HUB_BE, BeltHubBlockEntity::new);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new BeltHubBlock(p, kind));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BeltHubBlockEntity hub) {
            hub.onBroken();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
