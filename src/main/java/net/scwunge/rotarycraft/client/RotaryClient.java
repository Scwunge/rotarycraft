package net.scwunge.rotarycraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.scwunge.rotarycraft.RotaryCraft;
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
    }

    /** Extractor products are tinted with their ore's colour. */
    @SubscribeEvent
    public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> {
            var product = stack.get(net.scwunge.rotarycraft.registry.RotaryComponents.ORE_PRODUCT.get());
            return layer == 0 && product != null ? 0xFF000000 | product.color() : -1;
        }, net.scwunge.rotarycraft.registry.RotaryItems.ORE_DUST.get(), net.scwunge.rotarycraft.registry.RotaryItems.ORE_SLURRY.get(),
                net.scwunge.rotarycraft.registry.RotaryItems.ORE_SOLUTION.get(), net.scwunge.rotarycraft.registry.RotaryItems.ORE_FLAKES.get());
    }
}
