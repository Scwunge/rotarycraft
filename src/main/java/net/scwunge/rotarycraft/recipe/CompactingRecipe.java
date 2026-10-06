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
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/**
 * Compactor recipe (rotarycraft:compacting): four of the ingredient become the result once the Compactor holds at least
 * {@code pressure} kPa and {@code temperature} C. {@code stage} (1-4) scales the time, as the original's coal stages do.
 */
public record CompactingRecipe(Ingredient ingredient, ItemStack result, int pressure, int temperature, int stage) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CompactingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CompactingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CompactingRecipe::result),
            Codec.INT.fieldOf("pressure").forGetter(CompactingRecipe::pressure),
            Codec.INT.fieldOf("temperature").forGetter(CompactingRecipe::temperature),
            Codec.intRange(1, 4).optionalFieldOf("stage", 1).forGetter(CompactingRecipe::stage)
    ).apply(i, CompactingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CompactingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CompactingRecipe::ingredient,
            ItemStack.STREAM_CODEC, CompactingRecipe::result,
            ByteBufCodecs.VAR_INT, CompactingRecipe::pressure,
            ByteBufCodecs.VAR_INT, CompactingRecipe::temperature,
            ByteBufCodecs.VAR_INT, CompactingRecipe::stage,
            CompactingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RotaryRecipes.COMPACTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.COMPACTING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<CompactingRecipe> {
        @Override
        public MapCodec<CompactingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CompactingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
