package net.scwunge.rotarycraft.tool;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.scwunge.rotarycraft.RotaryCraft;

/** Lets the sickles cut around a block as it breaks, after claim and protection mods have had their say on the first one. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public final class ToolEvents {
    private ToolEvents() {}

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void blockBroken(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || !(event.getLevel() instanceof ServerLevel)) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (tool.getItem() instanceof SickleItem sickle && sickle.breakAround(tool, event.getPos(), player)) {
            event.setCanceled(true);
        }
    }
}
