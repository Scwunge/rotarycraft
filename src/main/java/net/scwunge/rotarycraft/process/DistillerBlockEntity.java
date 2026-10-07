package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.ProcessRegistry;
import net.scwunge.rotarycraft.registry.RotaryFluids;

import java.util.List;
import java.util.function.Supplier;

/**
 * The Distiller, as the original's: every six ticks, if its shaft turns hard and fast enough for the fluid in its input tank, it turns some of it
 * into another. The original's fluids came from other mods; here they are the common tags {@code c:crude_oil}, {@code c:bioethanol} and
 * {@code c:biofuel}, which those mods' fluids join. Fluid goes in from every side but the top and comes out of the top.
 */
public class DistillerBlockEntity extends FarmBlockEntity {
    public static final int CAPACITY = 6000;
    public static final int PERIOD = 6;

    /** What one fluid becomes: {@code consumed} mB in, {@code produced} mB out, at no less than this torque and power. */
    public record Conversion(TagKey<Fluid> input, Supplier<Fluid> output, int consumed, int produced, int minTorque, long minPower) {
    }

    public static final TagKey<Fluid> CRUDE_OIL = tag("crude_oil");
    public static final TagKey<Fluid> BIOETHANOL = tag("bioethanol");
    public static final TagKey<Fluid> BIOFUEL = tag("biofuel");

    /** What it can make: this list takes more (another mod's fluids, say), added with {@link #addConversion}. */
    public static final List<Conversion> CONVERSIONS = new java.util.concurrent.CopyOnWriteArrayList<>(List.of(
            new Conversion(CRUDE_OIL, () -> RotaryFluids.LUBRICANT.get(), 1, 6, 2048, 8192),
            new Conversion(BIOETHANOL, () -> RotaryFluids.ETHANOL.get(), 1, 1, 512, 131072),
            new Conversion(BIOFUEL, () -> RotaryFluids.ETHANOL.get(), 2, 1, 512, 131072)));

    public static void addConversion(Conversion conversion) {
        CONVERSIONS.add(conversion);
    }

    private static TagKey<Fluid> tag(String name) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", name));
    }

    public static Conversion conversionFor(FluidStack fluid) {
        for (Conversion c : CONVERSIONS) {
            if (!fluid.isEmpty() && fluid.is(c.input())) {
                return c;
            }
        }
        return null;
    }

    private final FluidTank input = new FluidTank(CAPACITY, f -> conversionFor(f) != null) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank output = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int tickCount;

    public DistillerBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.DISTILLER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "distiller";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(0, 0, 1);
    }

    public FluidTank input() {
        return input;
    }

    public FluidTank output() {
        return output;
    }

    /** What pipes see: fluid goes into the input tank and comes out of the output tank. */
    public IFluidHandler handler() {
        return Tanks.split(input, output);
    }

    /** Whether it can make this conversion now: enough power, input, and room. */
    public boolean canMake(Conversion c) {
        return c != null && getTorque() >= c.minTorque() && (long) getTorque() * getOmega() >= c.minPower() && input.getFluidAmount() >= c.consumed()
                && output.fill(new FluidStack(c.output().get(), c.produced()), IFluidHandler.FluidAction.SIMULATE) == c.produced();
    }

    @Override
    protected void machineTick(boolean powered) {
        if (++tickCount >= PERIOD) {
            tickCount = 0;
            Conversion c = conversionFor(input.getFluid());
            if (canMake(c)) {
                input.drain(c.consumed(), IFluidHandler.FluidAction.EXECUTE);
                output.fill(new FluidStack(c.output().get(), c.produced()), IFluidHandler.FluidAction.EXECUTE);
            }
        }
        if (output.getFluidAmount() > 0) {
            IFluidHandler above = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.above(), Direction.DOWN);
            if (above != null) {
                int taken = above.fill(output.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                if (taken > 0) {
                    output.drain(taken, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
        if (level.getGameTime() % 20 == 0) {
            syncNow();
        }
    }

    @Override
    protected int[] status() {
        return new int[] {input.getFluidAmount(), output.getFluidAmount(), CAPACITY};
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        writeData(tag, registries);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        readData(tag, registries);
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", input.writeToNBT(registries, new CompoundTag()));
        tag.put("output", output.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        input.readFromNBT(registries, tag.getCompound("input"));
        output.readFromNBT(registries, tag.getCompound("output"));
    }
}
