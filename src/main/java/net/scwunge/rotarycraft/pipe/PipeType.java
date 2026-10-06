package net.scwunge.rotarycraft.pipe;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.registry.RotaryFluids;

import java.util.EnumSet;
import java.util.Set;

/**
 * The original's pipes and pipe fittings and what they carry:
 * <ul>
 * <li>hoses take lubricant only, fuel lines fuels only, fluid pipes any other liquid, bedrock pipes anything (and never
 * burst or melt);</li>
 * <li>a valve joins any of them; with a redstone signal it draws fluid from the tank beside it;</li>
 * <li>a separator takes fluid in from its sides and lets it out only downwards (upwards with a redstone signal);</li>
 * <li>a bypass joins pipes but only joins another bypass it was placed against, so parallel lines stay apart;</li>
 * <li>a suction pipe draws from tanks on any side and only pushes into pipes.</li>
 * </ul>
 */
public enum PipeType {
    HOSE("hose", 2_400_000, Integer.MAX_VALUE),
    PIPE("pipe", 2_400_000, 2500),
    FUEL_LINE("fuel_line", 2_400_000, Integer.MAX_VALUE),
    BEDROCK("bedrock_pipe", Integer.MAX_VALUE, 5000),
    VALVE("valve", 2_400_000, Integer.MAX_VALUE),
    SEPARATOR("separator", 2_400_000, Integer.MAX_VALUE),
    BYPASS("bypass", 2_400_000, Integer.MAX_VALUE),
    SUCTION("suction_pipe", 2_400_000, Integer.MAX_VALUE);

    /** Fluids fuel lines carry and other pipes refuse ({@code c:ethanol}, {@code c:jet_fuel}, and other mods' fuels). */
    public static final TagKey<Fluid> FUELS = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("rotarycraft", "pipe_fuels"));

    public final String id;
    /** Pressure (Pa) above which it bursts; liquids add 24 Pa per mB over atmospheric. */
    public final int maxPressure;
    /** Fluid temperature (C) above which it melts. */
    public final int maxTemperature;

    PipeType(String id, int maxPressure, int maxTemperature) {
        this.id = id;
        this.maxPressure = maxPressure;
        this.maxTemperature = maxTemperature;
    }

    public boolean isFitting() {
        return ordinal() >= VALVE.ordinal();
    }

    public boolean carries(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return false;
        }
        boolean lube = fluid.is(RotaryFluids.LUBRICANT.get());
        boolean fuel = fluid.is(FUELS);
        return switch (this) {
            case HOSE -> lube;
            case FUEL_LINE -> fuel;
            case PIPE -> !lube && !fuel;
            default -> true;
        };
    }

    /** What this kind is willing to join (the original's canConnectToPipe); a joint needs both sides willing. */
    private Set<PipeType> joins() {
        return switch (this) {
            case HOSE -> EnumSet.of(HOSE, VALVE, SEPARATOR, SUCTION);
            case FUEL_LINE -> EnumSet.of(FUEL_LINE, VALVE, SEPARATOR, BYPASS, SUCTION);
            case PIPE, BEDROCK -> EnumSet.of(PIPE, BEDROCK, VALVE, SEPARATOR, BYPASS, SUCTION);
            case VALVE -> EnumSet.of(PIPE, BEDROCK, HOSE, FUEL_LINE, SEPARATOR);
            case SEPARATOR -> EnumSet.of(PIPE, BEDROCK, FUEL_LINE, HOSE, VALVE);
            case BYPASS -> EnumSet.of(PIPE, BEDROCK, FUEL_LINE, BYPASS);
            case SUCTION -> EnumSet.of(PIPE, BEDROCK, FUEL_LINE, HOSE);
        };
    }

    /** Two bypasses only join when one was placed against the other; that is checked by the block. */
    public boolean connectsTo(PipeType other) {
        return joins().contains(other) && other.joins().contains(this);
    }

    /** Whether fluid may come in from a pipe on this side. */
    public boolean receivesFromPipe(Direction side, boolean powered) {
        return switch (this) {
            case SEPARATOR -> side.getAxis().isHorizontal();
            case SUCTION -> false;
            default -> true;
        };
    }

    /** Whether fluid may go out to a pipe on this side. */
    public boolean emitsToPipe(Direction side, boolean powered) {
        return this != SEPARATOR || side == (powered ? Direction.UP : Direction.DOWN);
    }

    /** Whether it draws from a tank on this side (other mods' tanks; this mod's machines on any side for plain pipes). */
    public boolean drawsFromTank(Direction side, boolean powered, boolean ownMachine) {
        return switch (this) {
            case VALVE -> powered;
            case SUCTION -> true;
            case SEPARATOR, BYPASS -> false;
            default -> ownMachine || side.getAxis() == Direction.Axis.Y;
        };
    }

    /** Whether it fills a tank on this side. */
    public boolean fillsTank(Direction side, boolean ownMachine) {
        return switch (this) {
            case VALVE, SUCTION, SEPARATOR, BYPASS -> false;
            default -> ownMachine || side.getAxis().isHorizontal();
        };
    }

    /** Whether it joins a fluid-holding block at all (fittings that never touch tanks don't). */
    public boolean touchesTanks() {
        return this != SEPARATOR && this != BYPASS;
    }

    /** The original: 101.3 kPa plus 24 Pa per mB held. */
    public static long pressure(int level) {
        return 101_300L + 24L * level;
    }
}
