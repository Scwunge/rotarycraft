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

import java.util.List;

/**
 * Centrifuge recipe (rotarycraft:centrifuge): one input item, any number of outputs each with its own chance (percent, as
 * in the original; over 100 means more than one), and optionally a fluid. {@code per_operation} is how many inputs one
 * spin can take (the original lets sludge go two at a time).
 */
public record CentrifugeRecipe(Ingredient ingredient, int perOperation, List<ChancedOutput> outputs, FluidStack fluid, float fluidChance)
        implements Recipe<SingleRecipeInput> {

    public record ChancedOutput(ItemStack item, float chance) {
        public static final Codec<ChancedOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.STRICT_CODEC.fieldOf("item").forGetter(ChancedOutput::item),
                Codec.FLOAT.fieldOf("chance").forGetter(ChancedOutput::chance)
        ).apply(i, ChancedOutput::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChancedOutput> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, ChancedOutput::item,
                ByteBufCodecs.FLOAT, ChancedOutput::chance,
                ChancedOutput::new);
    }

    public static final MapCodec<CentrifugeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CentrifugeRecipe::ingredient),
            Codec.INT.optionalFieldOf("per_operation", 1).forGetter(CentrifugeRecipe::perOperation),
            ChancedOutput.CODEC.listOf().fieldOf("outputs").forGetter(CentrifugeRecipe::outputs),
            FluidStack.OPTIONAL_CODEC.optionalFieldOf("fluid", FluidStack.EMPTY).forGetter(CentrifugeRecipe::fluid),
            Codec.FLOAT.optionalFieldOf("fluid_chance", 100F).forGetter(CentrifugeRecipe::fluidChance)
    ).apply(i, CentrifugeRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, CentrifugeRecipe::ingredient,
            ByteBufCodecs.VAR_INT, CentrifugeRecipe::perOperation,
            ChancedOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), CentrifugeRecipe::outputs,
            FluidStack.OPTIONAL_STREAM_CODEC, CentrifugeRecipe::fluid,
            ByteBufCodecs.FLOAT, CentrifugeRecipe::fluidChance,
            CentrifugeRecipe::new);

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).item().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).item();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RotaryRecipes.CENTRIFUGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.CENTRIFUGE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<CentrifugeRecipe> {
        @Override
        public MapCodec<CentrifugeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CentrifugeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
