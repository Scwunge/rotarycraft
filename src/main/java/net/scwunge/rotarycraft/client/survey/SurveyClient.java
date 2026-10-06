package net.scwunge.rotarycraft.client.survey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.SurveyRegistry;

/** Client setup for the survey machines: renderers, screens and item models. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SurveyClient {
    private SurveyClient() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SurveyRegistry.MOB_RADAR_BE.get(), c -> new MobRadarRenderer());
        event.registerBlockEntityRenderer(SurveyRegistry.CAVE_SCANNER_BE.get(), c -> new CaveScannerRenderer());
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(SurveyRegistry.RADAR_MENU.get(), RadarScreen::new);
        event.register(SurveyRegistry.GPR_MENU.get(), GprScreen::new);
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new MobRadarRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, SurveyRegistry.MOB_RADAR.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new CaveScannerRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, SurveyRegistry.CAVE_SCANNER.get().asItem());
    }
}
