package net.scwunge.rotarycraft.decor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.List;

/** The Decorative Tank as an item: it takes up to 25 mB of one fluid (from a filling station, or a pipe into the item), and its settings, and says so. */
public class DecoTankItem extends BlockItem {
    public DecoTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** The fluid it holds, empty if it is not full. */
    public static FluidStack fluidOf(ItemStack stack) {
        SimpleFluidContent content = stack.get(DecoTank.FLUID.get());
        return content == null ? FluidStack.EMPTY : content.copy();
    }

    public static boolean isFull(ItemStack stack) {
        return fluidOf(stack).getAmount() >= DecoTank.FILL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = fluidOf(stack);
        if (!fluid.isEmpty() && isFull(stack)) {
            tooltip.add(Component.translatable("tooltip.rotarycraft.deco_tank.full", fluid.getHoverName()).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.rotarycraft.deco_tank.empty").withStyle(ChatFormatting.GRAY));
        }
        for (DecoTank.Flag f : DecoTank.Flag.LIST) {
            tooltip.add(Component.literal(f.display + ": " + DecoTank.has(stack, f)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
