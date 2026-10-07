package net.scwunge.rotarycraft.client.farm;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.farm.FanBlockEntity;
import net.scwunge.rotarycraft.farm.FarmBlock;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.farm.FertilizerBlockEntity;
import net.scwunge.rotarycraft.farm.GroundHydratorBlockEntity;
import net.scwunge.rotarycraft.farm.LawnSprinklerBlockEntity;
import net.scwunge.rotarycraft.farm.SprinklerBlockEntity;
import net.scwunge.rotarycraft.registry.FarmRegistry;

import java.util.Set;

/** Renderers, item renderers and screens of the farming and automation machines. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FarmClient {
    private FarmClient() {}

    /** The turn of a spinning part: degrees a tick, from the speed of the shaft (the original's formula). */
    private static double spin(int omega, double scale) {
        return scale * Math.pow(Math.log(omega + 1) / Math.log(2), 1.05);
    }

    static final FarmRenderer.Look<FanBlockEntity> FAN = new FarmRenderer.Look<>("fan", be -> be.isWide() ? "fan_wide" : "fan",
            Set.of("Shape1", "Shape5", "Shape6", "Shape7", "Shape8", "Shape9", "Shape3", "Shape4", "Shape2"), Axis.ZP, 1, true,
            be -> be.getPower() < FanBlockEntity.REQUIREMENT.minPower() ? 0 : -spin(be.getOmega(), 3));
    static final FarmRenderer.Look<SprinklerBlockEntity> SPRINKLER = FarmRenderer.Look.still("sprinkler", "sprinkler");
    static final FarmRenderer.Look<LawnSprinklerBlockEntity> LAWN_SPRINKLER = new FarmRenderer.Look<>("lawn_sprinkler", be -> "lawn_sprinkler",
            Set.of("Shape1"), Axis.YP, 0, false, be -> be.isWorking() ? 24 : 0);
    static final FarmRenderer.Look<GroundHydratorBlockEntity> HYDRATOR = FarmRenderer.Look.still("reservoir", "ground_hydrator");
    static final FarmRenderer.Look<FertilizerBlockEntity> FERTILIZER = new FarmRenderer.Look<>("fertilizer", be -> "fertilizer", Set.of("Shape2", "Shape2a"),
            Axis.YP, 0, false, be -> be.getPower() < FertilizerBlockEntity.REQUIREMENT.minPower() ? 0 : spin(be.getOmega(), 1));

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FarmRegistry.FAN_BE.get(), c -> new FarmRenderer<>(FAN));
        event.registerBlockEntityRenderer(FarmRegistry.SPRINKLER_BE.get(), c -> new FarmRenderer<>(SPRINKLER));
        event.registerBlockEntityRenderer(FarmRegistry.LAWN_SPRINKLER_BE.get(), c -> new FarmRenderer<>(LAWN_SPRINKLER));
        event.registerBlockEntityRenderer(FarmRegistry.GROUND_HYDRATOR_BE.get(), c -> new FarmRenderer<>(HYDRATOR));
        event.registerBlockEntityRenderer(FarmRegistry.FERTILIZER_BE.get(), c -> new FarmRenderer<>(FERTILIZER));
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(FarmRegistry.FARM_MENU.get(), FarmScreen::new);
    }

    private static <T extends FarmBlockEntity> void item(RegisterClientExtensionsEvent event, DeferredBlock<FarmBlock> block, FarmRenderer.Look<T> look,
                                                         String texture) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new FarmRenderer.Item<>(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), new FarmRenderer<>(look), texture);
                }
                return renderer;
            }
        }, block.get().asItem());
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        item(event, FarmRegistry.FAN, FAN, "fan");
        item(event, FarmRegistry.SPRINKLER, SPRINKLER, "sprinkler");
        item(event, FarmRegistry.LAWN_SPRINKLER, LAWN_SPRINKLER, "lawn_sprinkler");
        item(event, FarmRegistry.GROUND_HYDRATOR, HYDRATOR, "ground_hydrator");
        item(event, FarmRegistry.FERTILIZER, FERTILIZER, "fertilizer");
    }
}
