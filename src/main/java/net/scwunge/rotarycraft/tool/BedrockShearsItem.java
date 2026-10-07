package net.scwunge.rotarycraft.tool;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.IShearable;

/**
 * The bedrock shears: never wear, cut webs and wool and plants fast, and take the block itself from anything that can be taken whole, as silk touch would
 * (see {@link ToolEvents}).
 */
public class BedrockShearsItem extends ShearsItem {
    public BedrockShearsItem() {
        super(new Properties().stacksTo(1).component(DataComponents.TOOL, ShearsItem.createToolProperties()).component(DataComponents.UNBREAKABLE, new Unbreakable(true)));
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (state.is(Blocks.COBWEB)) {
            return 40F;
        }
        if (state.is(BlockTags.WOOL)) {
            return 16F;
        }
        if (state.getBlock() instanceof IShearable || state.getBlock() instanceof BushBlock || state.is(BlockTags.LEAVES)) {
            return 8F;
        }
        return 0.75F;
    }
}
