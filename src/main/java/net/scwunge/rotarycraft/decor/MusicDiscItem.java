package net.scwunge.rotarycraft.decor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/** The Music Box Disc (ItemDisk): holds a Music Box's music, put on it with the screen's Save button and read back with Load while it is in the hand. */
public class MusicDiscItem extends Item {
    public MusicDiscItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    /** How many notes the disc holds in each channel. */
    public static int[] counts(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        int[] counts = new int[MusicBoxBlockEntity.CHANNELS];
        if (data != null) {
            CompoundTag tag = data.copyTag();
            for (int i = 0; i < counts.length; i++) {
                counts[i] = tag.getList("ch" + i, Tag.TAG_COMPOUND).size();
            }
        }
        return counts;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int[] counts = counts(stack);
        boolean any = false;
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) {
                if (!any) {
                    tooltip.add(Component.translatable("tooltip.rotarycraft.music_disc.stored").withStyle(ChatFormatting.GRAY));
                    any = true;
                }
                tooltip.add(Component.translatable("tooltip.rotarycraft.music_disc.track", i, counts[i]).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
