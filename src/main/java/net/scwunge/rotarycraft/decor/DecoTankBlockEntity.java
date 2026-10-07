package net.scwunge.rotarycraft.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/** What a Decorative Tank holds: the fluid it was placed full of, for the glass to show. */
public class DecoTankBlockEntity extends BlockEntity {
    private FluidStack fluid = FluidStack.EMPTY;

    public DecoTankBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.DECO_TANK_BE.get(), pos, state);
    }

    public FluidStack fluid() {
        return fluid;
    }

    public void setFluid(FluidStack fluid) {
        this.fluid = fluid;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!fluid.isEmpty()) {
            tag.put("fluid", fluid.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluid = tag.contains("fluid") ? FluidStack.parseOptional(registries, tag.getCompound("fluid")) : FluidStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
