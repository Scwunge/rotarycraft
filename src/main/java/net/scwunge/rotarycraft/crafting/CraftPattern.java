package net.scwunge.rotarycraft.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.RotaryComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a Craft Pattern holds, as the original's pattern tags: the nine ingredients of a recipe (one of each, in the grid they were laid
 * out in), what that recipe makes, the kind of recipe, and how many of each ingredient a machine that is given the pattern may keep
 * ({@code limit}, 64 meaning no limit).
 */
public record CraftPattern(PatternMode mode, List<ItemStack> inputs, ItemStack output, int limit) {
    public static final int GRID = 9;
    public static final int NO_LIMIT = 64;

    public static final Codec<CraftPattern> CODEC = RecordCodecBuilder.create(i -> i.group(
            PatternMode.CODEC.optionalFieldOf("mode", PatternMode.CRAFTING).forGetter(CraftPattern::mode),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(CraftPattern::inputs),
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("output", ItemStack.EMPTY).forGetter(CraftPattern::output),
            Codec.intRange(1, NO_LIMIT).optionalFieldOf("limit", NO_LIMIT).forGetter(CraftPattern::limit)
    ).apply(i, CraftPattern::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftPattern> STREAM_CODEC = StreamCodec.composite(
            PatternMode.STREAM_CODEC, CraftPattern::mode,
            ItemStack.OPTIONAL_LIST_STREAM_CODEC, CraftPattern::inputs,
            ItemStack.OPTIONAL_STREAM_CODEC, CraftPattern::output,
            ByteBufCodecs.VAR_INT, CraftPattern::limit,
            CraftPattern::new);
    public static final CraftPattern BLANK = new CraftPattern(PatternMode.CRAFTING, List.of(), ItemStack.EMPTY, NO_LIMIT);

    public CraftPattern {
        List<ItemStack> grid = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) {
            grid.add(i < inputs.size() ? inputs.get(i).copyWithCount(Math.min(1, inputs.get(i).getCount())) : ItemStack.EMPTY);
        }
        inputs = List.copyOf(grid);
        output = output.copy();
        limit = Math.max(1, Math.min(NO_LIMIT, limit));
    }

    /** Item stacks compare by identity, so two patterns written alike only stack if the comparison looks inside them. */
    @Override
    public boolean equals(Object other) {
        return other instanceof CraftPattern p && mode == p.mode && limit == p.limit && ItemStack.listMatches(inputs, p.inputs) && ItemStack.matches(output, p.output);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * mode.hashCode() + limit) + ItemStack.hashStackList(inputs)) + ItemStack.hashItemAndComponents(output);
    }

    /** The pattern a stack carries; a bare pattern is the blank one. */
    public static CraftPattern of(ItemStack stack) {
        return stack.getOrDefault(RotaryComponents.CRAFT_PATTERN.get(), BLANK);
    }

    public static boolean isPattern(ItemStack stack) {
        return stack.getItem() instanceof CraftPatternItem;
    }

    /** True if the pattern has a recipe written in it that made something. */
    public boolean hasRecipe() {
        return !output.isEmpty();
    }

    public CraftPattern withMode(PatternMode newMode) {
        return new CraftPattern(newMode, inputs, output, limit);
    }

    public CraftPattern withLimit(int newLimit) {
        return new CraftPattern(mode, inputs, output, newLimit);
    }

    public CraftingInput input() {
        return CraftingInput.of(3, 3, new ArrayList<>(inputs));
    }

    /** The pattern for these ingredients, with whatever they make. */
    public static CraftPattern write(Level level, PatternMode mode, List<ItemStack> inputs, int limit) {
        CraftPattern draft = new CraftPattern(mode, inputs, ItemStack.EMPTY, limit);
        Optional<RecipeHolder<? extends Recipe<CraftingInput>>> recipe = mode.find(level, draft.input());
        ItemStack out = recipe.map(h -> h.value().assemble(draft.input(), level.registryAccess())).orElse(ItemStack.EMPTY);
        return new CraftPattern(mode, inputs, out, limit);
    }

    /** The recipe the pattern's ingredients make right now (it can be gone if a data pack changed). */
    public Optional<RecipeHolder<? extends Recipe<CraftingInput>>> recipe(Level level) {
        return hasRecipe() ? mode.find(level, input()) : Optional.empty();
    }

    /** The ingredients as a list of what is asked for, each item with how many the recipe takes. */
    public List<ItemStack> ingredientTotals() {
        List<ItemStack> totals = new ArrayList<>();
        for (ItemStack in : inputs) {
            if (in.isEmpty()) {
                continue;
            }
            boolean merged = false;
            for (ItemStack seen : totals) {
                if (ItemStack.isSameItemSameComponents(seen, in)) {
                    seen.grow(1);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                totals.add(in.copyWithCount(1));
            }
        }
        return totals;
    }

    /** True if {@code stack} is the ingredient the pattern asks for in grid slot {@code slot}. */
    public boolean wants(int slot, ItemStack stack) {
        ItemStack in = inputs.get(slot);
        return !in.isEmpty() && matches(in, stack);
    }

    /** An ingredient written in a pattern matches by item, and by components too if the ingredient had any of its own. */
    public static boolean matches(ItemStack ingredient, ItemStack candidate) {
        if (ingredient.isEmpty() || candidate.isEmpty() || !candidate.is(ingredient.getItem())) {
            return false;
        }
        return ingredient.getComponentsPatch().isEmpty() || ItemStack.isSameItemSameComponents(ingredient, candidate);
    }

    /** The empty grid, for a screen that starts from nothing. */
    public static NonNullList<ItemStack> emptyGrid() {
        return NonNullList.withSize(GRID, ItemStack.EMPTY);
    }
}
