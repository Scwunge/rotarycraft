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

/**
 * Crystallizer recipe (rotarycraft:crystallizing): {@code fluid.amount} mB of the fluid freeze into {@code result}. The fluid
 * must be at or below its own freezing point (0.9 x its temperature) in the machine, as in the original.
 */
public record CrystallizingRecipe(FluidStack fluid, ItemStack result) implements Recipe<SingleRecipeInput> {
    public static final MapCodec<CrystallizingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            FluidStack.CODEC.fieldOf("fluid").forGetter(CrystallizingRecipe::fluid),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrystallizingRecipe::result)
    ).apply(i, CrystallizingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrystallizingRecipe> STREAM_CODEC = StreamCodec.composite(
            FluidStack.STREAM_CODEC, CrystallizingRecipe::fluid,
            ItemStack.STREAM_CODEC, CrystallizingRecipe::result,
            CrystallizingRecipe::new);

    /** The temperature (C) the fluid freezes at, from the original: -273 + 0.9 x its temperature in kelvin. */
    public int freezingPoint() {
        return -273 + (int) (0.9 * fluid.getFluidType().getTemperature(fluid));
    }

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
        return RotaryRecipes.CRYSTALLIZING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.CRYSTALLIZING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<CrystallizingRecipe> {
        @Override
        public MapCodec<CrystallizingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrystallizingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
