package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Engages or disengages a power line with redstone. By default it transmits while powered; right-click to invert, so it
 * transmits while unpowered instead.
 */
public class ClutchBlockEntity extends PowerBlockEntity {
    private boolean needsRedstone = true;

    public ClutchBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.CLUTCH.get(), pos, state);
    }

    public boolean needsRedstone() {
        return needsRedstone;
    }

    public void toggleMode() {
        needsRedstone = !needsRedstone;
        setChanged();
    }

    public boolean isEngaged() {
        return level != null && level.hasNeighborSignal(worldPosition) == needsRedstone;
    }

    @Override
    public void serverTick() {
        if (isEngaged()) {
            IShaftPowerOutput.Reading in = readInput();
            setPower(in.torque(), in.omega());
        } else {
            setPower(0, 0);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("needsRedstone", needsRedstone);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        needsRedstone = !tag.contains("needsRedstone") || tag.getBoolean("needsRedstone");
    }
}
