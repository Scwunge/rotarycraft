package net.scwunge.rotarycraft.tool;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The bedrock axe: never wears, fells a whole tree (leaves too) at once unless you sneak (see {@link ToolEvents}), and cannot be silk touch. */
public class BedrockAxeItem extends AxeItem {
    public BedrockAxeItem() {
        super(BedrockTools.tier(12F), BedrockTools.properties(6, -3.0F));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide()) {
            var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            if (stack.getEnchantmentLevel(silk) > 0) {
                EnchantmentHelper.updateEnchantments(stack, e -> e.set(silk, 0));
            }
        }
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE) || !state.requiresCorrectToolForDrops();
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 14;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE) ? 20F : 1F;
    }
}
