package net.scwunge.rotarycraft.tool;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;

/** The HSLA steel axe: harvests higher too when the config says so. */
public class SteelAxeItem extends AxeItem {
    public SteelAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return SteelTools.harvestsHigher(state, BlockTags.MINEABLE_WITH_AXE) || super.isCorrectToolForDrops(stack, state);
    }
}
