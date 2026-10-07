package net.scwunge.rotarycraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.registry.RotaryFluids;

import java.util.List;

/**
 * The original's integrated gearbox upgrade. The plain frame takes gears to make one of ratio 2, 4, 8 or 16; filled with lubricant it works in torque mode and with
 * liquid nitrogen in speed mode (500 mB makes it ready). Fitted to an engine that is not running, it stays there until the engine is broken.
 */
public class GearUpgradeItem extends Item {
    public static final int CAPACITY = 500;

    private final int exponent;

    /** @param exponent 0 for the plain frame, 1 to 4 for ratios 2 to 16 */
    public GearUpgradeItem(Properties properties, int exponent) {
        super(properties.stacksTo(exponent == 0 ? 16 : 1));
        this.exponent = exponent;
    }

    public int exponent() {
        return exponent;
    }

    public int ratio() {
        return exponent == 0 ? 0 : 1 << exponent;
    }

    public static FluidStack fluid(ItemStack stack) {
        SimpleFluidContent content = stack.get(RotaryComponents.ITEM_FLUID.get());
        return content == null ? FluidStack.EMPTY : content.copy();
    }

    public static boolean accepts(FluidStack fluid) {
        return fluid.getFluid() == RotaryFluids.LUBRICANT.get() || fluid.getFluid() == RotaryFluids.LIQUID_NITROGEN.get();
    }

    /** The signed ratio it gives, if it is full enough to work: positive in torque mode, negative in speed mode, 0 if it cannot be used yet. */
    public static int ratioOf(ItemStack stack) {
        if (!(stack.getItem() instanceof GearUpgradeItem gear) || gear.exponent == 0) {
            return 0;
        }
        FluidStack fluid = fluid(stack);
        if (fluid.getAmount() < CAPACITY) {
            return 0;
        }
        return fluid.getFluid() == RotaryFluids.LIQUID_NITROGEN.get() ? -gear.ratio() : gear.ratio();
    }

    /** The item for a ratio and the fluid it holds (full). */
    public static ItemStack stackFor(int ratio, boolean lubricated, net.minecraft.world.item.Item[] byExponent) {
        int exponent = Integer.numberOfTrailingZeros(Math.abs(ratio));
        ItemStack stack = new ItemStack(byExponent[exponent]);
        stack.set(RotaryComponents.ITEM_FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(
                lubricated ? RotaryFluids.LUBRICANT.get() : RotaryFluids.LIQUID_NITROGEN.get(), CAPACITY)));
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (exponent == 0) {
            tooltip.add(Component.translatable("upgrade.rotarycraft.gear.frame").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("upgrade.rotarycraft.gear.ratio", ratio()).withStyle(ChatFormatting.GRAY));
        FluidStack fluid = fluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.translatable("upgrade.rotarycraft.gear.needs_fluid").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("upgrade.rotarycraft.gear.holds", fluid.getAmount() * 100 / CAPACITY, fluid.getHoverName()).withStyle(ChatFormatting.GRAY));
            int signed = ratioOf(stack);
            if (signed != 0) {
                tooltip.add(Component.translatable(signed > 0 ? "upgrade.rotarycraft.gear.torque_mode" : "upgrade.rotarycraft.gear.speed_mode").withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
