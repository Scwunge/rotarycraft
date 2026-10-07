package net.scwunge.rotarycraft.client.process;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.RotaryComponents;

/** What the process machines' items say about what they hold. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, value = Dist.CLIENT)
public final class ProcessTooltips {
    private ProcessTooltips() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        SimpleFluidContent held = event.getItemStack().get(RotaryComponents.GAS_CONTENTS.get());
        if (held != null && !held.isEmpty()) {
            var fluid = held.copy();
            event.getToolTip().add(Component.translatable("tooltip.rotarycraft.gas_contents", String.format("%,d", fluid.getAmount() / 1000), fluid.getHoverName()));
        }
    }
}
