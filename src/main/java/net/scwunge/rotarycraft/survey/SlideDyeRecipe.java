package net.scwunge.rotarycraft.survey;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.SurveyRegistry;

/**
 * A dye and a slide make the slide a number of places on: how many depends on the dye (the original gave each of the sixteen dyes its
 * own step, at random for each world; here the steps are fixed). 24 slides, so it comes round again.
 */
public class SlideDyeRecipe extends CustomRecipe {
    /** How far each dye (by its id) moves a slide on: all different, 0 to 15. */
    private static final int[] STEP = {5, 11, 3, 14, 8, 1, 12, 6, 9, 2, 15, 7, 0, 13, 4, 10};

    public SlideDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.ingredientCount() == 2 && find(input, true) != null && find(input, false) != null;
    }

    private static ItemStack find(CraftingInput input, boolean slide) {
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty() && (slide ? stack.getItem() instanceof SlideItem : stack.getItem() instanceof DyeItem)) {
                return stack;
            }
        }
        return null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        SlideItem slide = (SlideItem) find(input, true).getItem();
        DyeItem dye = (DyeItem) find(input, false).getItem();
        return new ItemStack(SurveyRegistry.SLIDES.get((slide.index() + STEP[dye.getDyeColor().getId()]) % SlideItem.COUNT).get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return NonNullList.withSize(input.size(), ItemStack.EMPTY);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SurveyRegistry.SLIDE_DYE_SERIALIZER.get();
    }

    public static RecipeSerializer<SlideDyeRecipe> serializer() {
        return new SimpleCraftingRecipeSerializer<>(SlideDyeRecipe::new);
    }
}
