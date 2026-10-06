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
 * Pulse Furnace recipe (rotarycraft:pulse_smelting): one item becomes {@code result} once the furnace is at least
 * {@code temperature} C. {@code temperature} defaults to 600, like most of the original's recycling recipes.
 */
public record PulseSmeltingRecipe(Ingredient ingredient, ItemStack result, int temperature) implements Recipe<SingleRecipeInput> {
    public static final int DEFAULT_TEMPERATURE = 600;

    public static final MapCodec<PulseSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(PulseSmeltingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(PulseSmeltingRecipe::result),
            Codec.INT.optionalFieldOf("temperature", DEFAULT_TEMPERATURE).forGetter(PulseSmeltingRecipe::temperature)
    ).apply(i, PulseSmeltingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PulseSmeltingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, PulseSmeltingRecipe::ingredient,
            ItemStack.STREAM_CODEC, PulseSmeltingRecipe::result,
            ByteBufCodecs.VAR_INT, PulseSmeltingRecipe::temperature,
            PulseSmeltingRecipe::new);

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
        return RotaryRecipes.PULSE_SMELTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.PULSE_SMELTING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<PulseSmeltingRecipe> {
        @Override
        public MapCodec<PulseSmeltingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PulseSmeltingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
