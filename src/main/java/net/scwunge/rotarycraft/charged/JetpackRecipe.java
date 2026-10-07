package net.scwunge.rotarycraft.charged;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.GadgetRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.registry.RotaryRecipes;
import net.scwunge.rotarycraft.registry.ToolRegistry;
import net.scwunge.rotarycraft.tool.BedrockTools;
import net.scwunge.rotarycraft.tool.Forced;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the original's Worktable did with jetpacks, as one shapeless recipe: a steel or bedrock chestplate and a jetpack make the chestplate with the pack
 * built in (keeping its fuel and upgrades); a pack and three ingots of its metal (steel for the plain one) get wings; a pack and two cooling fins get fin cooling; a pack
 * and a Jet Engine get the thrust boost; and a lone steel or bedrock pack comes apart into its chestplate and its jetpack.
 */
public class JetpackRecipe extends CustomRecipe {
    public JetpackRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static boolean isPack(ItemStack stack) {
        return stack.getItem() instanceof Jetpack;
    }

    private static boolean isIngot(ItemStack stack, boolean bedrock) {
        return bedrock ? stack.is(net.scwunge.rotarycraft.registry.RotaryParts.part("bedrock_ingot").get()) : stack.is(ToolRegistry.STEEL_INGOTS);
    }

    private static List<ItemStack> contents(CraftingInput input) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : input.items()) {
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }

    private static ItemStack firstOf(List<ItemStack> items, java.util.function.Predicate<ItemStack> test) {
        return items.stream().filter(test).findFirst().orElse(ItemStack.EMPTY);
    }

    /** What the grid is, if it is any of the recipes. */
    private enum Job {
        NONE, BUILD, WINGS, COOLING, THRUST, UNBUILD
    }

    private static Job job(List<ItemStack> items) {
        if (items.size() == 1) {
            return isPack(items.get(0)) && ((Jetpack) items.get(0).getItem()).kind() != Jetpack.Kind.PLAIN ? Job.UNBUILD : Job.NONE;
        }
        ItemStack pack = firstOf(items, JetpackRecipe::isPack);
        if (items.size() == 2) {
            ItemStack other = firstOf(items, s -> !isPack(s));
            if (isPack(pack) && ((Jetpack) pack.getItem()).kind() == Jetpack.Kind.PLAIN && !other.isEmpty()
                    && (other.is(ToolRegistry.STEEL_CHESTPLATE.get()) || other.is(ToolRegistry.BEDROCK_CHESTPLATE.get()))) {
                return Job.BUILD;
            }
            if (isPack(pack) && other.is(RotaryBlocks.JET_ENGINE.asItem()) && !Jetpack.Upgrade.JET.on(pack)) {
                return Job.THRUST;
            }
            return Job.NONE;
        }
        if (!isPack(pack) || items.stream().filter(JetpackRecipe::isPack).count() != 1) {
            return Job.NONE;
        }
        List<ItemStack> rest = items.stream().filter(s -> !isPack(s)).toList();
        if (rest.size() == 3 && !Jetpack.Upgrade.WING.on(pack) && rest.stream().allMatch(s -> isIngot(s, ((Jetpack) pack.getItem()).kind() == Jetpack.Kind.BEDROCK))) {
            return Job.WINGS;
        }
        if (rest.size() == 2 && !Jetpack.Upgrade.COOLING.on(pack) && rest.stream().allMatch(s -> s.is(RotaryBlocks.COOLING_FIN.asItem()))) {
            return Job.COOLING;
        }
        return Job.NONE;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        List<ItemStack> items = contents(input);
        if (items.isEmpty()) {
            return false;
        }
        if (job(items) == Job.BUILD) {
            ItemStack plate = firstOf(items, s -> !isPack(s));
            return !plate.isEmpty();
        }
        return job(items) != Job.NONE;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        List<ItemStack> items = contents(input);
        ItemStack pack = firstOf(items, JetpackRecipe::isPack);
        switch (job(items)) {
            case BUILD -> {
                ItemStack plate = firstOf(items, s -> !isPack(s));
                boolean bedrock = plate.is(ToolRegistry.BEDROCK_CHESTPLATE.get());
                ItemStack out = bedrock ? Forced.stackOf(GadgetRegistry.BEDROCK_JETPACK.get(), registries, BedrockTools.CHESTPLATE) : new ItemStack(GadgetRegistry.STEEL_JETPACK.get());
                carry(pack, out);
                return out;
            }
            case WINGS, COOLING, THRUST -> {
                ItemStack out = pack.copyWithCount(1);
                Jetpack.Upgrade upgrade = switch (job(items)) {
                    case WINGS -> Jetpack.Upgrade.WING;
                    case COOLING -> Jetpack.Upgrade.COOLING;
                    default -> Jetpack.Upgrade.JET;
                };
                upgrade.set(out);
                return out;
            }
            case UNBUILD -> {
                return ((Jetpack) pack.getItem()).kind() == Jetpack.Kind.BEDROCK
                        ? Forced.stackOf(ToolRegistry.BEDROCK_CHESTPLATE.get(), registries, BedrockTools.CHESTPLATE) : new ItemStack(ToolRegistry.STEEL_CHESTPLATE.get());
            }
            default -> {
                return ItemStack.EMPTY;
            }
        }
    }

    /** The fuel and the upgrades of one pack onto another. */
    static void carry(ItemStack from, ItemStack to) {
        if (from.has(RotaryComponents.ITEM_FLUID.get())) {
            to.set(RotaryComponents.ITEM_FLUID.get(), from.get(RotaryComponents.ITEM_FLUID.get()));
        }
        if (from.has(RotaryComponents.PACK_UPGRADES.get())) {
            to.set(RotaryComponents.PACK_UPGRADES.get(), from.get(RotaryComponents.PACK_UPGRADES.get()));
        }
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        List<ItemStack> items = contents(input);
        if (job(items) == Job.UNBUILD) {
            for (int i = 0; i < input.size(); i++) {
                if (isPack(input.getItem(i))) {
                    ItemStack jetpack = new ItemStack(GadgetRegistry.JETPACK.get());
                    carry(input.getItem(i), jetpack);
                    remaining.set(i, jetpack);
                }
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RotaryRecipes.JETPACK_SERIALIZER.get();
    }

    public static RecipeSerializer<JetpackRecipe> serializer() {
        return new SimpleCraftingRecipeSerializer<>(JetpackRecipe::new);
    }
}
