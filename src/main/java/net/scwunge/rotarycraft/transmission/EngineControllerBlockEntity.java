package net.scwunge.rotarycraft.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.blockentity.FuelEngineBlockEntity;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * The Engine Control Unit, as the original: put it directly above or below a gas, performance, micro or jet engine and it throttles that
 * engine and feeds it fuel from its own small tank (which takes fuel from pipes). The screwdriver steps its setting (shutdown, standby, low,
 * medium, full), or with sneak turns on Redstone mode, where the signal strength picks the setting: 0 full, 3 medium, 6 low, 9 standby, 12
 * shutdown, and 15 (full strength) shutdown too. Running slow burns fuel more thriftily than the speed alone would.
 * <p>
 * Not here: the original's dye colours and ProjectRed bundled signal.
 */
public class EngineControllerBlockEntity extends BlockEntity implements MachineInteractions {
    public static final int FUEL_CAPACITY = 3000;
    /** After a change in the signal, further changes are ignored for this long (the original's 60 ticks). */
    public static final int SIGNAL_HOLD = 60;

    /** A setting: how much slower the engine turns, and how much longer between each unit of fuel (the original's factors). */
    public enum Setting {
        SHUTDOWN(0, 0),
        STANDBY(16, 64),
        LOW(4, 8),
        MEDIUM(2, 2),
        FULL(1, 1);

        public final int speedFactor;
        public final int fuelFactor;

        Setting(int speedFactor, int fuelFactor) {
            this.speedFactor = speedFactor;
            this.fuelFactor = fuelFactor;
        }

        public Setting next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Component label() {
            return Component.translatable("message.rotarycraft.ecu.setting." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private final FluidTank tank = new FluidTank(FUEL_CAPACITY, this::accepts) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private Setting setting = Setting.FULL;
    private boolean redstoneMode;
    private int holdTicks;
    private int lastSignal;

    public EngineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(TransmissionRegistry.ENGINE_CONTROLLER_BE.get(), pos, state);
    }

    public FluidTank tank() {
        return tank;
    }

    public Setting setting() {
        return setting;
    }

    public void setSetting(Setting next) {
        setting = next;
        setChanged();
    }

    public boolean redstoneMode() {
        return redstoneMode;
    }

    public void setRedstoneMode(boolean on) {
        redstoneMode = on;
        setChanged();
    }

    /** True if the engine may run at all (not shut down). */
    public boolean canProducePower() {
        return setting.speedFactor != 0;
    }

    /** True if the engine burns fuel at all (not shut down). */
    public boolean consumesFuel() {
        return setting.fuelFactor != 0;
    }

    /** The share of its top speed the engine may turn at: 1 at full, down to 1/16 on standby, 0 when shut down. */
    public float speedMultiplier() {
        return canProducePower() ? 1F / setting.speedFactor : 0;
    }

    /** How many times longer the engine takes to burn each unit of fuel (turbines count an eighth of it, at least 1). */
    public int fuelIntervalFactor(boolean turbine) {
        int base = setting.fuelFactor;
        if (turbine) {
            base /= 8;
        }
        return Math.max(1, base);
    }

    /** The engine this controls, directly above or below. */
    @Nullable
    public FuelEngineBlockEntity engine() {
        if (level == null) {
            return null;
        }
        for (Direction side : new Direction[] {Direction.UP, Direction.DOWN}) {
            if (level.getBlockEntity(worldPosition.relative(side)) instanceof FuelEngineBlockEntity engine) {
                return engine;
            }
        }
        return null;
    }

    /** Fuel goes in if the engine beside it burns it; with no engine yet, any fuel a controllable engine burns. */
    private boolean accepts(FluidStack fluid) {
        FuelEngineBlockEntity engine = engine();
        if (engine != null) {
            return engine.fuel().isFluidValid(fluid);
        }
        return fluid.is(RotaryFluids.ETHANOL.get()) || fluid.is(RotaryFluids.JET_FUEL.get());
    }

    public void serverTick() {
        if (holdTicks > 0) {
            holdTicks--;
        }
        int signal = holdTicks == 0 ? level.getBestNeighborSignal(worldPosition) : lastSignal;
        if (holdTicks == 0 && signal == 0 && level.hasNeighborSignal(worldPosition)) {
            signal = 15;
        }
        if (signal != lastSignal) {
            holdTicks = SIGNAL_HOLD;
        }
        lastSignal = signal;
        if (redstoneMode) {
            Setting wanted = signal >= 15 ? Setting.SHUTDOWN : Setting.values()[4 - signal / 3];
            if (wanted != setting) {
                setSetting(wanted);
            }
        }
        FuelEngineBlockEntity engine = engine();
        if (engine != null && !tank.isEmpty()) {
            FluidStack in = tank.getFluid();
            int moved = engine.fuel().fill(in.copyWithAmount(in.getAmount() / 4 + 1), IFluidHandler.FluidAction.EXECUTE);
            if (moved > 0) {
                tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    @Override
    public boolean onScrewdriver(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            setRedstoneMode(!redstoneMode);
            player.displayClientMessage(Component.translatable(redstoneMode ? "message.rotarycraft.ecu.redstone" : "message.rotarycraft.ecu.manual"), true);
        } else {
            setSetting(setting.next());
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.rotarycraft.ecu.set", setting.label(), 100 * speedMultiplier()), true);
            }
        }
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("setting", setting.ordinal());
        tag.putBoolean("redstone", redstoneMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        setting = Setting.values()[Math.floorMod(tag.getInt("setting"), Setting.values().length)];
        redstoneMode = tag.getBoolean("redstone");
    }
}
