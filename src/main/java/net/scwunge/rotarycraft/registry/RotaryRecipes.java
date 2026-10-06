package net.scwunge.rotarycraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.recipe.BlastFurnaceRecipe;
import net.scwunge.rotarycraft.recipe.CentrifugeRecipe;
import net.scwunge.rotarycraft.recipe.ExtractionRecipe;
import net.scwunge.rotarycraft.recipe.GrindingRecipe;
import net.scwunge.rotarycraft.recipe.MeltingRecipe;
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

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE =
            TYPES.register("blast_furnace", () -> RecipeType.simple(RotaryCraft.id("blast_furnace")));
    public static final DeferredHolder<RecipeSerializer<?>, BlastFurnaceRecipe.Serializer> BLAST_FURNACE_SERIALIZER =
            SERIALIZERS.register("blast_furnace", BlastFurnaceRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<net.scwunge.rotarycraft.recipe.BlastCraftingRecipe>> BLAST_CRAFTING =
            TYPES.register("blast_crafting", () -> RecipeType.simple(RotaryCraft.id("blast_crafting")));
    public static final DeferredHolder<RecipeSerializer<?>, net.scwunge.rotarycraft.recipe.BlastCraftingRecipe.Serializer> BLAST_CRAFTING_SERIALIZER =
            SERIALIZERS.register("blast_crafting", net.scwunge.rotarycraft.recipe.BlastCraftingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<net.scwunge.rotarycraft.recipe.FrictionHeatingRecipe>> FRICTION_HEATING =
            TYPES.register("friction_heating", () -> RecipeType.simple(RotaryCraft.id("friction_heating")));
    public static final DeferredHolder<RecipeSerializer<?>, net.scwunge.rotarycraft.recipe.FrictionHeatingRecipe.Serializer> FRICTION_HEATING_SERIALIZER =
            SERIALIZERS.register("friction_heating", net.scwunge.rotarycraft.recipe.FrictionHeatingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<net.scwunge.rotarycraft.recipe.CompactingRecipe>> COMPACTING =
            TYPES.register("compacting", () -> RecipeType.simple(RotaryCraft.id("compacting")));
    public static final DeferredHolder<RecipeSerializer<?>, net.scwunge.rotarycraft.recipe.CompactingRecipe.Serializer> COMPACTING_SERIALIZER =
            SERIALIZERS.register("compacting", net.scwunge.rotarycraft.recipe.CompactingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CentrifugeRecipe>> CENTRIFUGE =
            TYPES.register("centrifuge", () -> RecipeType.simple(RotaryCraft.id("centrifuge")));
    public static final DeferredHolder<RecipeSerializer<?>, CentrifugeRecipe.Serializer> CENTRIFUGE_SERIALIZER =
            SERIALIZERS.register("centrifuge", CentrifugeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<net.scwunge.rotarycraft.recipe.CrystallizingRecipe>> CRYSTALLIZING =
            TYPES.register("crystallizing", () -> RecipeType.simple(RotaryCraft.id("crystallizing")));
    public static final DeferredHolder<RecipeSerializer<?>, net.scwunge.rotarycraft.recipe.CrystallizingRecipe.Serializer> CRYSTALLIZING_SERIALIZER =
            SERIALIZERS.register("crystallizing", net.scwunge.rotarycraft.recipe.CrystallizingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<net.scwunge.rotarycraft.recipe.DryingRecipe>> DRYING =
            TYPES.register("drying", () -> RecipeType.simple(RotaryCraft.id("drying")));
    public static final DeferredHolder<RecipeSerializer<?>, net.scwunge.rotarycraft.recipe.DryingRecipe.Serializer> DRYING_SERIALIZER =
            SERIALIZERS.register("drying", net.scwunge.rotarycraft.recipe.DryingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<net.scwunge.rotarycraft.recipe.PulseSmeltingRecipe>> PULSE_SMELTING =
            TYPES.register("pulse_smelting", () -> RecipeType.simple(RotaryCraft.id("pulse_smelting")));
    public static final DeferredHolder<RecipeSerializer<?>, net.scwunge.rotarycraft.recipe.PulseSmeltingRecipe.Serializer> PULSE_SMELTING_SERIALIZER =
            SERIALIZERS.register("pulse_smelting", net.scwunge.rotarycraft.recipe.PulseSmeltingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MeltingRecipe>> MELTING =
            TYPES.register("melting", () -> RecipeType.simple(RotaryCraft.id("melting")));
    public static final DeferredHolder<RecipeSerializer<?>, MeltingRecipe.Serializer> MELTING_SERIALIZER =
            SERIALIZERS.register("melting", MeltingRecipe.Serializer::new);

    /** Furnace recipes whose result comes from an item tag (Extractor flakes into the pack's ingot). */
    public static final DeferredHolder<RecipeSerializer<?>, TagResultSmeltingRecipe.Serializer> TAG_SMELTING_SERIALIZER =
            SERIALIZERS.register("tag_smelting", TagResultSmeltingRecipe.Serializer::new);
}
