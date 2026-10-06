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
    public static final Map<Integer, DeferredBlock<GearboxBlock>> GEARBOXES = new LinkedHashMap<>();
    public static final Map<FlywheelType, DeferredBlock<FlywheelBlock>> FLYWHEELS = new EnumMap<>(FlywheelType.class);

    static {
        for (ShaftMaterial m : SHAFT_MATERIALS) {
            SHAFTS.put(m, BLOCKS.register("shaft_" + m.id(), () -> new ShaftBlock(shaftProps(m), m)));
        }
        for (FlywheelType t : FlywheelType.values()) {
            FLYWHEELS.put(t, BLOCKS.register("flywheel_" + t.id(), () -> new FlywheelBlock(machineProps(), t)));
        }
        for (int ratio : GEARBOX_RATIOS) {
            GEARBOXES.put(ratio, BLOCKS.register("gearbox_" + ratio + "x", () -> new GearboxBlock(machineProps(), ratio)));
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
    public static final DeferredBlock<DynamometerBlock> DYNAMOMETER = BLOCKS.register("dynamometer",
            () -> new DynamometerBlock(machineProps()));

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
