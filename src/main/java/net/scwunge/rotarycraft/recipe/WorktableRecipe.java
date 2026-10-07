package net.scwunge.rotarycraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/**
 * A shaped 3x3 recipe only the Worktable makes (a crafting table does not know it), as the original's worktable-only recipes. A Craft Pattern
 * in Worktable mode is written from these.
 */
public record WorktableRecipe(ShapedRecipePattern pattern, ItemStack result) implements Recipe<CraftingInput> {
    public static final MapCodec<WorktableRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ShapedRecipePattern.MAP_CODEC.forGetter(WorktableRecipe::pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WorktableRecipe::result)
    ).apply(i, WorktableRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WorktableRecipe> STREAM_CODEC = StreamCodec.composite(
            ShapedRecipePattern.STREAM_CODEC, WorktableRecipe::pattern,
            ItemStack.STREAM_CODEC, WorktableRecipe::result,
            WorktableRecipe::new);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return pattern.matches(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.width() && height >= pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RotaryRecipes.WORKTABLE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.WORKTABLE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<WorktableRecipe> {
        @Override
        public MapCodec<WorktableRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WorktableRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
