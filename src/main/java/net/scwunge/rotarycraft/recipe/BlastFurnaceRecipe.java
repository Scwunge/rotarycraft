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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.List;
import java.util.Optional;

/**
 * Blast Furnace recipe (rotarycraft:blast_furnace), as in the original: up to three additives (centre, lower, upper slots),
 * each consumed with a chance per item made; a main ingredient in the 3x3 grid (one item from each filled slot per
 * operation); a minimum temperature; and an optional bonus yield.
 */
public record BlastFurnaceRecipe(Optional<Additive> center, Optional<Additive> lower, Optional<Additive> upper,
                                 Ingredient main, int mainPerResult, ItemStack result, int temperature, float xp, float bonusYield)
        implements Recipe<RecipeInput> {

    /** An additive: what goes in the slot, the chance per item made that one is used up (0-1), and how many tries. */
    public record Additive(Ingredient ingredient, float chance, int count) {
        public static final Codec<Additive> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(Additive::ingredient),
                Codec.FLOAT.fieldOf("chance").forGetter(Additive::chance),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Additive::count)
        ).apply(i, Additive::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Additive> STREAM_CODEC = StreamCodec.of(
                (buf, a) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, a.ingredient());
                    buf.writeFloat(a.chance());
                    buf.writeVarInt(a.count());
                },
                buf -> new Additive(Ingredient.CONTENTS_STREAM_CODEC.decode(buf), buf.readFloat(), buf.readVarInt()));
    }

    public static final MapCodec<BlastFurnaceRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Additive.CODEC.optionalFieldOf("center").forGetter(BlastFurnaceRecipe::center),
            Additive.CODEC.optionalFieldOf("lower").forGetter(BlastFurnaceRecipe::lower),
            Additive.CODEC.optionalFieldOf("upper").forGetter(BlastFurnaceRecipe::upper),
            Ingredient.CODEC_NONEMPTY.fieldOf("main").forGetter(BlastFurnaceRecipe::main),
            Codec.INT.optionalFieldOf("main_per_result", 1).forGetter(BlastFurnaceRecipe::mainPerResult),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(BlastFurnaceRecipe::result),
            Codec.INT.fieldOf("temperature").forGetter(BlastFurnaceRecipe::temperature),
            Codec.FLOAT.optionalFieldOf("xp", 0F).forGetter(BlastFurnaceRecipe::xp),
            Codec.FLOAT.optionalFieldOf("bonus_yield", 0F).forGetter(BlastFurnaceRecipe::bonusYield)
    ).apply(i, BlastFurnaceRecipe::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Additive>> OPT_ADDITIVE =
            net.minecraft.network.codec.ByteBufCodecs.optional(Additive.STREAM_CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                OPT_ADDITIVE.encode(buf, r.center());
                OPT_ADDITIVE.encode(buf, r.lower());
                OPT_ADDITIVE.encode(buf, r.upper());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.main());
                buf.writeVarInt(r.mainPerResult());
                ItemStack.STREAM_CODEC.encode(buf, r.result());
                buf.writeVarInt(r.temperature());
                buf.writeFloat(r.xp());
                buf.writeFloat(r.bonusYield());
            },
            buf -> new BlastFurnaceRecipe(OPT_ADDITIVE.decode(buf), OPT_ADDITIVE.decode(buf), OPT_ADDITIVE.decode(buf),
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf), buf.readVarInt(), ItemStack.STREAM_CODEC.decode(buf),
                    buf.readVarInt(), buf.readFloat(), buf.readFloat()));

    public List<Optional<Additive>> additives() {
        return List.of(center, lower, upper);
    }

    /** Items made from this many matching main items. */
    public int produced(int mainItems) {
        return mainPerResult > 1 ? mainItems / mainPerResult : mainItems;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false; // matched by the Blast Furnace itself, which also checks temperature and additives
    }

    @Override
    public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
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
        return RotaryRecipes.BLAST_FURNACE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RotaryRecipes.BLAST_FURNACE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Serializer implements RecipeSerializer<BlastFurnaceRecipe> {
        @Override
        public MapCodec<BlastFurnaceRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BlastFurnaceRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
