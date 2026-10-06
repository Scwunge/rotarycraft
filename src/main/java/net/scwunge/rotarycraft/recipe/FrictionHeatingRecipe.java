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
 * Friction Heater recipe (rotarycraft:friction_heating): the Friction Heater smelts {@code ingredient} in the furnace in front
 * of it once it is at least {@code temperature} C, taking {@code duration} furnace ticks (shortened as it gets hotter).
 */
public record FrictionHeatingRecipe(Ingredient ingredient, ItemStack result, int temperature, int duration) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<FrictionHeatingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(FrictionHeatingRecipe::ingredient),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(FrictionHeatingRecipe::result),
            Codec.INT.fieldOf("temperature").forGetter(FrictionHeatingRecipe::temperature),
            Codec.INT.fieldOf("duration").forGetter(FrictionHeatingRecipe::duration)
    ).apply(i, FrictionHeatingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FrictionHeatingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, FrictionHeatingRecipe::ingredient,
            ItemStack.STREAM_CODEC, FrictionHeatingRecipe::result,
            ByteBufCodecs.VAR_INT, FrictionHeatingRecipe::temperature,
            ByteBufCodecs.VAR_INT, FrictionHeatingRecipe::duration,
            FrictionHeatingRecipe::new);

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
        return RotaryRecipes.FRICTION_HEATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.FRICTION_HEATING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<FrictionHeatingRecipe> {
        @Override
        public MapCodec<FrictionHeatingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FrictionHeatingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
