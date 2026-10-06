package net.scwunge.rotarycraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
 * A shaped 3x3 recipe made in the Blast Furnace's grid once it is at least {@code temperature} C, as the original's
 * "blast crafting" (high-temperature combustor, diamond gears, bedrock parts). {@code speed} > 1 makes the furnace advance
 * only every {@code speed}-th tick, so the craft takes that many times longer.
 */
public record BlastCraftingRecipe(ShapedRecipePattern pattern, ItemStack result, int temperature, int speed, float xp)
        implements Recipe<CraftingInput> {
    public static final MapCodec<BlastCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ShapedRecipePattern.MAP_CODEC.forGetter(BlastCraftingRecipe::pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(BlastCraftingRecipe::result),
            Codec.INT.fieldOf("temperature").forGetter(BlastCraftingRecipe::temperature),
            Codec.INT.optionalFieldOf("speed", 1).forGetter(BlastCraftingRecipe::speed),
            Codec.FLOAT.optionalFieldOf("xp", 0F).forGetter(BlastCraftingRecipe::xp)
    ).apply(i, BlastCraftingRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlastCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
            ShapedRecipePattern.STREAM_CODEC, BlastCraftingRecipe::pattern,
            ItemStack.STREAM_CODEC, BlastCraftingRecipe::result,
            ByteBufCodecs.VAR_INT, BlastCraftingRecipe::temperature,
            ByteBufCodecs.VAR_INT, BlastCraftingRecipe::speed,
            ByteBufCodecs.FLOAT, BlastCraftingRecipe::xp,
            BlastCraftingRecipe::new);

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
        return RotaryRecipes.BLAST_CRAFTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.BLAST_CRAFTING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<BlastCraftingRecipe> {
        @Override
        public MapCodec<BlastCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlastCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
