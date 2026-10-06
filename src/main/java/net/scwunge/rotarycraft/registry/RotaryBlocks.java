package net.scwunge.rotarycraft.registry;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.DynamometerBlock;
import net.scwunge.rotarycraft.block.GearboxBlock;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.block.ShaftBlock;
import net.scwunge.rotarycraft.block.SplitterBlock;
import net.scwunge.rotarycraft.block.BevelGearBlock;
import net.scwunge.rotarycraft.block.ClutchBlock;
import net.scwunge.rotarycraft.block.FlywheelBlock;
import net.scwunge.rotarycraft.blockentity.DCEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.SteamEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.WindEngineBlockEntity;
import net.scwunge.rotarycraft.power.FlywheelType;
import net.scwunge.rotarycraft.blockentity.GeneratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.blockentity.FermenterBlockEntity;
import net.scwunge.rotarycraft.blockentity.FrictionHeaterBlockEntity;
import net.scwunge.rotarycraft.blockentity.GrinderBlockEntity;
import net.scwunge.rotarycraft.blockentity.MotorBlockEntity;
import net.scwunge.rotarycraft.power.ShaftMaterial;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class RotaryBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RotaryCraft.MOD_ID);

    /** Shaft materials that have a craftable shaft in this phase (tungsten arrives with its ore processing). */
    public static final ShaftMaterial[] SHAFT_MATERIALS = {ShaftMaterial.WOOD, ShaftMaterial.STONE, ShaftMaterial.STEEL, ShaftMaterial.DIAMOND, ShaftMaterial.BEDROCK};
    public static final int[] GEARBOX_RATIOS = {2, 4, 8, 16};

    public static final Map<ShaftMaterial, DeferredBlock<ShaftBlock>> SHAFTS = new EnumMap<>(ShaftMaterial.class);
    /** Gearboxes by material, then ratio. */
    public static final Map<ShaftMaterial, Map<Integer, DeferredBlock<GearboxBlock>>> GEARBOXES = new EnumMap<>(ShaftMaterial.class);
    public static final ShaftMaterial[] GEARBOX_MATERIALS = {ShaftMaterial.WOOD, ShaftMaterial.STONE, ShaftMaterial.STEEL,
            ShaftMaterial.TUNGSTEN, ShaftMaterial.DIAMOND, ShaftMaterial.BEDROCK};
    public static final Map<FlywheelType, DeferredBlock<FlywheelBlock>> FLYWHEELS = new EnumMap<>(FlywheelType.class);

    static {
        for (ShaftMaterial m : SHAFT_MATERIALS) {
            SHAFTS.put(m, BLOCKS.register("shaft_" + m.id(), () -> new ShaftBlock(shaftProps(m), m)));
        }
        for (FlywheelType t : FlywheelType.values()) {
            FLYWHEELS.put(t, BLOCKS.register("flywheel_" + t.id(), () -> new FlywheelBlock(machineProps(), t)));
        }
        for (ShaftMaterial m : GEARBOX_MATERIALS) {
            Map<Integer, DeferredBlock<GearboxBlock>> byRatio = new LinkedHashMap<>();
            for (int ratio : GEARBOX_RATIOS) {
                byRatio.put(ratio, BLOCKS.register("gearbox_" + m.id() + "_" + ratio + "x", () -> new GearboxBlock(gearboxProps(m), m, ratio)));
            }
            GEARBOXES.put(m, byRatio);
        }
    }

    public static final DeferredBlock<MachineBlock> DC_ENGINE = BLOCKS.register("dc_engine",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.DC_ENGINE, DCEngineBlockEntity::new));
    public static final DeferredBlock<MachineBlock> WIND_ENGINE = BLOCKS.register("wind_engine",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.WIND_ENGINE, WindEngineBlockEntity::new));
    public static final DeferredBlock<MachineBlock> STEAM_ENGINE = BLOCKS.register("steam_engine",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.STEAM_ENGINE, SteamEngineBlockEntity::new));
    public static final DeferredBlock<ClutchBlock> CLUTCH = BLOCKS.register("clutch", () -> new ClutchBlock(machineProps()));
    public static final DeferredBlock<SplitterBlock> SPLITTER = BLOCKS.register("splitter", () -> new SplitterBlock(machineProps()));
    public static final DeferredBlock<BevelGearBlock> BEVEL_GEAR = BLOCKS.register("bevel_gear", () -> new BevelGearBlock(machineProps()));
    public static final DeferredBlock<MachineBlock> GENERATOR = BLOCKS.register("generator",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.GENERATOR, GeneratorBlockEntity::new));
    public static final DeferredBlock<MachineBlock> ELECTRIC_MOTOR = BLOCKS.register("electric_motor",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.ELECTRIC_MOTOR, MotorBlockEntity::new));
    public static final DeferredBlock<MachineBlock> GRINDER = BLOCKS.register("grinder",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.GRINDER, GrinderBlockEntity::new));
    public static final DeferredBlock<MachineBlock> EXTRACTOR = BLOCKS.register("extractor",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.EXTRACTOR, ExtractorBlockEntity::new));
    public static final DeferredBlock<MachineBlock> BLAST_FURNACE = BLOCKS.register("blast_furnace",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.BLAST_FURNACE, BlastFurnaceBlockEntity::new));
    public static final DeferredBlock<MachineBlock> FRICTION_HEATER = BLOCKS.register("friction_heater",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.FRICTION_HEATER, FrictionHeaterBlockEntity::new));
    public static final DeferredBlock<MachineBlock> FERMENTER = BLOCKS.register("fermenter",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.FERMENTER, FermenterBlockEntity::new));
    public static final DeferredBlock<MachineBlock> CENTRIFUGE = BLOCKS.register("centrifuge",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.CENTRIFUGE, net.scwunge.rotarycraft.blockentity.CentrifugeBlockEntity::new));
    public static final DeferredBlock<MachineBlock> GAS_ENGINE = BLOCKS.register("gas_engine",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.GAS_ENGINE, net.scwunge.rotarycraft.blockentity.GasEngineBlockEntity::new));
    public static final DeferredBlock<MachineBlock> PERFORMANCE_ENGINE = BLOCKS.register("performance_engine",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.PERFORMANCE_ENGINE, net.scwunge.rotarycraft.blockentity.PerformanceEngineBlockEntity::new));
    public static final DeferredBlock<MachineBlock> MICROTURBINE = BLOCKS.register("microturbine",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.MICROTURBINE, net.scwunge.rotarycraft.blockentity.MicroturbineBlockEntity::new));
    public static final DeferredBlock<net.scwunge.rotarycraft.block.JetEngineBlock> JET_ENGINE = BLOCKS.register("jet_engine",
            () -> new net.scwunge.rotarycraft.block.JetEngineBlock(machineProps()));
    public static final DeferredBlock<net.scwunge.rotarycraft.block.HydroEngineBlock> HYDRO_ENGINE = BLOCKS.register("hydro_engine",
            () -> new net.scwunge.rotarycraft.block.HydroEngineBlock(machineProps()));
    public static final DeferredBlock<MachineBlock> AC_ENGINE = BLOCKS.register("ac_engine",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.AC_ENGINE, net.scwunge.rotarycraft.blockentity.ACEngineBlockEntity::new));
    public static final DeferredBlock<MachineBlock> MAGNETIZER = BLOCKS.register("magnetizer",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.MAGNETIZER, net.scwunge.rotarycraft.blockentity.MagnetizerBlockEntity::new));
    public static final DeferredBlock<MachineBlock> FRACTIONATOR = BLOCKS.register("fractionator",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.FRACTIONATOR, net.scwunge.rotarycraft.blockentity.FractionatorBlockEntity::new));
    public static final DeferredBlock<MachineBlock> PUMP = BLOCKS.register("pump",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.PUMP, net.scwunge.rotarycraft.blockentity.PumpBlockEntity::new));
    public static final DeferredBlock<net.scwunge.rotarycraft.block.ReservoirBlock> RESERVOIR = BLOCKS.register("reservoir",
            () -> new net.scwunge.rotarycraft.block.ReservoirBlock(machineProps().noOcclusion()));
    public static final DeferredBlock<MachineBlock> ROCK_MELTER = BLOCKS.register("rock_melter",
            () -> new MachineBlock(machineProps(), RotaryBlockEntities.ROCK_MELTER, net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity::new));
    /** Pipes by kind (hose, pipe, fuel line, bedrock pipe). */
    public static final Map<net.scwunge.rotarycraft.pipe.PipeType, DeferredBlock<net.scwunge.rotarycraft.block.PipeBlock>> PIPES = new EnumMap<>(net.scwunge.rotarycraft.pipe.PipeType.class);

    static {
        for (net.scwunge.rotarycraft.pipe.PipeType t : net.scwunge.rotarycraft.pipe.PipeType.values()) {
            PIPES.put(t, BLOCKS.register(t.id, () -> new net.scwunge.rotarycraft.block.PipeBlock(pipeProps(t), t)));
        }
    }

    static BlockBehaviour.Properties pipeProps(net.scwunge.rotarycraft.pipe.PipeType t) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().noOcclusion().strength(1.0F, 4.0F);
        return switch (t) {
            case HOSE -> p.mapColor(MapColor.WOOD).sound(SoundType.WOOD);
            case FUEL_LINE -> p.mapColor(MapColor.COLOR_BLACK).sound(SoundType.STONE).requiresCorrectToolForDrops();
            case BEDROCK -> p.mapColor(MapColor.STONE).strength(5.0F, 3600000F).sound(SoundType.STONE).requiresCorrectToolForDrops();
            default -> p.mapColor(MapColor.METAL).sound(SoundType.METAL).requiresCorrectToolForDrops();
        };
    }

    public static final DeferredBlock<net.scwunge.rotarycraft.block.CanolaBlock> CANOLA = BLOCKS.register("canola",
            () -> new net.scwunge.rotarycraft.block.CanolaBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WHEAT)));
    public static final DeferredBlock<DynamometerBlock> DYNAMOMETER = BLOCKS.register("dynamometer",
            () -> new DynamometerBlock(machineProps()));

    public static DeferredBlock<GearboxBlock> gearbox(ShaftMaterial m, int ratio) {
        return GEARBOXES.get(m).get(ratio);
    }

    public static java.util.stream.Stream<DeferredBlock<GearboxBlock>> allGearboxes() {
        return GEARBOXES.values().stream().flatMap(byRatio -> byRatio.values().stream());
    }

    static BlockBehaviour.Properties gearboxProps(ShaftMaterial m) {
        return switch (m) {
            case WOOD -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD);
            case STONE -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.5F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
            case BEDROCK -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F, 3600000F).sound(SoundType.METAL).requiresCorrectToolForDrops();
            default -> machineProps();
        };
    }

    static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    static BlockBehaviour.Properties shaftProps(ShaftMaterial m) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().noOcclusion();
        return switch (m) {
            case WOOD -> p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD);
            case STONE -> p.mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
            case BEDROCK -> p.mapColor(MapColor.STONE).strength(50.0F, 3600000.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
            default -> p.mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
        };
    }
}
