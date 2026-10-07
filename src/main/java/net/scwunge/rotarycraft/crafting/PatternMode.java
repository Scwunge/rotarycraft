package net.scwunge.rotarycraft.crafting;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/** Which recipes a Craft Pattern is written for, as the original's recipe modes. */
public enum PatternMode implements StringRepresentable {
    /** The recipes of a crafting table. */
    CRAFTING("crafting"),
    /** The recipes only the Worktable knows (data pack recipes of type rotarycraft:worktable). */
    WORKTABLE("worktable"),
    /** The Blast Furnace's crafting recipes. */
    BLAST_FURNACE("blast_furnace");

    public static final com.mojang.serialization.Codec<PatternMode> CODEC = StringRepresentable.fromEnum(PatternMode::values);
    public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, PatternMode> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.idMapper(i -> values()[Math.floorMod(i, values().length)], PatternMode::ordinal);

    private final String id;

    PatternMode(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public PatternMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public Component label() {
        return Component.translatable("gui.rotarycraft.pattern_mode." + id);
    }

    /** The item that stands for the mode on the pattern's screen. */
    public Item icon() {
        return switch (this) {
            case CRAFTING -> Items.CRAFTING_TABLE;
            case WORKTABLE -> CraftingRegistry.WORKTABLE.get().asItem();
            case BLAST_FURNACE -> RotaryBlocks.BLAST_FURNACE.get().asItem();
        };
    }

    /** The recipe of this kind that {@code input} makes, if there is one. */
    public Optional<RecipeHolder<? extends Recipe<CraftingInput>>> find(Level level, CraftingInput input) {
        return switch (this) {
            case CRAFTING -> upcast(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level));
            case WORKTABLE -> upcast(level.getRecipeManager().getRecipeFor(RotaryRecipes.WORKTABLE.get(), input, level));
            case BLAST_FURNACE -> upcast(level.getRecipeManager().getRecipeFor(RotaryRecipes.BLAST_CRAFTING.get(), input, level));
        };
    }

    @SuppressWarnings("unchecked")
    private static Optional<RecipeHolder<? extends Recipe<CraftingInput>>> upcast(Optional<? extends RecipeHolder<? extends Recipe<CraftingInput>>> found) {
        return (Optional<RecipeHolder<? extends Recipe<CraftingInput>>>) found;
    }
}
