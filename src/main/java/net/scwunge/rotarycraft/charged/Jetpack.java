package net.scwunge.rotarycraft.charged;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.registry.RotaryFluids;

import java.util.List;

/**
 * What the three jetpack items (the plain pack, and the steel and bedrock chestplates with one built in) have in common: a 30 000 mB tank of ethanol or jet
 * fuel kept in the item, and up to three upgrades. The flying itself is in {@link JetpackEvents}.
 */
public interface Jetpack {
    int CAPACITY = 30000;
    /** The wings are folded away (set by sneak-using a winged pack). */
    int WINGS_OFF = 8;

    enum Kind {
        PLAIN, STEEL, BEDROCK;

        /** Bedrock packs burn twice the fuel. */
        public int fuelFactor() {
            return this == BEDROCK ? 2 : 1;
        }
    }

    enum Upgrade {
        WING(1, "Winged"), JET(2, "Thrust Boost"), COOLING(4, "Fin-Cooled");

        public final int bit;
        public final String label;

        Upgrade(int bit, String label) {
            this.bit = bit;
            this.label = label;
        }

        public boolean on(ItemStack stack) {
            return (bits(stack) & bit) != 0;
        }

        public void set(ItemStack stack) {
            int bits = bits(stack) | bit;
            stack.set(RotaryComponents.PACK_UPGRADES.get(), this == WING ? bits & ~WINGS_OFF : bits);
        }
    }

    Kind kind();

    static int bits(ItemStack stack) {
        return stack.getOrDefault(RotaryComponents.PACK_UPGRADES.get(), 0);
    }

    static boolean winged(ItemStack stack) {
        return Upgrade.WING.on(stack) && (bits(stack) & WINGS_OFF) == 0;
    }

    static FluidStack fluid(ItemStack stack) {
        SimpleFluidContent content = stack.get(RotaryComponents.ITEM_FLUID.get());
        return content == null ? FluidStack.EMPTY : content.copy();
    }

    static int fuel(ItemStack stack) {
        return fluid(stack).getAmount();
    }

    static boolean jetFueled(ItemStack stack) {
        FluidStack fluid = fluid(stack);
        return !fluid.isEmpty() && fluid.getFluid() == RotaryFluids.JET_FUEL.get();
    }

    static void use(ItemStack stack, int amount) {
        FluidStack fluid = fluid(stack);
        if (fluid.isEmpty()) {
            return;
        }
        fluid.shrink(amount);
        if (fluid.isEmpty()) {
            stack.remove(RotaryComponents.ITEM_FLUID.get());
        } else {
            stack.set(RotaryComponents.ITEM_FLUID.get(), SimpleFluidContent.copyOf(fluid));
        }
    }

    /** A pack holding this much of a fuel. */
    static ItemStack filled(ItemStack stack, boolean jetFuel, int amount) {
        stack.set(RotaryComponents.ITEM_FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(
                jetFuel ? RotaryFluids.JET_FUEL.get() : RotaryFluids.ETHANOL.get(), Math.min(amount, CAPACITY))));
        return stack;
    }

    /** Ethanol is a fuel unless the server wants jet fuel only; jet fuel always is. */
    static boolean accepts(FluidStack fluid) {
        if (fluid.getFluid() == RotaryFluids.JET_FUEL.get()) {
            return true;
        }
        return fluid.getFluid() == RotaryFluids.ETHANOL.get() && !RotaryConfig.get(RotaryConfig.JETPACK_NEEDS_JET_FUEL);
    }

    /** What the creative tab shows: the pack empty, full of ethanol, full of jet fuel, and full of jet fuel with every upgrade. */
    static List<ItemStack> creative(net.minecraft.world.item.Item item, net.minecraft.core.HolderLookup.Provider holders) {
        java.util.function.Supplier<ItemStack> base = () -> item instanceof net.scwunge.rotarycraft.tool.Forced forced
                ? net.scwunge.rotarycraft.tool.Forced.stackOf(item, holders, forced.needs()) : new ItemStack(item);
        ItemStack all = filled(base.get(), true, CAPACITY);
        for (Upgrade upgrade : Upgrade.values()) {
            upgrade.set(all);
        }
        return List.of(base.get(), filled(base.get(), false, CAPACITY), filled(base.get(), true, CAPACITY), all);
    }

    static void describe(ItemStack stack, List<Component> tooltip) {
        for (Upgrade upgrade : Upgrade.values()) {
            if (upgrade.on(stack)) {
                tooltip.add(Component.literal(upgrade.label).withStyle(ChatFormatting.AQUA));
                if (upgrade == Upgrade.WING && !winged(stack)) {
                    tooltip.add(Component.translatable("item.rotarycraft.jetpack.wings_off").withStyle(ChatFormatting.RED));
                }
            }
        }
        FluidStack fluid = fluid(stack);
        tooltip.add(fluid.isEmpty() ? Component.translatable("item.rotarycraft.jetpack.empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable("item.rotarycraft.jetpack.fuel", fluid.getAmount(), fluid.getHoverName()).withStyle(ChatFormatting.GRAY));
    }
}
