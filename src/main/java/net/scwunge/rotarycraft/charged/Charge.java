package net.scwunge.rotarycraft.charged;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.registry.RotaryComponents;

/**
 * The charge of the original's charged tools and armour: a number of kJ from 0 to 32000 that a use takes a unit or two off. A new tool is empty; a spring coil
 * charges it on the Worktable, and it warns at 32, 16, 4 and 2 kJ.
 */
public final class Charge {
    public static final int FULL = 32000;

    private Charge() {}

    public static int get(ItemStack stack) {
        return stack.getOrDefault(RotaryComponents.CHARGE.get(), 0);
    }

    public static void set(ItemStack stack, int charge) {
        if (charge > 0) {
            stack.set(RotaryComponents.CHARGE.get(), Math.min(FULL, charge));
        } else {
            stack.remove(RotaryComponents.CHARGE.get());
        }
    }

    public static ItemStack full(ItemStack stack) {
        set(stack, FULL);
        return stack;
    }

    /** Takes {@code amount} off if there is that much; otherwise says the tool is flat and leaves it. Creative players never run down. */
    public static boolean use(ItemStack stack, Player player, int amount, String what) {
        if (player.isCreative()) {
            return true;
        }
        int charge = get(stack);
        if (charge < amount || charge <= 0) {
            player.displayClientMessage(Component.translatable("message.rotarycraft." + what + ".depleted"), true);
            return false;
        }
        set(stack, charge - amount);
        int left = charge - amount;
        if (left == 2 || left == 4 || left == 16 || left == 32) {
            player.displayClientMessage(Component.translatable("message.rotarycraft." + what + (left == 2 ? ".very_low" : ".low"), left), true);
        }
        return true;
    }
}
