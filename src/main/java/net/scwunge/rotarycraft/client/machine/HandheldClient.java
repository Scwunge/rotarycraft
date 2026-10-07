package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.handheld.FireballLauncherItem;
import net.scwunge.rotarycraft.registry.HandheldRegistry;

/** The handheld tools' screen, and the fireball launcher's icon, which shows how far it is wound up. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HandheldClient {
    private HandheldClient() {}

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent event) {
        event.register(HandheldRegistry.MATCH_FILTER_MENU.get(), MatchFilterScreen::new);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(HandheldRegistry.FIREBALL_LAUNCHER.get(), RotaryCraft.id("blast"), (stack, level, entity, seed) -> {
            if (entity == null || !entity.isUsingItem() || entity.getUseItem() != stack) {
                return 0F;
            }
            boolean creative = entity instanceof Player p && p.isCreative();
            return FireballLauncherItem.level(entity.getTicksUsingItem(), creative, entity.isShiftKeyDown()) / (float) FireballLauncherItem.MAX_LEVEL;
        }));
    }
}
