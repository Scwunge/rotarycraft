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
    static final StaticModelRenderer<net.scwunge.rotarycraft.survey.SpyCamBlockEntity> SPY_CAM = new StaticModelRenderer<>("spy_cam", "spy_cam", 0);
    static final StaticModelRenderer<net.scwunge.rotarycraft.survey.ScreenBlockEntity> SCREEN = new StaticModelRenderer<>("screen", "cctv_screen", 0);

    private SurveyClient() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SurveyRegistry.MOB_RADAR_BE.get(), c -> new MobRadarRenderer());
        event.registerBlockEntityRenderer(SurveyRegistry.CAVE_SCANNER_BE.get(), c -> new CaveScannerRenderer());
        event.registerBlockEntityRenderer(SurveyRegistry.CCTV_BE.get(), c -> new CctvRenderer());
        event.registerBlockEntityRenderer(SurveyRegistry.DISPLAY_BE.get(), c -> new DisplayRenderer());
        event.registerBlockEntityRenderer(SurveyRegistry.PROJECTOR_BE.get(), c -> new ProjectorRenderer());
        event.registerBlockEntityRenderer(SurveyRegistry.SPY_CAM_BE.get(), c -> SPY_CAM);
        event.registerBlockEntityRenderer(SurveyRegistry.SCREEN_BE.get(), c -> SCREEN);
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(SurveyRegistry.RADAR_MENU.get(), RadarScreen::new);
        event.register(SurveyRegistry.GPR_MENU.get(), GprScreen::new);
        event.register(SurveyRegistry.REMOTE_MENU.get(), SurveyClient::remoteScreen);
        event.register(SurveyRegistry.SCREEN_MENU.get(), SurveyClient::screenScreen);
        event.register(SurveyRegistry.SPY_CAM_MENU.get(), SpyCamScreen::new);
        event.register(SurveyRegistry.DISPLAY_MENU.get(), SurveyClient::displayScreen);
        event.register(SurveyRegistry.PROJECTOR_MENU.get(), SurveyClient::projectorScreen);
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
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new CctvRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, SurveyRegistry.CCTV.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new DisplayRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, SurveyRegistry.DISPLAY.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new ProjectorRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, SurveyRegistry.PROJECTOR.get().asItem());
        event.registerItem(staticItem(SPY_CAM), SurveyRegistry.SPY_CAM.get().asItem());
        event.registerItem(staticItem(SCREEN), SurveyRegistry.SCREEN.get().asItem());
    }

    private static IClientItemExtensions staticItem(StaticModelRenderer<?> model) {
        return new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new StaticModelRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), model);
                }
                return renderer;
            }
        };
    }

    private static PanelScreen<net.scwunge.rotarycraft.survey.RemoteMenu> remoteScreen(net.scwunge.rotarycraft.survey.RemoteMenu menu,
            net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
        return new PanelScreen<>(menu, inventory, title, "cctv", null, 0);
    }

    private static PanelScreen<net.scwunge.rotarycraft.survey.ScreenMenu> screenScreen(net.scwunge.rotarycraft.survey.ScreenMenu menu,
            net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
        return new PanelScreen<>(menu, inventory, title, "cctv_screen", net.minecraft.network.chat.Component.translatable("gui.rotarycraft.camera_select"), 54);
    }

    private static PanelScreen<net.scwunge.rotarycraft.survey.CoilMenu> displayScreen(net.scwunge.rotarycraft.survey.CoilMenu menu,
            net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
        return new PanelScreen<>(menu, inventory, title, "one_slot", null, 0);
    }

    private static PanelScreen<net.scwunge.rotarycraft.survey.ProjectorMenu> projectorScreen(net.scwunge.rotarycraft.survey.ProjectorMenu menu,
            net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
        return new PanelScreen<>(menu, inventory, title, "projector", null, 0, 222);
    }
}
