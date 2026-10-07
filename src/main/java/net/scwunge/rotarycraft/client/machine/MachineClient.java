package net.scwunge.rotarycraft.client.machine;

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
import net.scwunge.rotarycraft.machine.LayoutMenus;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/** Screens, renderers and item renderers of the world, decoration and logistics machines. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MachineClient {
    private static final ObsidianMakerRenderer OBSIDIAN_MAKER = new ObsidianMakerRenderer();
    private static final LineBuilderRenderer LINE_BUILDER = new LineBuilderRenderer();

    private MachineClient() {}

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        LayoutMenus.types().values().forEach(type -> event.register(type.get(), LayoutScreen::new));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DecorRegistry.OBSIDIAN_MAKER.type().get(), c -> OBSIDIAN_MAKER);
        event.registerBlockEntityRenderer(DecorRegistry.LINE_BUILDER.type().get(), c -> LINE_BUILDER);
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
        event.registerItem(modelItem(OBSIDIAN_MAKER), DecorRegistry.OBSIDIAN_MAKER.block().get().asItem());
        event.registerItem(modelItem(LINE_BUILDER), DecorRegistry.LINE_BUILDER.block().get().asItem());
    }
}
