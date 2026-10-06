package net.scwunge.rotarycraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.item.OreProduct;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

/**
 * Defines one ore for the Extractor (type rotarycraft:extraction): which blocks count as the raw ore, the product tint,
 * how often each stage doubles its output, and an optional bonus item from the last stage.
 * Rarity: common doubles 50% of the time, nether 80%, rare 90% (the original's oreCopy values).
 */
public record ExtractionRecipe(Ingredient ore, String type, int color, String rarity, ItemStack bonus, float bonusChance)
        implements Recipe<SingleRecipeInput> {
    public static final MapCodec<ExtractionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.fieldOf("ore").forGetter(ExtractionRecipe::ore),
            Codec.STRING.fieldOf("ore_type").forGetter(ExtractionRecipe::type),
            Codec.INT.fieldOf("color").forGetter(ExtractionRecipe::color),
            Codec.STRING.optionalFieldOf("rarity", "common").forGetter(ExtractionRecipe::rarity),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("bonus", ItemStack.EMPTY).forGetter(ExtractionRecipe::bonus),
            Codec.FLOAT.optionalFieldOf("bonus_chance", 0F).forGetter(ExtractionRecipe::bonusChance)
    ).apply(i, ExtractionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractionRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ore());
                buf.writeUtf(r.type());
                buf.writeInt(r.color());
                buf.writeUtf(r.rarity());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, r.bonus());
                buf.writeFloat(r.bonusChance());
            },
            buf -> new ExtractionRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), buf.readUtf(), buf.readInt(), buf.readUtf(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readFloat()));

    public OreProduct product() {
        return new OreProduct(type, color);
    }

    /** Chance that a stage produces two items instead of one. */
    public double doublingChance() {
        return switch (rarity) {
            case "rare" -> 0.9;
            case "nether" -> 0.8;
            default -> 0.5;
        };
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ore.test(input.item());
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
        return RotaryRecipes.EXTRACTION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.EXTRACTION.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<ExtractionRecipe> {
        @Override
        public MapCodec<ExtractionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ExtractionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
