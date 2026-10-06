package net.scwunge.rotarycraft.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/** Dryer recipe (rotarycraft:drying): {@code fluid.amount} mB of the fluid dry out into {@code result}, every 400 ticks. */
public record DryingRecipe(FluidStack fluid, ItemStack result) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<DryingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidStack.CODEC.fieldOf("fluid").forGetter(DryingRecipe::fluid),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(DryingRecipe::result)
    ).apply(i, DryingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DryingRecipe> STREAM_CODEC = StreamCodec.composite(
            FluidStack.STREAM_CODEC, DryingRecipe::fluid,
            ItemStack.STREAM_CODEC, DryingRecipe::result,
            DryingRecipe::new);

    public boolean accepts(FluidStack in) {
        return !in.isEmpty() && FluidStack.isSameFluidSameComponents(in.copyWithAmount(1), fluid.copyWithAmount(1));
    }

    /** Fluid recipes aren't matched by item. */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
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
        return RotaryRecipes.DRYING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.DRYING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<DryingRecipe> {
        @Override
        public MapCodec<DryingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DryingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
