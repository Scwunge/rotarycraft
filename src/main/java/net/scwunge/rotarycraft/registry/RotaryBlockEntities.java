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
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.blockentity.FrictionHeaterBlockEntity;
import net.scwunge.rotarycraft.blockentity.GrinderBlockEntity;
import net.scwunge.rotarycraft.blockentity.GeneratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.MotorBlockEntity;
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
            () -> BlockEntityType.Builder.of(GearboxBlockEntity::new, RotaryBlocks.GEARBOXES.values().stream().map(h -> (Block) h.get()).toArray(Block[]::new)).build(null));
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
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamometerBlockEntity>> DYNAMOMETER = TYPES.register("dynamometer",
            () -> BlockEntityType.Builder.of(DynamometerBlockEntity::new, RotaryBlocks.DYNAMOMETER.get()).build(null));
}
