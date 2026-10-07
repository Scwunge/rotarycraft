package net.scwunge.rotarycraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.scwunge.rotarycraft.registry.RotaryComponents;

import java.util.List;

/**
 * A shaft core. Magnetized in the Magnetizer, it runs the AC Engine, which uses up 1 uT every 30 seconds of running
 * (a tungsten core only half the time). {@code chargeChance} is how often a Magnetizer cycle adds 1 uT (1 in N).
 */
public class ShaftCoreItem extends Item implements Magnetizable {
    /** Magnetizer speed needed per uT of charge: a core holds at most speed / 2 uT. */
    public static final int SPEED_PER_MICROTESLA = 2;

    private final int chargeChance;
    private final boolean durable;

    public ShaftCoreItem(Properties props, int chargeChance, boolean durable) {
        super(props);
        this.chargeChance = chargeChance;
        this.durable = durable;
    }

    @Override
    public int chargeChance() {
        return chargeChance;
    }

    /** Tungsten cores lose charge only half as often. */
    public boolean durable() {
        return durable;
    }

    public static int magnetization(ItemStack stack) {
        return stack.getOrDefault(RotaryComponents.MAGNETIZATION.get(), 0);
    }

    public static void setMagnetization(ItemStack stack, int uT) {
        if (uT > 0) {
            stack.set(RotaryComponents.MAGNETIZATION.get(), uT);
        } else {
            stack.remove(RotaryComponents.MAGNETIZATION.get());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int m = magnetization(stack);
        tooltip.add(m > 0 ? Component.translatable("item.rotarycraft.shaft_core.magnetized", m).withStyle(ChatFormatting.AQUA)
                : Component.translatable("item.rotarycraft.shaft_core.unmagnetized").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return magnetization(stack) > 0;
    }
}
