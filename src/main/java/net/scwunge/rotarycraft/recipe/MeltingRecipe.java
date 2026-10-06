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
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/**
 * Rock Melter recipe (rotarycraft:melting): one item becomes {@code result} once the melter is at least
 * {@code temperature} C and has put {@code energy} joules into it, as in the original.
 */
public record MeltingRecipe(Ingredient ingredient, FluidStack result, int temperature, long energy) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<MeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MeltingRecipe::ingredient),
            FluidStack.CODEC.fieldOf("result").forGetter(MeltingRecipe::result),
            Codec.INT.fieldOf("temperature").forGetter(MeltingRecipe::temperature),
            Codec.LONG.fieldOf("energy").forGetter(MeltingRecipe::energy)
    ).apply(i, MeltingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, MeltingRecipe::ingredient,
            FluidStack.STREAM_CODEC, MeltingRecipe::result,
            ByteBufCodecs.VAR_INT, MeltingRecipe::temperature,
            ByteBufCodecs.VAR_LONG, MeltingRecipe::energy,
            MeltingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RotaryRecipes.MELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.MELTING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<MeltingRecipe> {
        @Override
        public MapCodec<MeltingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MeltingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
