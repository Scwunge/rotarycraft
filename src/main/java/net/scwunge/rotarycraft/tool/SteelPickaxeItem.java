package net.scwunge.rotarycraft.tool;

import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

/** The HSLA steel pickaxe: an iron one that digs a fifth faster, and breaks glass quickly (the original's). */
public class SteelPickaxeItem extends PickaxeItem {
    public SteelPickaxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return SteelTools.harvestsHigher(state, net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) || super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)) {
            return 8F;
        }
        return super.getDestroySpeed(stack, state);
    }
}
