package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.FlywheelBlock;
import net.scwunge.rotarycraft.block.GearboxBlock;
import net.scwunge.rotarycraft.blockentity.ACEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.ClutchBlockEntity;
import net.scwunge.rotarycraft.blockentity.CentrifugeBlockEntity;
import net.scwunge.rotarycraft.blockentity.CompactorBlockEntity;
import net.scwunge.rotarycraft.blockentity.CrystallizerBlockEntity;
import net.scwunge.rotarycraft.blockentity.DCEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.blockentity.FlywheelBlockEntity;
import net.scwunge.rotarycraft.blockentity.FractionatorBlockEntity;
import net.scwunge.rotarycraft.blockentity.FrictionHeaterBlockEntity;
import net.scwunge.rotarycraft.blockentity.GasEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.GearboxBlockEntity;
import net.scwunge.rotarycraft.blockentity.GrinderBlockEntity;
import net.scwunge.rotarycraft.blockentity.HydroEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.JetEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.MagnetizerBlockEntity;
import net.scwunge.rotarycraft.blockentity.MicroturbineBlockEntity;
import net.scwunge.rotarycraft.blockentity.PerformanceEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.blockentity.PulseFurnaceBlockEntity;
import net.scwunge.rotarycraft.blockentity.PumpBlockEntity;
import net.scwunge.rotarycraft.blockentity.RefrigeratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity;
import net.scwunge.rotarycraft.blockentity.SteamEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.WindEngineBlockEntity;
import net.scwunge.rotarycraft.client.machine.MachineRenderer.Look;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryBlocks;

import java.util.function.Supplier;

/**
 * The machines that were first drawn as plain cubes, drawn from the original's models now, with the moving parts of each (tools/gen_models.py makes
 * the models and the textures). Each model turns by {@code pow(log2(omega + 1), p)} degrees a tick while its shaft turns, as the original's did.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModelMachinesClient {
    private ModelMachinesClient() {}

    /** The original's renderers' rotations for the machines whose models were turned to face their shafts. */
    static final float[] FLYWHEEL = {180, 0, 270, 90};
    static final float[] COMPACTOR = {180, 0, 270, 90};
    static final float[] PULSE = {180, 0, 270, 90};
    static final float[] FRIDGE = {270, 90, 0, 180};
    static final float[] EXTRACTOR = {90, 270, 0, 180};
    static final float[] PUMP = {90, 90, 0, 0};

    /** The turn of the parts each tick, in degrees, while the machine's shaft turns. */
    static double spin(PowerBlockEntity be, double power) {
        return be.getOmega() <= 0 ? 0 : Math.pow(Math.log(be.getOmega() + 1) / Math.log(2), power);
    }

    static <T extends PowerBlockEntity> Look<T> engine(String model, String texture) {
        return Look.<T>spinning(model, texture, MachineRenderer.ENGINE, be -> spin(be, 1.05), -1);
    }

    static final Look<DCEngineBlockEntity> DC = engine("dc_engine", "engine_dc");
    static final Look<ACEngineBlockEntity> AC = engine("ac_engine", "engine_ac");
    static final Look<GasEngineBlockEntity> GAS = engine("gas_engine", "engine_gas");
    static final Look<PerformanceEngineBlockEntity> PERFORMANCE = engine("performance_engine", "engine_performance");
    static final Look<SteamEngineBlockEntity> STEAM = engine("steam_engine", "engine_steam");
    static final Look<HydroEngineBlockEntity> HYDRO = engine("hydro_engine", "engine_hydro");
    static final Look<WindEngineBlockEntity> WIND = engine("wind_engine", "engine_wind");
    static final Look<MicroturbineBlockEntity> MICRO = engine("microturbine", "engine_micro");
    static final Look<JetEngineBlockEntity> JET = Look.<JetEngineBlockEntity>spinning("jet_engine", "engine_jet", MachineRenderer.ENGINE, be -> spin(be, 1.1), -1)
            .turned(90);

    static final Look<ClutchBlockEntity> CLUTCH = Look.<ClutchBlockEntity>spinning("clutch", "clutch", MachineRenderer.ENGINE, be -> spin(be, 1.05), -1)
            .modelled(be -> be.facing().getAxis().isVertical() ? "vclutch" : "clutch");
    static final Look<FlywheelBlockEntity> FLYWHEEL_LOOK = Look.<FlywheelBlockEntity>spinning("flywheel", "flywheel_iron", FLYWHEEL, be -> spin(be, 1.05), -1)
            .textured(be -> "flywheel_" + (be.getBlockState().getBlock() instanceof FlywheelBlock f ? f.type().id() : "iron"));
    static final Look<GearboxBlockEntity> GEARBOX = Look.<GearboxBlockEntity>spinning("gearbox_2", "gearbox_wood", MachineRenderer.ENGINE, be -> spin(be, 1.05), -1)
            .modelled(be -> "gearbox_" + (be.getBlockState().getBlock() instanceof GearboxBlock g ? g.ratio() : 2))
            .textured(be -> "gearbox_" + (be.getBlockState().getBlock() instanceof GearboxBlock g ? g.material().id() : "wood"));

    static final Look<PumpBlockEntity> PUMP_LOOK = Look.spinning("pump", "pump", PUMP, be -> spin(be, 1.05), -1);
    static final Look<FrictionHeaterBlockEntity> FRICTION = Look.spinning("friction_heater", "friction_heater", MachineRenderer.BEAM, be -> spin(be, 1.05), -1);
    static final Look<CrystallizerBlockEntity> CRYSTALLIZER = Look.spinning("crystallizer", "crystallizer", MachineRenderer.BEAM, be -> spin(be, 1.05), -1);
    static final Look<RefrigeratorBlockEntity> FRIDGE_LOOK = Look.spinning("refrigerator", "refrigerator", ModelMachinesClient.FRIDGE, be -> spin(be, 1.05), -1);
    static final Look<GrinderBlockEntity> GRINDER = Look.spinning("grinder", "grinder", MachineRenderer.BEAM, be -> spin(be, 1.05), -1);
    static final Look<MagnetizerBlockEntity> MAGNETIZER = Look.spinning("magnetizer", "magnetizer", MachineRenderer.BEAM, be -> spin(be, 1.05), -1);
    static final Look<FractionatorBlockEntity> FRACTIONATOR = Look.still("fractionator", "fractionator", null);
    static final Look<PulseFurnaceBlockEntity> PULSE_FURNACE = Look.still("pulse_furnace", "pulse_furnace", PULSE);
    static final Look<RockMelterBlockEntity> ROCK_MELTER = Look.spinning("rock_melter", "rock_melter", EXTRACTOR, be -> spin(be, 1.05), -1);
    static final Look<ExtractorBlockEntity> EXTRACTOR_LOOK = Look.still("extractor", "extractor", EXTRACTOR);
    static final Look<CompactorBlockEntity> COMPACTOR_LOOK = Look.spinning("compactor", "compactor", COMPACTOR, be -> spin(be, 1.05), 1);
    static final Look<CentrifugeBlockEntity> CENTRIFUGE = Look.spinning("centrifuge", "centrifuge", null, be -> spin(be, 1.05), 1);

    private static <T extends PowerBlockEntity> void renderer(EntityRenderersEvent.RegisterRenderers event, Supplier<BlockEntityType<T>> type, Look<T> look) {
        event.registerBlockEntityRenderer(type.get(), c -> new MachineRenderer<>(look));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        renderer(event, RotaryBlockEntities.DC_ENGINE::get, DC);
        renderer(event, RotaryBlockEntities.AC_ENGINE::get, AC);
        renderer(event, RotaryBlockEntities.GAS_ENGINE::get, GAS);
        renderer(event, RotaryBlockEntities.PERFORMANCE_ENGINE::get, PERFORMANCE);
        renderer(event, RotaryBlockEntities.STEAM_ENGINE::get, STEAM);
        renderer(event, RotaryBlockEntities.HYDRO_ENGINE::get, HYDRO);
        renderer(event, RotaryBlockEntities.WIND_ENGINE::get, WIND);
        renderer(event, RotaryBlockEntities.MICROTURBINE::get, MICRO);
        renderer(event, RotaryBlockEntities.JET_ENGINE::get, JET);
        renderer(event, RotaryBlockEntities.CLUTCH::get, CLUTCH);
        renderer(event, RotaryBlockEntities.FLYWHEEL::get, FLYWHEEL_LOOK);
        renderer(event, RotaryBlockEntities.GEARBOX::get, GEARBOX);
        renderer(event, RotaryBlockEntities.PUMP::get, PUMP_LOOK);
        renderer(event, RotaryBlockEntities.FRICTION_HEATER::get, FRICTION);
        renderer(event, RotaryBlockEntities.CRYSTALLIZER::get, CRYSTALLIZER);
        renderer(event, RotaryBlockEntities.REFRIGERATOR::get, FRIDGE_LOOK);
        renderer(event, RotaryBlockEntities.GRINDER::get, GRINDER);
        renderer(event, RotaryBlockEntities.MAGNETIZER::get, MAGNETIZER);
        renderer(event, RotaryBlockEntities.FRACTIONATOR::get, FRACTIONATOR);
        renderer(event, RotaryBlockEntities.PULSE_FURNACE::get, PULSE_FURNACE);
        renderer(event, RotaryBlockEntities.ROCK_MELTER::get, ROCK_MELTER);
        renderer(event, RotaryBlockEntities.EXTRACTOR::get, EXTRACTOR_LOOK);
        renderer(event, RotaryBlockEntities.COMPACTOR::get, COMPACTOR_LOOK);
        renderer(event, RotaryBlockEntities.CENTRIFUGE::get, CENTRIFUGE);
        event.registerBlockEntityRenderer(RotaryBlockEntities.BEVEL_GEAR.get(), c -> new JunctionRenderers.Bevel());
        event.registerBlockEntityRenderer(RotaryBlockEntities.SPLITTER.get(), c -> new JunctionRenderers.Splitter());
    }

    private static <T extends PowerBlockEntity> void item(RegisterClientExtensionsEvent event, DeferredBlock<? extends Block> block, Look<T> look, String model,
                                                          String texture) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new MachineRenderer.Item<>(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), new MachineRenderer<>(look), model, texture);
                }
                return renderer;
            }
        }, block.get().asItem());
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        item(event, RotaryBlocks.DC_ENGINE, DC, "dc_engine", "engine_dc");
        item(event, RotaryBlocks.AC_ENGINE, AC, "ac_engine", "engine_ac");
        item(event, RotaryBlocks.GAS_ENGINE, GAS, "gas_engine", "engine_gas");
        item(event, RotaryBlocks.PERFORMANCE_ENGINE, PERFORMANCE, "performance_engine", "engine_performance");
        item(event, RotaryBlocks.STEAM_ENGINE, STEAM, "steam_engine", "engine_steam");
        item(event, RotaryBlocks.HYDRO_ENGINE, HYDRO, "hydro_engine", "engine_hydro");
        item(event, RotaryBlocks.WIND_ENGINE, WIND, "wind_engine", "engine_wind");
        item(event, RotaryBlocks.MICROTURBINE, MICRO, "microturbine", "engine_micro");
        item(event, RotaryBlocks.JET_ENGINE, JET, "jet_engine", "engine_jet");
        item(event, RotaryBlocks.CLUTCH, CLUTCH, "clutch", "clutch");
        RotaryBlocks.FLYWHEELS.forEach((type, block) -> item(event, block, FLYWHEEL_LOOK, "flywheel", "flywheel_" + type.id()));
        RotaryBlocks.GEARBOXES.forEach((material, byRatio) -> byRatio.forEach((ratio, block) -> item(event, block, GEARBOX, "gearbox_" + ratio, "gearbox_" + material.id())));
        item(event, RotaryBlocks.PUMP, PUMP_LOOK, "pump", "pump");
        item(event, RotaryBlocks.FRICTION_HEATER, FRICTION, "friction_heater", "friction_heater");
        item(event, RotaryBlocks.CRYSTALLIZER, CRYSTALLIZER, "crystallizer", "crystallizer");
        item(event, RotaryBlocks.REFRIGERATOR, FRIDGE_LOOK, "refrigerator", "refrigerator");
        item(event, RotaryBlocks.GRINDER, GRINDER, "grinder", "grinder");
        item(event, RotaryBlocks.MAGNETIZER, MAGNETIZER, "magnetizer", "magnetizer");
        item(event, RotaryBlocks.FRACTIONATOR, FRACTIONATOR, "fractionator", "fractionator");
        item(event, RotaryBlocks.PULSE_FURNACE, PULSE_FURNACE, "pulse_furnace", "pulse_furnace");
        item(event, RotaryBlocks.ROCK_MELTER, ROCK_MELTER, "rock_melter", "rock_melter");
        item(event, RotaryBlocks.EXTRACTOR, EXTRACTOR_LOOK, "extractor", "extractor");
        item(event, RotaryBlocks.COMPACTOR, COMPACTOR_LOOK, "compactor", "compactor");
        item(event, RotaryBlocks.CENTRIFUGE, CENTRIFUGE, "centrifuge", "centrifuge");
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new JunctionRenderers.Bevel.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, RotaryBlocks.BEVEL_GEAR.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new JunctionRenderers.Splitter.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, RotaryBlocks.SPLITTER.get().asItem());
    }
}
