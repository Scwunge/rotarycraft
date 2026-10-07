package net.scwunge.rotarycraft.tool;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.state.BlockState;

/** The bedrock shovel: never wears, digs at 24, and turns up extra finds in earth (see {@link ToolEvents}). */
public class BedrockShovelItem extends ShovelItem {
    public BedrockShovelItem() {
        super(BedrockTools.tier(20F), BedrockTools.properties(4, -3.0F));
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_SHOVEL) || !state.requiresCorrectToolForDrops();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_SHOVEL) ? 24F : 1F;
    }
}
