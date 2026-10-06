package net.scwunge.rotarycraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/** Composter recipe (rotarycraft:composting): one item rots into {@code value} Compost. */
public record CompostingRecipe(Ingredient ingredient, int value) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CompostingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CompostingRecipe::ingredient),
            Codec.INT.fieldOf("value").forGetter(CompostingRecipe::value)
    ).apply(i, CompostingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompostingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CompostingRecipe::ingredient,
            ByteBufCodecs.VAR_INT, CompostingRecipe::value,
            CompostingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return new ItemStack(RotaryItems.COMPOST.get(), value);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(RotaryItems.COMPOST.get(), value);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RotaryRecipes.COMPOSTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.COMPOSTING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<CompostingRecipe> {
        @Override
        public MapCodec<CompostingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CompostingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
