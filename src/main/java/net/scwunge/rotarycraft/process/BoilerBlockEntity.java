package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/**
 * The Friction Boiler, as the original's: shaft power (from any side) heats it, 0.3125 x log2 of the power in degrees a second, while it cools
 * towards the air's temperature. Above 100 C, with water in its tank (24000 mB, from pipes on any side but the top) and room for steam, it
 * stores up what the shaft gives it (200 x the power a tick) and makes 8 mB of steam from each millibucket of water for about 2600 units of
 * it; the steam goes out of the top to whatever takes it, and to pipes. Above 500 C it blows up. It melts snow and ice beside it above 50 C.
 */
public class BoilerBlockEntity extends FarmBlockEntity implements Heatable {
    public static final int CAPACITY = 24_000;
    public static final int MAX_TEMPERATURE = 500;
    public static final int ENERGY_PER_MB = 2600;
    public static final int STEAM_PER_WATER = 8;

    private final FluidTank water = new FluidTank(CAPACITY, f -> f.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank steam = new FluidTank(CAPACITY, f -> SteamTurbineBlockEntity.isSteam(f)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int temperature = Integer.MIN_VALUE;
    private long storedEnergy;

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.BOILER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "boiler";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(0, 0, 1);
    }

    public FluidTank water() {
        return water;
    }

    public FluidTank steam() {
        return steam;
    }

    /** What pipes see: water goes in, steam comes out. */
    public IFluidHandler handler() {
        return Tanks.split(water, steam);
    }

    public long storedEnergy() {
        return storedEnergy;
    }

    @Override
    public int getTemperature() {
        return temperature == Integer.MIN_VALUE ? Ambient.temperature(level, worldPosition) : temperature;
    }

    @Override
    public void addTemperature(int amount) {
        temperature = getTemperature() + amount;
        setChanged();
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    @Override
    public boolean canBeCooledWithFins() {
        return false;
    }

    /** Whether it takes the shaft's power in, as the original's: warm enough, with water to boil and room for steam. */
    public boolean acceptsEnergy() {
        return getTemperature() > 100 && !water.isEmpty() && steam.getSpace() > 0;
    }

    @Override
    protected void machineTick(boolean powered) {
        ServerLevel server = server();
        long time = server.getGameTime();
        if (time % 20 == 0) {
            updateTemperature(server);
        }
        if (acceptsEnergy()) {
            storedEnergy += (long) (getPower() * 200 * RotaryConfig.get(net.scwunge.rotarycraft.config.FarmConfig.CONVERTER_EFFICIENCY));
        }
        int space = steam.getSpace() / STEAM_PER_WATER;
        int mB = (int) Math.min(space, Math.min(water.getFluidAmount(), storedEnergy / ENERGY_PER_MB));
        if (mB > 0) {
            water.drain(mB, IFluidHandler.FluidAction.EXECUTE);
            steam.fill(new FluidStack(ProcessRegistry.steam(), mB * STEAM_PER_WATER), IFluidHandler.FluidAction.EXECUTE);
            storedEnergy -= (long) mB * ENERGY_PER_MB;
        }
        if (steam.getFluidAmount() > 0) {
            IFluidHandler above = server.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.above(), Direction.DOWN);
            if (above != null) {
                int taken = above.fill(steam.getFluid().copy(), IFluidHandler.FluidAction.EXECUTE);
                if (taken > 0) {
                    steam.drain(taken, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
        if (time % 20 == 0) {
            syncNow();
        }
    }

    private void updateTemperature(ServerLevel server) {
        int ambient = Ambient.temperature(server, worldPosition);
        double t = getTemperature();
        if (getPower() > 0) {
            t += 0.3125 * Math.log(getPower()) / Math.log(2);
        }
        t -= (t - ambient) / 40.0;
        if (t - ambient <= 40 && t > ambient) {
            t--;
        }
        temperature = (int) t;
        if (temperature > MAX_TEMPERATURE) {
            temperature = MAX_TEMPERATURE;
            overheat(server);
            return;
        }
        if (temperature > 50) {
            for (Direction side : Direction.values()) {
                BlockState state = server.getBlockState(worldPosition.relative(side));
                if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
                    server.removeBlock(worldPosition.relative(side), false);
                } else if (state.is(Blocks.ICE)) {
                    server.setBlockAndUpdate(worldPosition.relative(side), Blocks.WATER.defaultBlockState());
                }
            }
        }
    }

    private void overheat(ServerLevel server) {
        server.removeBlock(worldPosition, false);
        server.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 6,
                RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    @Override
    protected int[] status() {
        return new int[] {water.getFluidAmount(), steam.getFluidAmount(), getTemperature(), CAPACITY};
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        tag.putInt("temperature", temperature);
        tag.putLong("energy", storedEnergy);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        water.readFromNBT(registries, tag.getCompound("water"));
        steam.readFromNBT(registries, tag.getCompound("steam"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        storedEnergy = tag.getLong("energy");
    }
}
