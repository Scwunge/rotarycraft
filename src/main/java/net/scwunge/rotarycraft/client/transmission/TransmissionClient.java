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

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TransmissionRegistry.MULTI_CLUTCH_BE.get(), c -> new MachineRenderer<>(MULTI_CLUTCH));
        event.registerBlockEntityRenderer(TransmissionRegistry.DISTRIBUTION_CLUTCH_BE.get(), c -> new MachineRenderer<>(DISTRIBUTION_CLUTCH));
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(TransmissionRegistry.MULTI_CLUTCH_MENU.get(), MultiClutchScreen::new);
        event.register(TransmissionRegistry.POWER_BUS_MENU.get(), PowerBusScreen::new);
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
