package net.scwunge.rotarycraft.pipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/**
 * The original's four pipe kinds and what they carry: hoses take lubricant only, fuel lines fuels only, pipes any other
 * liquid, bedrock pipes anything (and never burst or melt).
 */
public enum PipeType {
    HOSE("hose", 2_400_000, Integer.MAX_VALUE),
    PIPE("pipe", 2_400_000, 2500),
    FUEL_LINE("fuel_line", 2_400_000, Integer.MAX_VALUE),
    BEDROCK("bedrock_pipe", Integer.MAX_VALUE, 5000);

    /** Fluids fuel lines carry and other pipes refuse ({@code c:ethanol}, {@code c:jet_fuel}, and other mods' fuels). */
    public static final TagKey<Fluid> FUELS = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("rotarycraft", "pipe_fuels"));

    public final String id;
    /** Pressure (Pa) above which the pipe bursts; liquids add 24 Pa per mB over atmospheric. */
    public final int maxPressure;
    /** Fluid temperature (C) above which the pipe melts. */
    public final int maxTemperature;

    PipeType(String id, int maxPressure, int maxTemperature) {
        this.id = id;
        this.maxPressure = maxPressure;
        this.maxTemperature = maxTemperature;
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
            case BEDROCK -> true;
        };
    }

    /** Pipes and bedrock pipes join each other; hoses and fuel lines only join their own kind. */
    public boolean connectsTo(PipeType other) {
        if (this == other) {
            return true;
        }
        return (this == PIPE && other == BEDROCK) || (this == BEDROCK && other == PIPE);
    }

    /** The original: 101.3 kPa plus 24 Pa per mB held. */
    public static long pressure(int level) {
        return 101_300L + 24L * level;
    }
}
