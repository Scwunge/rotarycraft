package net.scwunge.rotarycraft.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Comparator;
import java.util.Optional;

/**
 * A normal furnace recipe (a real SmeltingRecipe, which the vanilla furnace requires) whose result is "an item from this tag" (rotarycraft:tag_smelting), so Extractor flakes smelt
 * into whatever ingot the modpack has for that metal. Prefers Minecraft's own item, then this mod's, then the first other
 * mod alphabetically. If the tag is empty the recipe makes nothing.
 */
public class TagResultSmeltingRecipe extends SmeltingRecipe {
    private final ResourceLocation resultTag;
    private final int count;

    public TagResultSmeltingRecipe(Ingredient ingredient, ResourceLocation resultTag, int count, float experience, int cookingTime) {
        super("", CookingBookCategory.MISC, ingredient, ItemStack.EMPTY, experience, cookingTime);
        this.resultTag = resultTag;
        this.count = count;
    }

    public ResourceLocation resultTag() {
        return resultTag;
    }

    public int count() {
        return count;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public float experience() {
        return experience;
    }

    public int time() {
        return cookingTime;
    }

    /** The item this recipe currently produces, or empty if nothing in the pack has the tag. */
    public ItemStack resolveResult() {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, resultTag);
        Optional<Item> best = BuiltInRegistries.ITEM.getTag(tag).stream()
                .flatMap(set -> set.stream())
                .map(Holder::value)
                .min(Comparator.comparingInt((Item i) -> priority(BuiltInRegistries.ITEM.getKey(i).getNamespace()))
                        .thenComparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
        return best.map(i -> new ItemStack(i, count)).orElse(ItemStack.EMPTY);
    }

    private static int priority(String namespace) {
        return switch (namespace) {
            case "minecraft" -> 0;
            case "rotarycraft" -> 1;
            default -> 2;
        };
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item()) && !resolveResult().isEmpty();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return resolveResult();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return resolveResult();
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.FURNACE);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RotaryRecipes.TAG_SMELTING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<TagResultSmeltingRecipe> {
        public static final MapCodec<TagResultSmeltingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(TagResultSmeltingRecipe::ingredient),
                ResourceLocation.CODEC.fieldOf("result_tag").forGetter(TagResultSmeltingRecipe::resultTag),
                Codec.INT.optionalFieldOf("count", 1).forGetter(TagResultSmeltingRecipe::count),
                Codec.FLOAT.optionalFieldOf("experience", 0.7F).forGetter(TagResultSmeltingRecipe::experience),
                Codec.INT.optionalFieldOf("cookingtime", 200).forGetter(TagResultSmeltingRecipe::time)
        ).apply(i, TagResultSmeltingRecipe::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, TagResultSmeltingRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.ingredient());
                    buf.writeResourceLocation(r.resultTag());
                    buf.writeVarInt(r.count());
                    buf.writeFloat(r.experience());
                    buf.writeVarInt(r.time());
                },
                buf -> new TagResultSmeltingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), buf.readResourceLocation(),
                        buf.readVarInt(), buf.readFloat(), buf.readVarInt()));

        @Override
        public MapCodec<TagResultSmeltingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TagResultSmeltingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
