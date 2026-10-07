package net.scwunge.rotarycraft.charged;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A tool that runs on {@link Charge}: shows what is left as its bar and in its tooltip. */
public class ChargedItem extends Item implements Rechargeable {
    public ChargedItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * Charge.get(stack) / Charge.FULL);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3FA8FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rotarycraft.charge", Charge.get(stack), Charge.FULL).withStyle(ChatFormatting.GRAY));
    }
}
