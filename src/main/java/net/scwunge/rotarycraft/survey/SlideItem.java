package net.scwunge.rotarycraft.survey;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** One of the original's 24 projector slides: the picture it carries is its number (the slide's own file is textures/projector/image N). */
public class SlideItem extends Item {
    public static final int COUNT = 24;

    private final int index;

    public SlideItem(Properties props, int index) {
        super(props.stacksTo(1));
        this.index = index;
    }

    public int index() {
        return index;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.rotarycraft.slide", index);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rotarycraft.slide.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
