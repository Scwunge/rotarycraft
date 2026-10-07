package net.scwunge.rotarycraft.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.farm.FarmBlock;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.process.AirCompressorBlockEntity;
import net.scwunge.rotarycraft.process.BoilerBlockEntity;
import net.scwunge.rotarycraft.process.DynamoBlockEntity;
import net.scwunge.rotarycraft.process.MagneticMotorBlockEntity;
import net.scwunge.rotarycraft.process.PneumaticEngineBlockEntity;
import net.scwunge.rotarycraft.process.SteamTurbineBlockEntity;

/**
 * The fluid and process machines: the Boiler and Steam Turbine, the Air Compressor and Pneumatic Engine, the Magnetic Motor and Dynamo, and the others
 * of the original's conversion and processing block. Their blocks, block entities and items go into the mod's shared registers; each has an off
 * switch in rotarycraft-farm.toml.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ProcessRegistry {
    /** Fluids that count as steam: this mod's own, and any other mod's that puts itself in the {@code c:steam} tag. */
    public static final TagKey<Fluid> STEAM_TAG = FluidTags.create(ResourceLocation.fromNamespaceAndPath("c", "steam"));

    /** A machine's block and block entity type. */
    public record Machine<T extends PowerBlockEntity>(DeferredBlock<FarmBlock> block, DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> be) {
    }

    @SuppressWarnings("unchecked")
    static <T extends PowerBlockEntity> Machine<T> machine(String name, BlockEntityType.BlockEntitySupplier<T> factory) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<T>>[] type = new DeferredHolder[1];
        DeferredBlock<FarmBlock> block = RotaryBlocks.BLOCKS.register(name, () -> new FarmBlock(RotaryBlocks.machineProps().noOcclusion(), () -> type[0].get(), factory::create));
        type[0] = RotaryBlockEntities.TYPES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(block));
        return new Machine<>(block, type[0]);
    }

    public static final Machine<BoilerBlockEntity> BOILER_M = machine("boiler", BoilerBlockEntity::new);
    public static final Machine<SteamTurbineBlockEntity> STEAM_TURBINE_M = machine("steam_turbine", SteamTurbineBlockEntity::new);
    public static final Machine<AirCompressorBlockEntity> AIR_COMPRESSOR_M = machine("air_compressor", AirCompressorBlockEntity::new);
    public static final Machine<PneumaticEngineBlockEntity> PNEUMATIC_ENGINE_M = machine("pneumatic_engine", PneumaticEngineBlockEntity::new);
    public static final Machine<MagneticMotorBlockEntity> MAGNETIC_MOTOR_M = machine("magnetic_motor", MagneticMotorBlockEntity::new);
    public static final Machine<DynamoBlockEntity> DYNAMO_M = machine("dynamo", DynamoBlockEntity::new);

    public static final DeferredBlock<FarmBlock> BOILER = BOILER_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER_BE = BOILER_M.be();
    public static final DeferredBlock<FarmBlock> STEAM_TURBINE = STEAM_TURBINE_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE_BE = STEAM_TURBINE_M.be();
    public static final DeferredBlock<FarmBlock> AIR_COMPRESSOR = AIR_COMPRESSOR_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirCompressorBlockEntity>> AIR_COMPRESSOR_BE = AIR_COMPRESSOR_M.be();
    public static final DeferredBlock<FarmBlock> PNEUMATIC_ENGINE = PNEUMATIC_ENGINE_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticEngineBlockEntity>> PNEUMATIC_ENGINE_BE = PNEUMATIC_ENGINE_M.be();
    public static final DeferredBlock<FarmBlock> MAGNETIC_MOTOR = MAGNETIC_MOTOR_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagneticMotorBlockEntity>> MAGNETIC_MOTOR_BE = MAGNETIC_MOTOR_M.be();
    public static final DeferredBlock<FarmBlock> DYNAMO = DYNAMO_M.block();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamoBlockEntity>> DYNAMO_BE = DYNAMO_M.be();

    public static Fluid steam() {
        return RotaryFluids.STEAM.get();
    }

    public static Fluid compressedAir() {
        return RotaryFluids.COMPRESSED_AIR.get();
    }

    private ProcessRegistry() {}

    /** Loads the class, so its entries join the shared registers. */
    public static void init(IEventBus modBus) {
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, BOILER_BE.get(), (be, side) -> be.handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, STEAM_TURBINE_BE.get(), (be, side) -> side == be.facing() ? null : be.steam());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AIR_COMPRESSOR_BE.get(), (be, side) -> side == be.inputSide() ? null : FluidAccess.drainOnly(be.air()));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PNEUMATIC_ENGINE_BE.get(), (be, side) -> side == be.facing() ? null : be.air());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MAGNETIC_MOTOR_BE.get(), (be, side) -> be.energy());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DYNAMO_BE.get(), (be, side) -> side == be.inputSide() ? null : be.energy());
    }
}
