package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.GearboxBlock;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Trades speed for torque. In reduction mode the output turns {@code ratio} times slower with {@code ratio} times the torque;
 * in acceleration mode the reverse. Power is conserved. A steel gearbox breaks like a steel shaft when its output exceeds
 * the material's limits.
 */
public class GearboxBlockEntity extends PowerBlockEntity {
    private boolean reduction = true;

    public GearboxBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.GEARBOX.get(), pos, state);
    }

    public int ratio() {
        return getBlockState().getBlock() instanceof GearboxBlock gb ? gb.ratio() : 2;
    }

    public boolean isReduction() {
        return reduction;
    }

    public void toggleMode() {
        reduction = !reduction;
        setChanged();
    }

    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = readInput();
        int ratio = ratio();
        int torqueOut;
        int omegaOut;
        if (reduction) {
            omegaOut = in.omega() / ratio;
            torqueOut = (int) Math.min(Integer.MAX_VALUE, (long) in.torque() * ratio);
        } else {
            omegaOut = (int) Math.min(Integer.MAX_VALUE, (long) in.omega() * ratio);
            torqueOut = in.torque() / ratio;
        }
        if (RotaryConfig.get(RotaryConfig.SHAFT_FAILURE) && ShaftMaterial.STEEL.fails(torqueOut, omegaOut)) {
            ShaftBlockEntity.fail(level, worldPosition, new IShaftPowerOutput.Reading(torqueOut, omegaOut));
            return;
        }
        setPower(torqueOut, omegaOut);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("reduction", reduction);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reduction = !tag.contains("reduction") || tag.getBoolean("reduction");
    }
}
