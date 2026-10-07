package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/**
 * The Air Compressor, as the original's: it turns shaft power (into its back) into compressed air, power / watts-per-air at the converter
 * efficiency each tick, and pushes it out of its front into what takes it (a Pneumatic Engine, a pipe). It keeps up to 1000 mB. The original
 * fed another mod's air; this one makes this mod's compressed air.
 */
public class AirCompressorBlockEntity extends FarmBlockEntity {
    public static final int CAPACITY = 1000;

    private final FluidTank air = new FluidTank(CAPACITY, f -> f.getFluid() == ProcessRegistry.compressedAir()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public AirCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.AIR_COMPRESSOR_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "airCompressor";
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(0, 0, 1);
    }

    public FluidTank air() {
        return air;
    }

    /** mB of compressed air made each tick at the power it is turned with. */
    public int generated() {
        return (int) Math.min(Integer.MAX_VALUE, getPower() / RotaryConfig.get(FarmConfig.WATTS_PER_AIR) * RotaryConfig.get(FarmConfig.CONVERTER_EFFICIENCY));
    }

    @Override
    protected void machineTick(boolean powered) {
        ServerLevel server = server();
        int made = generated();
        if (made > 0) {
            air.fill(new FluidStack(ProcessRegistry.compressedAir(), made), IFluidHandler.FluidAction.EXECUTE);
        }
        if (air.getFluidAmount() > 0) {
            Direction out = facing();
            IFluidHandler target = server.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(out), out.getOpposite());
            if (target != null) {
                int taken = target.fill(air.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                if (taken > 0) {
                    air.drain(taken, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
        if (server.getGameTime() % 20 == 0) {
            syncNow();
        }
    }

    @Override
    protected int[] status() {
        return new int[] {air.getFluidAmount(), CAPACITY};
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("air", air.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        air.readFromNBT(registries, tag.getCompound("air"));
    }
}
