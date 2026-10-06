package net.scwunge.rotarycraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.scwunge.rotarycraft.registry.WeaponRegistry;

import java.util.List;

/**
 * A wound coil (the original's Spring and Strong Coil): the Winder winds it up from shaft power and unwinds it into power again,
 * and spring-driven machines such as the Landmine run on its charge. The charge is a number up to 65536; a stiffer coil takes more
 * torque to wind and lasts longer in a machine, and gives more torque and speed in the Winder.
 */
public class CoilItem extends Item {
    public static final int MAX_CHARGE = 65536;

    private final int stiffness;
    private final int powerScale;
    private final boolean breakable;

    public CoilItem(Properties props, int stiffness, int powerScale, boolean breakable) {
        super(props.stacksTo(1));
        this.stiffness = stiffness;
        this.powerScale = powerScale;
        this.breakable = breakable;
    }

    /** How much torque winding takes per unit of charge, and how many times longer the coil lasts in a machine. */
    public int stiffness() {
        return stiffness;
    }

    /** A factor on the torque and speed the coil gives when unwound in the Winder. */
    public int powerScale() {
        return powerScale;
    }

    /** Whether the coil may snap as it is wound. */
    public boolean breakable() {
        return breakable;
    }

    public static int charge(ItemStack stack) {
        return stack.getOrDefault(WeaponRegistry.COIL_CHARGE.get(), 0);
    }

    public static void setCharge(ItemStack stack, int charge) {
        if (charge > 0) {
            stack.set(WeaponRegistry.COIL_CHARGE.get(), Math.min(MAX_CHARGE, charge));
        } else {
            stack.remove(WeaponRegistry.COIL_CHARGE.get());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rotarycraft.coil.charge", charge(stack), MAX_CHARGE).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return charge(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * charge(stack) / MAX_CHARGE);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.55f, 0.7f, 1f);
    }
}
