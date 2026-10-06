package net.scwunge.rotarycraft.client.world;

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
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;

/** Renderers, item renderers and screens of the world machines. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WorldMachineClient {
    private WorldMachineClient() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WorldMachineRegistry.CHUNK_LOADER_BE.get(), c -> new ChunkLoaderRenderer());
        event.registerBlockEntityRenderer(WorldMachineRegistry.WEATHER_CONTROLLER_BE.get(), c -> new WeatherControllerRenderer());
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(WorldMachineRegistry.WEATHER_MENU.get(), WeatherScreen::new);
        event.register(WorldMachineRegistry.BORER_MENU.get(), BorerScreen::new);
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new ChunkLoaderRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, WorldMachineRegistry.CHUNK_LOADER.get().asItem());
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new WeatherControllerRenderer.Item(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
                }
                return renderer;
            }
        }, WorldMachineRegistry.WEATHER_CONTROLLER.get().asItem());
    }
}
