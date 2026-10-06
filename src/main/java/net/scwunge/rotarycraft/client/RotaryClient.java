package net.scwunge.rotarycraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Client-only setup. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RotaryClient {
    private RotaryClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(RotaryMenus.GRINDER.get(), GrinderScreen::new);
        event.register(RotaryMenus.EXTRACTOR.get(), ExtractorScreen::new);
        event.register(RotaryMenus.BLAST_FURNACE.get(), BlastFurnaceScreen::new);
        event.register(RotaryMenus.FERMENTER.get(), FermenterScreen::new);
        event.register(RotaryMenus.CENTRIFUGE.get(), CentrifugeScreen::new);
        event.register(RotaryMenus.ROCK_MELTER.get(), RockMelterScreen::new);
        event.register(RotaryMenus.FRACTIONATOR.get(), FractionatorScreen::new);
        event.register(RotaryMenus.COMPACTOR.get(), CompactorScreen::new);
        event.register(RotaryMenus.FUEL_ENGINE.get(), FuelEngineScreen::new);
        event.register(RotaryMenus.PERFORMANCE_ENGINE.get(), PerformanceEngineScreen::new);
        event.register(RotaryMenus.TURBINE.get(), TurbineScreen::new);
        event.register(RotaryMenus.JET_ENGINE.get(), TurbineScreen::new);
        event.register(RotaryMenus.MAGNETIZER.get(), OneSlotScreen::new);
        event.register(RotaryMenus.AC_ENGINE.get(), OneSlotScreen::new);
    }

    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");

    /** The mod's fluids use the (grey) water textures tinted with their own colour. */
    @SubscribeEvent
    public static void fluidRendering(RegisterClientExtensionsEvent event) {
        for (RotaryFluids.Entry f : RotaryFluids.ALL) {
            int tint = 0xFF000000 | f.color;
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return WATER_STILL;
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return WATER_FLOW;
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return WATER_OVERLAY;
                }

                @Override
                public int getTintColor() {
                    return tint;
                }
            }, f.type.get());
        }
    }

    @SubscribeEvent
    public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(net.scwunge.rotarycraft.registry.RotaryBlockEntities.RESERVOIR.get(), ReservoirRenderer::new);
    }

    @SubscribeEvent
    public static void translucentFluids(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(net.scwunge.rotarycraft.registry.RotaryBlocks.CANOLA.get(), RenderType.cutout());
        net.scwunge.rotarycraft.registry.RotaryBlocks.PIPES.values().forEach(b -> ItemBlockRenderTypes.setRenderLayer(b.get(), RenderType.cutout()));
        for (RotaryFluids.Entry f : RotaryFluids.ALL) {
            ItemBlockRenderTypes.setRenderLayer(f.source.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(f.flowing.get(), RenderType.translucent());
        }
    }

    /** Extractor products are tinted with their ore's colour. */
    @SubscribeEvent
    public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            var product = stack.get(net.scwunge.rotarycraft.registry.RotaryComponents.ORE_PRODUCT.get());
            return layer == 0 && product != null ? 0xFF000000 | product.color() : -1;
        }, net.scwunge.rotarycraft.registry.RotaryItems.ORE_DUST.get(), net.scwunge.rotarycraft.registry.RotaryItems.ORE_SLURRY.get(),
                net.scwunge.rotarycraft.registry.RotaryItems.ORE_SOLUTION.get(), net.scwunge.rotarycraft.registry.RotaryItems.ORE_FLAKES.get());
        event.register(new DynamicFluidContainerModel.Colors(), RotaryFluids.ALL.stream().map(f -> f.bucket.get()).toArray(net.minecraft.world.level.ItemLike[]::new));
    }
}
