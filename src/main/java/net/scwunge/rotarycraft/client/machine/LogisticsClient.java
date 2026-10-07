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
import net.scwunge.rotarycraft.logistics.HeaterBlockEntity;
import net.scwunge.rotarycraft.logistics.PlayerDetectorBlockEntity;
import net.scwunge.rotarycraft.logistics.SmokeDetectorBlockEntity;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

/** Screens, renderers and item renderers of the automation and processing machines. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LogisticsClient {
    private static final ModelMachineRenderer<PlayerDetectorBlockEntity> PLAYER_DETECTOR = new ModelMachineRenderer<>("player_detector", "player_detector");
    private static final ModelMachineRenderer<SmokeDetectorBlockEntity> SMOKE_DETECTOR = new ModelMachineRenderer<>("smoke_detector", "smoke_detector");
    private static final HeaterRenderer HEATER = new HeaterRenderer();

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
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(modelItem(PLAYER_DETECTOR), LogisticsRegistry.PLAYER_DETECTOR.block().get().asItem());
        event.registerItem(modelItem(SMOKE_DETECTOR), LogisticsRegistry.SMOKE_DETECTOR.block().get().asItem());
        event.registerItem(modelItem(HEATER), LogisticsRegistry.HEATER.block().get().asItem());
    }
}
