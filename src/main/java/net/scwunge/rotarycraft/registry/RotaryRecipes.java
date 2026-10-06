package net.scwunge.rotarycraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.recipe.GrindingRecipe;

public class RotaryRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, RotaryCraft.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, RotaryCraft.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GrindingRecipe>> GRINDING =
            TYPES.register("grinding", () -> RecipeType.simple(RotaryCraft.id("grinding")));
    public static final DeferredHolder<RecipeSerializer<?>, GrindingRecipe.Serializer> GRINDING_SERIALIZER =
            SERIALIZERS.register("grinding", GrindingRecipe.Serializer::new);
}
