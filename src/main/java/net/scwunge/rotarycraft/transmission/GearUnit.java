package net.scwunge.rotarycraft.transmission;

import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryParts;
import org.jetbrains.annotations.Nullable;

/** A gear unit item (2:1 to 16:1, in a material): what a Power Bus side is fitted with to set its ratio and how much it can take. */
public record GearUnit(ShaftMaterial material, int ratio) {
    /** The gear unit this stack is, or null. */
    @Nullable
    public static GearUnit of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (ShaftMaterial material : RotaryParts.GEAR_MATERIALS) {
            for (int ratio : RotaryParts.RATIOS) {
                if (stack.is(RotaryParts.GEAR_UNITS.get(material).get(ratio).get())) {
                    return new GearUnit(material, ratio);
                }
            }
        }
        return null;
    }

    /** The ratio of the gear unit in a stack, or 0 if it is not one. */
    public static int ratioOf(ItemStack stack) {
        GearUnit unit = of(stack);
        return unit == null ? 0 : unit.ratio();
    }
}
