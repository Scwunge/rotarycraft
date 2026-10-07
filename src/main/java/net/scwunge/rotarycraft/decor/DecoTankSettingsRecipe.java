package net.scwunge.rotarycraft.decor;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/**
 * Changing a tank's settings (DecoTankSettingsRecipe): one tank and any of the items that switch them (a glass pane for clear glass, a dye for ignoring the
 * fluid's colour, glowstone dust for glowing, obsidian for resistant) in the grid; each switches its setting the other way, and the tank keeps its fluid.
 */
public class DecoTankSettingsRecipe extends CustomRecipe {
    public DecoTankSettingsRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int tanks = 0;
        int others = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(DecorRegistry.DECO_TANK.get())) {
                tanks++;
            } else if (java.util.Arrays.stream(DecoTank.Flag.LIST).anyMatch(f -> f.isToggle(stack))) {
                others++;
            } else {
                return false;
            }
        }
        return tanks == 1 && others >= 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack tank = ItemStack.EMPTY;
        for (ItemStack stack : input.items()) {
            if (stack.is(DecorRegistry.DECO_TANK.get())) {
                tank = stack;
            }
        }
        ItemStack out = tank.copyWithCount(1);
        int flags = DecoTank.flags(tank);
        for (DecoTank.Flag flag : DecoTank.Flag.LIST) {
            for (ItemStack stack : input.items()) {
                if (flag.isToggle(stack)) {
                    flags ^= flag.bit();
                    break;
                }
            }
        }
        if (flags == 0) {
            out.remove(DecoTank.FLAGS.get());
        } else {
            out.set(DecoTank.FLAGS.get(), flags);
        }
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DecorRegistry.DECO_TANK_SETTINGS.get();
    }

    public static RecipeSerializer<DecoTankSettingsRecipe> serializer() {
        return new SimpleCraftingRecipeSerializer<>(DecoTankSettingsRecipe::new);
    }
}
