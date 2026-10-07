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
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

/** Wetter recipe (rotarycraft:wetting): an item soaked in a fluid for {@code duration} ticks turns into the result, using the fluid's amount. */
public record WettingRecipe(Ingredient ingredient, SizedFluidIngredient fluid, ItemStack result, int duration) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<WettingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(WettingRecipe::ingredient),
            SizedFluidIngredient.FLAT_CODEC.fieldOf("fluid").forGetter(WettingRecipe::fluid),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WettingRecipe::result),
            Codec.INT.fieldOf("duration").forGetter(WettingRecipe::duration)
    ).apply(i, WettingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WettingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, WettingRecipe::ingredient,
            SizedFluidIngredient.STREAM_CODEC, WettingRecipe::fluid,
            ItemStack.STREAM_CODEC, WettingRecipe::result,
            ByteBufCodecs.VAR_INT, WettingRecipe::duration,
            WettingRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    /** Whether this fluid soaks the item, however little of it there is. */
    public boolean acceptsFluid(FluidStack stack) {
        return fluid.ingredient().test(stack);
    }

    /** Whether there is enough of the fluid. */
    public boolean hasEnough(FluidStack stack) {
        return fluid.test(stack);
    }

    public int amount() {
        return fluid.amount();
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
        return LogisticsRegistry.WETTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return LogisticsRegistry.WETTING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<WettingRecipe> {
        @Override
        public MapCodec<WettingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WettingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
