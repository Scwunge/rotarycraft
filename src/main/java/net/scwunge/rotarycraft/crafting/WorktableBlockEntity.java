package net.scwunge.rotarycraft.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.WorktableMenu;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Worktable, as the original: nine slots for a recipe, nine to take what comes out, and a slot for a Craft Pattern. It crafts once on
 * a redstone signal (or when you click the middle output slot; shift-click crafts as many as it can). A lone item in the middle of the grid
 * is taken apart instead, if some recipe makes it, and its ingredients come out in the places they went in. With a pattern in, it takes only
 * that recipe's items. It needs no power.
 * <p>
 * Not here yet: the original's tool charging, jetpack and armour upgrades, which need items the port does not have.
 */
public class WorktableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MATRIX = 9;
    public static final int FIRST_OUTPUT = 9;
    public static final int MAIN_OUTPUT = 13;
    public static final int PATTERN = 18;
    public static final int SLOTS = 19;
    /** How often a table with the redstone upgrade crafts by itself, in ticks. */
    public static final int AUTO_INTERVAL = 6;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < MATRIX) {
                return accepts(slot, stack);
            }
            return slot == PATTERN && CraftPattern.isPattern(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == PATTERN) {
                return 1;
            }
            if (slot < MATRIX) {
                ItemStack pattern = getStackInSlot(PATTERN);
                return CraftPattern.isPattern(pattern) ? Math.min(64, CraftPattern.of(pattern).limit()) : 64;
            }
            return 64;
        }
    };
    private boolean hasUpgrade;
    private boolean wasPowered;

    public WorktableBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingRegistry.WORKTABLE_BE.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    // ---- what goes in ----

    /** With a pattern in the table, a matrix slot takes only what that pattern lays out there. */
    private boolean accepts(int slot, ItemStack stack) {
        ItemStack held = items.getStackInSlot(PATTERN);
        if (!CraftPattern.isPattern(held)) {
            return true;
        }
        CraftPattern pattern = CraftPattern.of(held);
        return pattern.mode() != PatternMode.BLAST_FURNACE && pattern.wants(slot, stack);
    }

    /** What automation sees: put things in the matrix, take things out of the output. */
    public IItemHandler automation() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return SLOTS;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return slot < MATRIX ? items.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot >= FIRST_OUTPUT && slot < PATTERN ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < MATRIX && items.isItemValid(slot, stack);
            }
        };
    }

    // ---- redstone upgrade ----

    public boolean hasRedstoneUpgrade() {
        return hasUpgrade;
    }

    /** With the upgrade the table crafts by itself every {@link #AUTO_INTERVAL} ticks, while it has no redstone signal. */
    public void addRedstoneUpgrade() {
        hasUpgrade = true;
        setChanged();
    }

    // ---- crafting ----

    private CraftingInput matrix() {
        List<ItemStack> grid = new ArrayList<>(MATRIX);
        for (int i = 0; i < MATRIX; i++) {
            grid.add(items.getStackInSlot(i));
        }
        return CraftingInput.of(3, 3, grid);
    }

    private boolean matrixEmpty() {
        for (int i = 0; i < MATRIX; i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** The recipe a grid makes: the Worktable's own recipes first, then a crafting table's. */
    public static Optional<RecipeHolder<? extends Recipe<CraftingInput>>> recipe(Level level, CraftingInput input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }
        Optional<RecipeHolder<? extends Recipe<CraftingInput>>> own = PatternMode.WORKTABLE.find(level, input);
        return own.isPresent() ? own : PatternMode.CRAFTING.find(level, input);
    }

    /** What a craft of this grid would make, or nothing. */
    public static ItemStack preview(Level level, List<ItemStack> grid) {
        CraftingInput input = CraftingInput.of(3, 3, grid);
        return recipe(level, input).map(h -> h.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /** True if the middle output slot can take {@code stack} on top of what it holds. */
    private boolean fitsMain(ItemStack stack) {
        ItemStack in = items.getStackInSlot(MAIN_OUTPUT);
        return in.isEmpty() || ItemStack.isSameItemSameComponents(in, stack) && in.getCount() + stack.getCount() <= Math.min(64, in.getMaxStackSize());
    }

    public boolean outputsEmpty() {
        for (int i = FIRST_OUTPUT; i < PATTERN; i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Puts a stack into the output slots (the middle one first), merging with what is there; returns what did not fit. */
    private ItemStack putOutput(ItemStack stack, boolean mainOnly) {
        ItemStack rest = stack.copy();
        int[] order = {MAIN_OUTPUT, 9, 10, 11, 12, 14, 15, 16, 17};
        for (int slot : order) {
            if (rest.isEmpty() || mainOnly && slot != MAIN_OUTPUT) {
                continue;
            }
            ItemStack in = items.getStackInSlot(slot);
            if (in.isEmpty()) {
                items.setStackInSlot(slot, rest.copy());
                rest = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(in, rest)) {
                int room = Math.min(64, in.getMaxStackSize()) - in.getCount();
                int move = Math.min(room, rest.getCount());
                if (move > 0) {
                    in.grow(move);
                    items.setStackInSlot(slot, in);
                    rest.shrink(move);
                }
            }
        }
        return rest;
    }

    /** Crafts once from the grid; true if it made something. {@code player} (if any) is who it is made for. */
    public boolean craft(@Nullable Player player) {
        if (level == null || level.isClientSide()) {
            return false;
        }
        CraftingInput input = matrix();
        Optional<RecipeHolder<? extends Recipe<CraftingInput>>> found = recipe(level, input);
        if (found.isEmpty()) {
            return false;
        }
        Recipe<CraftingInput> recipe = found.get().value();
        ItemStack result = recipe.assemble(input, level.registryAccess());
        if (result.isEmpty() || !fitsMain(result)) {
            return false;
        }
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);
        for (int i = 0; i < MATRIX; i++) {
            ItemStack in = items.getStackInSlot(i);
            if (!in.isEmpty()) {
                in.shrink(1);
                items.setStackInSlot(i, in.isEmpty() ? ItemStack.EMPTY : in);
            }
            ItemStack left = i < remaining.size() ? remaining.get(i) : ItemStack.EMPTY;
            if (!left.isEmpty()) {
                keepRemainder(i, left);
            }
        }
        putOutput(result, true);
        if (player != null) {
            result.onCraftedBy(level, player, result.getCount());
        }
        level.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.3F, 1.5F);
        setChanged();
        return true;
    }

    /** What a recipe leaves behind (an empty bucket) goes back where it was, or into the output, or onto the ground. */
    private void keepRemainder(int slot, ItemStack left) {
        ItemStack in = items.getStackInSlot(slot);
        if (in.isEmpty()) {
            items.setStackInSlot(slot, left.copy());
            return;
        }
        if (ItemStack.isSameItemSameComponents(in, left) && in.getCount() + left.getCount() <= in.getMaxStackSize()) {
            in.grow(left.getCount());
            items.setStackInSlot(slot, in);
            return;
        }
        ItemStack rest = putOutput(left, false);
        if (!rest.isEmpty() && level != null) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, rest);
        }
    }

    /** Crafts as many times as the grid, the output slot and {@code limit} allow. */
    public int craftAll(@Nullable Player player, int limit) {
        int made = 0;
        while (made < limit && craft(player)) {
            made++;
        }
        return made;
    }

    // ---- taking apart ----

    private static boolean isNotUncraftable(ItemStack stack) {
        return stack.isDamaged() || stack.isEnchanted() || !stack.getComponentsPatch().isEmpty();
    }

    /** The stacks a recipe's grid holds, by grid slot (nine, empty where the recipe has nothing), or null for a recipe that can't be undone. */
    @Nullable
    private static List<ItemStack> layout(Recipe<?> recipe) {
        List<ItemStack> grid = new ArrayList<>(MATRIX);
        for (int i = 0; i < MATRIX; i++) {
            grid.add(ItemStack.EMPTY);
        }
        if (recipe instanceof ShapedRecipe shaped) {
            List<Ingredient> parts = shaped.getIngredients();
            for (int y = 0; y < shaped.getHeight(); y++) {
                for (int x = 0; x < shaped.getWidth(); x++) {
                    Ingredient part = parts.get(x + y * shaped.getWidth());
                    if (!part.isEmpty() && part.getItems().length > 0) {
                        grid.set(x + y * 3, part.getItems()[0].copyWithCount(1));
                    }
                }
            }
            return grid;
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            int i = 0;
            for (Ingredient part : shapeless.getIngredients()) {
                if (i >= MATRIX || part.getItems().length == 0) {
                    return null;
                }
                grid.set(i++, part.getItems()[0].copyWithCount(1));
            }
            return grid;
        }
        return null;
    }

    /** The crafting recipe that makes the item in the middle of the grid (and takes it apart), if there is one. */
    @Nullable
    private RecipeHolder<CraftingRecipe> uncraftingRecipe(ItemStack stack) {
        if (level == null) {
            return null;
        }
        RecipeHolder<CraftingRecipe> best = null;
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            if (recipe.isSpecial() || !(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)) {
                continue;
            }
            ItemStack made = recipe.getResultItem(level.registryAccess());
            if (!ItemStack.isSameItem(made, stack) || made.getCount() > stack.getCount()) {
                continue;
            }
            List<ItemStack> grid = layout(recipe);
            if (grid == null || grid.stream().anyMatch(in -> !in.isEmpty() && in.is(stack.getItem()))) {
                continue;
            }
            if (best == null || holder.id().compareTo(best.id()) < 0) {
                best = holder;
            }
        }
        return best;
    }

    /** True if the grid holds one item, in the middle, which is whole and can be taken apart into outputs with room. */
    public boolean canUncraft() {
        for (int i = 0; i < MATRIX; i++) {
            if (i != 4 && !items.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        ItemStack middle = items.getStackInSlot(4);
        if (middle.isEmpty() || isNotUncraftable(middle)) {
            return false;
        }
        RecipeHolder<CraftingRecipe> recipe = uncraftingRecipe(middle);
        if (recipe == null) {
            return false;
        }
        List<ItemStack> grid = layout(recipe.value());
        for (int i = 0; i < MATRIX; i++) {
            ItemStack part = grid.get(i);
            ItemStack in = items.getStackInSlot(FIRST_OUTPUT + i);
            if (!part.isEmpty() && !in.isEmpty() && (!ItemStack.isSameItemSameComponents(in, part) || in.getCount() >= Math.min(64, in.getMaxStackSize()))) {
                return false;
            }
        }
        return true;
    }

    private void uncraft() {
        ItemStack middle = items.getStackInSlot(4);
        RecipeHolder<CraftingRecipe> recipe = uncraftingRecipe(middle);
        if (recipe == null) {
            return;
        }
        List<ItemStack> grid = layout(recipe.value());
        middle.shrink(recipe.value().getResultItem(level.registryAccess()).getCount());
        items.setStackInSlot(4, middle.isEmpty() ? ItemStack.EMPTY : middle);
        for (int i = 0; i < MATRIX; i++) {
            ItemStack part = grid.get(i);
            if (part.isEmpty()) {
                continue;
            }
            ItemStack in = items.getStackInSlot(FIRST_OUTPUT + i);
            if (in.isEmpty()) {
                items.setStackInSlot(FIRST_OUTPUT + i, part.copy());
            } else {
                in.grow(1);
                items.setStackInSlot(FIRST_OUTPUT + i, in);
            }
        }
        level.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.3F, 1.5F);
        setChanged();
    }

    // ---- redstone and ticking ----

    /** The block's neighbors changed: a signal that has just arrived crafts, or takes apart. */
    public void redstoneChanged(boolean powered) {
        if (powered && !wasPowered) {
            work();
        }
        if (powered != wasPowered) {
            wasPowered = powered;
            setChanged();
        }
    }

    private void work() {
        if (level == null || level.isClientSide()) {
            return;
        }
        if (!craft(null) && canUncraft()) {
            uncraft();
        }
    }

    public void serverTick() {
        if (hasUpgrade && level.getGameTime() % AUTO_INTERVAL == 0 && !level.hasNeighborSignal(worldPosition) && !matrixEmpty()) {
            work();
        }
    }

    /** What to drop when the block is broken. */
    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(i));
        }
    }

    // ---- screen ----

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WorktableMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putBoolean("redstoneUpgrade", hasUpgrade);
        tag.putBoolean("powered", wasPowered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        hasUpgrade = tag.getBoolean("redstoneUpgrade");
        wasPowered = tag.getBoolean("powered");
    }
}
