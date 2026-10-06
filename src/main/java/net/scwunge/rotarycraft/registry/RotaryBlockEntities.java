package net.scwunge.rotarycraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.DynamometerBlockEntity;
import net.scwunge.rotarycraft.blockentity.BevelGearBlockEntity;
import net.scwunge.rotarycraft.blockentity.ClutchBlockEntity;
import net.scwunge.rotarycraft.blockentity.DCEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.FlywheelBlockEntity;
import net.scwunge.rotarycraft.blockentity.SteamEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.WindEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.GearboxBlockEntity;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.blockentity.CentrifugeBlockEntity;
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.blockentity.FermenterBlockEntity;
import net.scwunge.rotarycraft.blockentity.FrictionHeaterBlockEntity;
import net.scwunge.rotarycraft.blockentity.GrinderBlockEntity;
import net.scwunge.rotarycraft.blockentity.GasEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.GeneratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.MotorBlockEntity;
import net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity;
import net.scwunge.rotarycraft.blockentity.ShaftBlockEntity;
import net.scwunge.rotarycraft.blockentity.SplitterBlockEntity;

public class RotaryBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RotaryCraft.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShaftBlockEntity>> SHAFT = TYPES.register("shaft",
            () -> BlockEntityType.Builder.of(ShaftBlockEntity::new, RotaryBlocks.SHAFTS.values().stream().map(h -> (Block) h.get()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DCEngineBlockEntity>> DC_ENGINE = TYPES.register("dc_engine",
            () -> BlockEntityType.Builder.of(DCEngineBlockEntity::new, RotaryBlocks.DC_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindEngineBlockEntity>> WIND_ENGINE = TYPES.register("wind_engine",
            () -> BlockEntityType.Builder.of(WindEngineBlockEntity::new, RotaryBlocks.WIND_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamEngineBlockEntity>> STEAM_ENGINE = TYPES.register("steam_engine",
            () -> BlockEntityType.Builder.of(SteamEngineBlockEntity::new, RotaryBlocks.STEAM_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlywheelBlockEntity>> FLYWHEEL = TYPES.register("flywheel",
            () -> BlockEntityType.Builder.of(FlywheelBlockEntity::new, RotaryBlocks.FLYWHEELS.values().stream().map(h -> (Block) h.get()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClutchBlockEntity>> CLUTCH = TYPES.register("clutch",
            () -> BlockEntityType.Builder.of(ClutchBlockEntity::new, RotaryBlocks.CLUTCH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BevelGearBlockEntity>> BEVEL_GEAR = TYPES.register("bevel_gear",
            () -> BlockEntityType.Builder.of(BevelGearBlockEntity::new, RotaryBlocks.BEVEL_GEAR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SplitterBlockEntity>> SPLITTER = TYPES.register("splitter",
            () -> BlockEntityType.Builder.of(SplitterBlockEntity::new, RotaryBlocks.SPLITTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GearboxBlockEntity>> GEARBOX = TYPES.register("gearbox",
            () -> BlockEntityType.Builder.of(GearboxBlockEntity::new, RotaryBlocks.allGearboxes().map(h -> (Block) h.get()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR = TYPES.register("generator",
            () -> BlockEntityType.Builder.of(GeneratorBlockEntity::new, RotaryBlocks.GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotorBlockEntity>> ELECTRIC_MOTOR = TYPES.register("electric_motor",
            () -> BlockEntityType.Builder.of(MotorBlockEntity::new, RotaryBlocks.ELECTRIC_MOTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrinderBlockEntity>> GRINDER = TYPES.register("grinder",
            () -> BlockEntityType.Builder.of(GrinderBlockEntity::new, RotaryBlocks.GRINDER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtractorBlockEntity>> EXTRACTOR = TYPES.register("extractor",
            () -> BlockEntityType.Builder.of(ExtractorBlockEntity::new, RotaryBlocks.EXTRACTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlastFurnaceBlockEntity>> BLAST_FURNACE = TYPES.register("blast_furnace",
            () -> BlockEntityType.Builder.of(BlastFurnaceBlockEntity::new, RotaryBlocks.BLAST_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FrictionHeaterBlockEntity>> FRICTION_HEATER = TYPES.register("friction_heater",
            () -> BlockEntityType.Builder.of(FrictionHeaterBlockEntity::new, RotaryBlocks.FRICTION_HEATER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FermenterBlockEntity>> FERMENTER = TYPES.register("fermenter",
            () -> BlockEntityType.Builder.of(FermenterBlockEntity::new, RotaryBlocks.FERMENTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE = TYPES.register("centrifuge",
            () -> BlockEntityType.Builder.of(CentrifugeBlockEntity::new, RotaryBlocks.CENTRIFUGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GasEngineBlockEntity>> GAS_ENGINE = TYPES.register("gas_engine",
            () -> BlockEntityType.Builder.of(GasEngineBlockEntity::new, RotaryBlocks.GAS_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.PerformanceEngineBlockEntity>> PERFORMANCE_ENGINE = TYPES.register("performance_engine",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.PerformanceEngineBlockEntity::new, RotaryBlocks.PERFORMANCE_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.MicroturbineBlockEntity>> MICROTURBINE = TYPES.register("microturbine",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.MicroturbineBlockEntity::new, RotaryBlocks.MICROTURBINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.JetEngineBlockEntity>> JET_ENGINE = TYPES.register("jet_engine",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.JetEngineBlockEntity::new, RotaryBlocks.JET_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.HydroEngineBlockEntity>> HYDRO_ENGINE = TYPES.register("hydro_engine",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.HydroEngineBlockEntity::new, RotaryBlocks.HYDRO_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.ACEngineBlockEntity>> AC_ENGINE = TYPES.register("ac_engine",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.ACEngineBlockEntity::new, RotaryBlocks.AC_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.MagnetizerBlockEntity>> MAGNETIZER = TYPES.register("magnetizer",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.MagnetizerBlockEntity::new, RotaryBlocks.MAGNETIZER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.FractionatorBlockEntity>> FRACTIONATOR = TYPES.register("fractionator",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.FractionatorBlockEntity::new, RotaryBlocks.FRACTIONATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.CompactorBlockEntity>> COMPACTOR = TYPES.register("compactor",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.CompactorBlockEntity::new, RotaryBlocks.COMPACTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.PumpBlockEntity>> PUMP = TYPES.register("pump",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.PumpBlockEntity::new, RotaryBlocks.PUMP.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.ReservoirBlockEntity>> RESERVOIR = TYPES.register("reservoir",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.ReservoirBlockEntity::new, RotaryBlocks.RESERVOIR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RockMelterBlockEntity>> ROCK_MELTER = TYPES.register("rock_melter",
            () -> BlockEntityType.Builder.of(RockMelterBlockEntity::new, RotaryBlocks.ROCK_MELTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<net.scwunge.rotarycraft.blockentity.PipeBlockEntity>> PIPE = TYPES.register("pipe",
            () -> BlockEntityType.Builder.of(net.scwunge.rotarycraft.blockentity.PipeBlockEntity::new,
                    RotaryBlocks.PIPES.values().stream().map(h -> (Block) h.get()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamometerBlockEntity>> DYNAMOMETER = TYPES.register("dynamometer",
            () -> BlockEntityType.Builder.of(DynamometerBlockEntity::new, RotaryBlocks.DYNAMOMETER.get()).build(null));
}
