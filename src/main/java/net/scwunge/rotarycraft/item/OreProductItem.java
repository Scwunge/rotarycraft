package net.scwunge.rotarycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.registry.RotaryComponents;

/**
 * One Extractor stage product (dust, slurry, solution or flakes) for any ore; the ore is stored on the stack. Named
 * "Iron Dust", "Tin Flakes" and so on.
 */
public class OreProductItem extends Item {
    public enum Stage {
        DUST, SLURRY, SOLUTION, FLAKES;

        public String id() {
            return "ore_" + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private final Stage stage;

    public OreProductItem(Properties props, Stage stage) {
        super(props);
        this.stage = stage;
    }

    public Stage stage() {
        return stage;
    }

    public static ItemStack of(Item item, OreProduct product, int count) {
        ItemStack stack = new ItemStack(item, count);
        stack.set(RotaryComponents.ORE_PRODUCT.get(), product);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        OreProduct product = stack.get(RotaryComponents.ORE_PRODUCT.get());
        if (product == null) {
            return super.getName(stack);
        }
        String type = product.type();
        String fallback = Character.toUpperCase(type.charAt(0)) + type.substring(1).replace('_', ' ');
        return Component.translatable(getDescriptionId() + ".named",
                Component.translatableWithFallback("ore_type.rotarycraft." + type, fallback));
    }
}
