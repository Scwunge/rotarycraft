package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.logistics.AggregatorBlockEntity;
import net.scwunge.rotarycraft.logistics.FillingStationBlockEntity;
import net.scwunge.rotarycraft.logistics.GrindstoneBlockEntity;
import net.scwunge.rotarycraft.logistics.HeaterBlockEntity;
import net.scwunge.rotarycraft.logistics.SpillwayBlockEntity;
import net.scwunge.rotarycraft.logistics.WetterBlockEntity;
import net.scwunge.rotarycraft.logistics.PlayerDetectorBlockEntity;
import net.scwunge.rotarycraft.logistics.SmokeDetectorBlockEntity;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

/** Screens, renderers and item renderers of the automation and processing machines. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LogisticsClient {
    private static final ModelMachineRenderer<PlayerDetectorBlockEntity> PLAYER_DETECTOR = new ModelMachineRenderer<>("player_detector", "player_detector");
    private static final ModelMachineRenderer<SmokeDetectorBlockEntity> SMOKE_DETECTOR = new ModelMachineRenderer<>("smoke_detector", "smoke_detector");
    private static final HeaterRenderer HEATER = new HeaterRenderer();
    private static final SpinningRenderer<WetterBlockEntity> WETTER = new SpinningRenderer<>("wetter", "wetter", new String[] {"Shape5", "Shape5a", "Shape5b", "Shape5c", "Shape5d"},
            com.mojang.math.Axis.YP, 0, SpinningRenderer::noYaw);
    private static final SpinningRenderer<GrindstoneBlockEntity> GRINDSTONE = new SpinningRenderer<>("grindstone", "grindstone",
            new String[] {"Shape2", "Shape2a", "Shape2b", "Shape2c", "Shape2d", "Shape2e", "Shape2f", "Shape2g", "Shape2h", "Shape4", "Shape4a"}, com.mojang.math.Axis.ZP, 0.9375,
            SpinningRenderer::axisYaw);
    private static final SpinningRenderer<AggregatorBlockEntity> AGGREGATOR = new SpinningRenderer<>("aggregator", "aggregator",
            new String[] {"Shape1", "Shape1a", "Shape1b", "Shape1c", "Shape1d", "Shape1e", "Shape1f", "Shape1g", "Shape2", "Shape2a", "Shape3", "Shape3a", "Shape3b", "Shape3c",
                    "Shape3d", "Shape3e", "Shape3f", "Shape3g"}, com.mojang.math.Axis.YP, 0, SpinningRenderer::noYaw);
    private static final ModelMachineRenderer<SpillwayBlockEntity> SPILLWAY = new ModelMachineRenderer<>("spillway", "spillway");
    private static final ModelMachineRenderer<FillingStationBlockEntity> FILLING_STATION = new ModelMachineRenderer<>("filling_station", "filling_station");

    private LogisticsClient() {}

    /** The heater, in the texture its temperature has earned. */
    private static final class HeaterRenderer extends ModelMachineRenderer<HeaterBlockEntity> {
        private static final ResourceLocation[] TEXTURES = new ResourceLocation[6];

        static {
            for (int i = 0; i < TEXTURES.length; i++) {
                TEXTURES[i] = RotaryCraft.id("textures/machine/heater_" + i + ".png");
            }
        }

        HeaterRenderer() {
            super("heater", "heater_0");
        }

        @Override
        protected ResourceLocation textureFor(HeaterBlockEntity heater) {
            return heater == null ? TEXTURES[0] : TEXTURES[HeaterBlockEntity.glowTier(heater.getTemperature())];
        }
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(LogisticsRegistry.PLAYER_DETECTOR.type().get(), c -> PLAYER_DETECTOR);
        event.registerBlockEntityRenderer(LogisticsRegistry.SMOKE_DETECTOR.type().get(), c -> SMOKE_DETECTOR);
        event.registerBlockEntityRenderer(LogisticsRegistry.HEATER.type().get(), c -> HEATER);
        event.registerBlockEntityRenderer(LogisticsRegistry.WETTER.type().get(), c -> WETTER);
        event.registerBlockEntityRenderer(LogisticsRegistry.GRINDSTONE.type().get(), c -> GRINDSTONE);
        event.registerBlockEntityRenderer(LogisticsRegistry.AGGREGATOR.type().get(), c -> AGGREGATOR);
        event.registerBlockEntityRenderer(LogisticsRegistry.FILLING_STATION.type().get(), c -> FILLING_STATION);
        event.registerBlockEntityRenderer(LogisticsRegistry.SPILLWAY.type().get(), c -> SPILLWAY);
        event.registerBlockEntityRenderer(LogisticsRegistry.SCALE_CHEST.type().get(), ScaleChestRenderer::new);
    }

    private static IClientItemExtensions modelItem(ModelMachineRenderer<?> renderer) {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer item;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (item == null) {
                    Minecraft mc = Minecraft.getInstance();
                    item = new ModelMachineRenderer.ItemForm<>(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), renderer);
                }
                return item;
            }
        };
    }

    @SubscribeEvent
    public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(LogisticsRegistry.SCALE_CHEST_MENU.get(), ScaleChestScreen::new);
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(modelItem(PLAYER_DETECTOR), LogisticsRegistry.PLAYER_DETECTOR.block().get().asItem());
        event.registerItem(modelItem(SMOKE_DETECTOR), LogisticsRegistry.SMOKE_DETECTOR.block().get().asItem());
        event.registerItem(modelItem(HEATER), LogisticsRegistry.HEATER.block().get().asItem());
        event.registerItem(modelItem(WETTER), LogisticsRegistry.WETTER.block().get().asItem());
        event.registerItem(modelItem(GRINDSTONE), LogisticsRegistry.GRINDSTONE.block().get().asItem());
        event.registerItem(modelItem(AGGREGATOR), LogisticsRegistry.AGGREGATOR.block().get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer item;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (item == null) {
                    Minecraft mc = Minecraft.getInstance();
                    item = new ScaleChestRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return item;
            }
        }, LogisticsRegistry.SCALE_CHEST.block().get().asItem());
        event.registerItem(modelItem(SPILLWAY), LogisticsRegistry.SPILLWAY.block().get().asItem());
        event.registerItem(modelItem(FILLING_STATION), LogisticsRegistry.FILLING_STATION.block().get().asItem());
    }
}
