package net.scwunge.rotarycraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.recipe.ExtractionRecipe;
import net.scwunge.rotarycraft.recipe.GrindingRecipe;
import net.scwunge.rotarycraft.recipe.TagResultSmeltingRecipe;

public class RotaryRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, RotaryCraft.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, RotaryCraft.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GrindingRecipe>> GRINDING =
            TYPES.register("grinding", () -> RecipeType.simple(RotaryCraft.id("grinding")));
    public static final DeferredHolder<RecipeSerializer<?>, GrindingRecipe.Serializer> GRINDING_SERIALIZER =
            SERIALIZERS.register("grinding", GrindingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ExtractionRecipe>> EXTRACTION =
            TYPES.register("extraction", () -> RecipeType.simple(RotaryCraft.id("extraction")));
    public static final DeferredHolder<RecipeSerializer<?>, ExtractionRecipe.Serializer> EXTRACTION_SERIALIZER =
            SERIALIZERS.register("extraction", ExtractionRecipe.Serializer::new);

    /** Furnace recipes whose result comes from an item tag (Extractor flakes into the pack's ingot). */
    public static final DeferredHolder<RecipeSerializer<?>, TagResultSmeltingRecipe.Serializer> TAG_SMELTING_SERIALIZER =
            SERIALIZERS.register("tag_smelting", TagResultSmeltingRecipe.Serializer::new);
}
