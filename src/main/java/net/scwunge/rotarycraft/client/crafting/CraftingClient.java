package net.scwunge.rotarycraft.client.crafting;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.CraftingRegistry;

/** Screens of the Worktable, the Auto-Crafter and the Craft Pattern. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CraftingClient {
    private CraftingClient() {}

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(CraftingRegistry.WORKTABLE_MENU.get(), WorktableScreen::new);
        event.register(CraftingRegistry.AUTO_CRAFTER_MENU.get(), AutoCrafterScreen::new);
        event.register(CraftingRegistry.CRAFT_PATTERN_MENU.get(), CraftPatternScreen::new);
    }
}
