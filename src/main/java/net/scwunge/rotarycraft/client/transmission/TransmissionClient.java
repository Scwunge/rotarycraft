package net.scwunge.rotarycraft.client.transmission;

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
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.client.machine.MachineRenderer;
import net.scwunge.rotarycraft.client.machine.MachineRenderer.Look;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import net.scwunge.rotarycraft.transmission.AdvancedGearBlock;
import net.scwunge.rotarycraft.transmission.AdvancedGearBlockEntity;
import net.scwunge.rotarycraft.transmission.DistributionClutchBlockEntity;
import net.scwunge.rotarycraft.transmission.MultiClutchBlockEntity;

/** Renderers, item renderers and screens of the transmission pieces. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TransmissionClient {
    private TransmissionClient() {}

    /** The turn of the parts each tick, in degrees, while the machine's shaft turns (the original's animation). */
    static double spin(PowerBlockEntity be) {
        return be.getOmega() <= 0 ? 0 : Math.pow(Math.log(be.getOmega() + 1) / Math.log(2), 1.05);
    }

    static final Look<MultiClutchBlockEntity> MULTI_CLUTCH = Look.spinning("multi_clutch", "multi_clutch", null, TransmissionClient::spin, 1);

    static final Look<DistributionClutchBlockEntity> DISTRIBUTION_CLUTCH = Look.spinning("distribution_clutch", "distribution_clutch", MachineRenderer.BEAM, TransmissionClient::spin, 1);

    /** The original's turning of the advanced gears' models for the side their output is on (west, east, north, south). */
    static final float[] GEAR_YAWS = {180, 0, 270, 90};
    static final Look<AdvancedGearBlockEntity> ADVANCED_GEAR = Look.<AdvancedGearBlockEntity>spinning("worm_gear", "clutch", GEAR_YAWS, TransmissionClient::spin, 1)
            .modelled(be -> switch (be.kind()) {
                case CVT -> "cvt";
                case COIL, BEDROCK_COIL -> "coil";
                default -> "worm_gear";
            }).textured(be -> switch (be.kind()) {
                case CVT -> "cvt";
                case COIL -> "coil";
                case BEDROCK_COIL -> "coil_bedrock";
                default -> "clutch";
            });

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TransmissionRegistry.MULTI_CLUTCH_BE.get(), c -> new MachineRenderer<>(MULTI_CLUTCH));
        event.registerBlockEntityRenderer(TransmissionRegistry.ADVANCED_GEAR_BE.get(), c -> new MachineRenderer<>(ADVANCED_GEAR));
        event.registerBlockEntityRenderer(TransmissionRegistry.BELT_HUB_BE.get(), c -> new BeltRenderer());
        event.registerBlockEntityRenderer(TransmissionRegistry.DISTRIBUTION_CLUTCH_BE.get(), c -> new MachineRenderer<>(DISTRIBUTION_CLUTCH));
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(TransmissionRegistry.MULTI_CLUTCH_MENU.get(), MultiClutchScreen::new);
        event.register(TransmissionRegistry.POWER_BUS_MENU.get(), PowerBusScreen::new);
        event.register(TransmissionRegistry.CVT_MENU.get(), CvtScreen::new);
        event.register(TransmissionRegistry.COIL_MENU.get(), CoilScreen::new);
        event.register(TransmissionRegistry.DISTRIBUTION_CLUTCH_MENU.get(), DistributionClutchScreen::new);
    }

    @SubscribeEvent
    public static void items(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new MachineRenderer.Item<>(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), new MachineRenderer<>(MULTI_CLUTCH), "multi_clutch", "multi_clutch");
                }
                return renderer;
            }
        }, TransmissionRegistry.MULTI_CLUTCH.get().asItem());
        TransmissionRegistry.ADVANCED_GEARS.forEach((kind, block) -> event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new MachineRenderer.Item<>(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), new MachineRenderer<>(ADVANCED_GEAR), kind == AdvancedGearBlock.Kind.CVT ? "cvt" : kind.isCoil() ? "coil" : "worm_gear",
                            kind == AdvancedGearBlock.Kind.CVT ? "cvt" : kind == AdvancedGearBlock.Kind.COIL ? "coil" : kind == AdvancedGearBlock.Kind.BEDROCK_COIL ? "coil_bedrock" : "clutch");
                }
                return renderer;
            }
        }, block.get().asItem()));
        for (var block : java.util.List.of(TransmissionRegistry.BELT_HUB, TransmissionRegistry.CHAIN_DRIVE, TransmissionRegistry.SPLIT_BELT)) {
            event.registerItem(new IClientItemExtensions() {
                private BlockEntityWithoutLevelRenderer renderer;

                @Override
                public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) {
                        Minecraft mc = Minecraft.getInstance();
                        renderer = new MachineRenderer.Item<>(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), new BeltRenderer(), "belt_hub", "belt_hub");
                    }
                    return renderer;
                }
            }, block.get().asItem());
        }
        event.registerItem(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new MachineRenderer.Item<>(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels(), new MachineRenderer<>(DISTRIBUTION_CLUTCH), "distribution_clutch", "distribution_clutch");
                }
                return renderer;
            }
        }, TransmissionRegistry.DISTRIBUTION_CLUTCH.get().asItem());
    }
}
