package net.scwunge.rotarycraft.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.menu.AutoCrafterMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import net.scwunge.rotarycraft.weapon.turret.OmniConsumerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Auto-Crafter, as the original: eighteen Craft Patterns (crafting recipes), each with an output slot, which it makes from what is in
 * the inventory directly above it, when it has shaft power (1 kW or more, from any side). In Request mode a button over a pattern makes
 * one batch of it; in Continuous mode it keeps making all of them while it can. If a pattern's ingredient is missing it first tries to make
 * it from another pattern of the machine (down to forty links deep, never looping), using what is already in that pattern's output first.
 * Whatever a recipe gives back (empty buckets) goes in a hidden slot per pattern, which must be clear for the next craft. Automation takes
 * from the outputs and gives it patterns.
 * <p>
 * Not here: the original's Sustain mode and the ME network links, which need Applied Energistics.
 */
public class AutoCrafterBlockEntity extends OmniConsumerBlockEntity implements MenuProvider, MachineInteractions {
    public static final int SIZE = 18;
    public static final int OUTPUT_OFFSET = SIZE;
    public static final int CONTAINER_OFFSET = SIZE * 2;
    public static final int SLOTS = SIZE * 3;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    /** Ticks between rounds in Continuous mode. */
    public static final int INTERVAL = 2;
    private static final int MAX_DEPTH = 40;
    /** How long a pattern's lamp stays lit after it crafts, in ticks. */
    public static final int FLASH = 5;

    public enum Mode {
        REQUEST(0xFF0000),
        CONTINUOUS(0x00AAFF);

        public final int color;

        Mode(int color) {
            this.color = color;
        }

        public Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Component label() {
            return Component.translatable("gui.rotarycraft.crafter_mode." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    /** What a pattern makes, worked out from the recipe it names. */
    private record Plan(RecipeHolder<? extends Recipe<CraftingInput>> holder, CraftingInput input, List<ItemStack> totals) {
        ItemStack output(net.minecraft.core.HolderLookup.Provider registries) {
            return holder.value().assemble(input, registries);
        }

        NonNullList<ItemStack> remaining() {
            return holder.value().getRemainingItems(input);
        }
    }

    private static final Plan NONE = new Plan(null, null, List.of());

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot < SIZE) {
                plans[slot] = null;
            }
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot < SIZE && isUsablePattern(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot < SIZE ? 1 : 64;
        }
    };
    private final Plan[] plans = new Plan[SIZE];
    private final int[] crafting = new int[SIZE];
    private Mode mode = Mode.REQUEST;
    private int tick;

    public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(CraftingRegistry.AUTO_CRAFTER_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public ItemStackHandler items() {
        return items;
    }

    public Mode mode() {
        return mode;
    }

    public void setMode(Mode next) {
        mode = next;
        setChanged();
    }

    public int flash(int slot) {
        return crafting[slot];
    }

    /** A pattern the machine can make something from: a crafting pattern with a recipe in it. */
    public static boolean isUsablePattern(ItemStack stack) {
        if (!CraftPattern.isPattern(stack)) {
            return false;
        }
        CraftPattern pattern = CraftPattern.of(stack);
        return pattern.mode() == PatternMode.CRAFTING && pattern.hasRecipe();
    }

    /** What automation sees: patterns go in, and the made things and returned containers come out. */
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
                return slot < SIZE ? items.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return slot >= SIZE ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot < SIZE && items.isItemValid(slot, stack);
            }
        };
    }

    // ---- plans ----

    @Nullable
    private Plan plan(int slot) {
        if (level == null) {
            return null;
        }
        Plan cached = plans[slot];
        if (cached == NONE) {
            return null;
        }
        if (cached != null && level.getRecipeManager().byKey(cached.holder().id()).isPresent()) {
            return cached;
        }
        ItemStack stack = items.getStackInSlot(slot);
        Plan made = NONE;
        if (isUsablePattern(stack)) {
            CraftPattern pattern = CraftPattern.of(stack);
            Optional<RecipeHolder<? extends Recipe<CraftingInput>>> recipe = pattern.recipe(level);
            if (recipe.isPresent()) {
                made = new Plan(recipe.get(), pattern.input(), pattern.ingredientTotals());
            }
        }
        plans[slot] = made;
        return made == NONE ? null : made;
    }

    /** What the pattern in a slot makes, or nothing. */
    public ItemStack recipeOutput(int slot) {
        Plan plan = plan(slot);
        return plan == null ? ItemStack.EMPTY : plan.output(level.registryAccess());
    }

    // ---- the work ----

    @Override
    protected void machineTick(boolean powered) {
        for (int i = 0; i < SIZE; i++) {
            if (crafting[i] > 0) {
                crafting[i]--;
            }
        }
        if (level == null || level.isClientSide() || !powered) {
            return;
        }
        if (++tick >= INTERVAL && mode == Mode.CONTINUOUS) {
            tick = 0;
            for (int i = 0; i < SIZE; i++) {
                craftSlot(i);
            }
        }
    }

    /** Makes one batch of what the pattern in {@code slot} makes, if the machine has power; true if it did. */
    public boolean request(int slot) {
        return slot >= 0 && slot < SIZE && hasEnoughPower() && craftSlot(slot);
    }

    public boolean craftSlot(int slot) {
        return tryCrafting(slot, 0, new HashSet<>(), new HashMap<>());
    }

    @Nullable
    private IItemHandler source() {
        return level == null ? null : level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.above(), Direction.DOWN);
    }

    private int available(ItemStack ingredient) {
        IItemHandler source = source();
        int count = 0;
        if (source != null) {
            for (int i = 0; i < source.getSlots(); i++) {
                ItemStack in = source.getStackInSlot(i);
                if (CraftPattern.matches(ingredient, in)) {
                    count += in.getCount();
                }
            }
        }
        return count;
    }

    private void removeFromSource(ItemStack ingredient, int amount) {
        IItemHandler source = source();
        for (int i = 0; source != null && amount > 0 && i < source.getSlots(); i++) {
            if (CraftPattern.matches(ingredient, source.getStackInSlot(i))) {
                amount -= source.extractItem(i, amount, false).getCount();
            }
        }
    }

    /** The one kind of thing a recipe leaves behind, or null if it leaves two kinds (which the single return slot can't hold). */
    @Nullable
    private static ItemStack returned(List<ItemStack> remaining) {
        ItemStack kept = ItemStack.EMPTY;
        for (ItemStack left : remaining) {
            if (left.isEmpty()) {
                continue;
            }
            if (kept.isEmpty()) {
                kept = left.copy();
            } else if (ItemStack.isSameItemSameComponents(kept, left)) {
                kept.grow(left.getCount());
            } else {
                return null;
            }
        }
        return kept;
    }

    /**
     * One batch of what the pattern in {@code slot} makes. {@code claims} counts what outputs the crafts above this one in the chain have
     * set aside for themselves, so that this one does not take it too.
     */
    private boolean tryCrafting(int slot, int depth, Set<Item> path, Map<Integer, Integer> claims) {
        Plan plan = plan(slot);
        if (plan == null) {
            return false;
        }
        ItemStack out = plan.output(level.registryAccess());
        if (out.isEmpty() || path.contains(out.getItem())) {
            return false;
        }
        ItemStack inOut = items.getStackInSlot(slot + OUTPUT_OFFSET);
        if (!inOut.isEmpty() && (!ItemStack.isSameItemSameComponents(inOut, out) || inOut.getCount() + out.getCount() > out.getMaxStackSize())) {
            return false;
        }
        if (!items.getStackInSlot(slot + CONTAINER_OFFSET).isEmpty()) {
            return false;
        }
        ItemStack back = returned(plan.remaining());
        if (back == null) {
            return false;
        }
        Set<Item> below = new HashSet<>(path);
        below.add(out.getItem());
        Map<Integer, Integer> mine = new HashMap<>();
        for (ItemStack req : plan.totals()) {
            int missing = req.getCount() - available(req);
            if (missing > 0 && (depth >= MAX_DEPTH || gather(missing, req, depth + 1, below, claims, mine) < missing)) {
                release(mine, claims);
                return false;
            }
        }
        for (Map.Entry<Integer, Integer> taken : mine.entrySet()) {
            ItemStack held = items.getStackInSlot(taken.getKey() + OUTPUT_OFFSET);
            held.shrink(taken.getValue());
            items.setStackInSlot(taken.getKey() + OUTPUT_OFFSET, held.isEmpty() ? ItemStack.EMPTY : held);
        }
        release(mine, claims);
        for (ItemStack req : plan.totals()) {
            removeFromSource(req, req.getCount());
        }
        ItemStack made = items.getStackInSlot(slot + OUTPUT_OFFSET);
        if (made.isEmpty()) {
            items.setStackInSlot(slot + OUTPUT_OFFSET, out);
        } else {
            made.grow(out.getCount());
            items.setStackInSlot(slot + OUTPUT_OFFSET, made);
        }
        if (!back.isEmpty()) {
            items.setStackInSlot(slot + CONTAINER_OFFSET, back);
        }
        crafting[slot] = FLASH;
        setChanged();
        return true;
    }

    /**
     * Finds {@code num} of an ingredient the source has too few of, from the other patterns: what their outputs already hold, and then what
     * they can be made to make. What it takes from an output is noted in {@code mine} (and {@code claims}) and removed only once the whole
     * craft is sure to go ahead, so a craft that fails part way loses nothing. Returns how many it found.
     */
    private int gather(int num, ItemStack ingredient, int depth, Set<Item> path, Map<Integer, Integer> claims, Map<Integer, Integer> mine) {
        int got = 0;
        for (int j = 0; j < SIZE && got < num; j++) {
            Plan other = plan(j);
            if (other == null || !CraftPattern.matches(ingredient, other.output(level.registryAccess()))) {
                continue;
            }
            got += claim(j, num - got, claims, mine);
            while (got < num && tryCrafting(j, depth, path, claims)) {
                got += claim(j, num - got, claims, mine);
            }
        }
        return got;
    }

    /** Sets aside up to {@code want} of what the output of pattern {@code slot} holds that is not already set aside. */
    private int claim(int slot, int want, Map<Integer, Integer> claims, Map<Integer, Integer> mine) {
        int free = items.getStackInSlot(slot + OUTPUT_OFFSET).getCount() - claims.getOrDefault(slot, 0);
        int use = Math.max(0, Math.min(free, want));
        if (use > 0) {
            claims.merge(slot, use, Integer::sum);
            mine.merge(slot, use, Integer::sum);
        }
        return use;
    }

    private static void release(Map<Integer, Integer> mine, Map<Integer, Integer> claims) {
        mine.forEach((slot, n) -> claims.merge(slot, -n, Integer::sum));
        mine.clear();
    }

    // ---- screwdriver ----

    @Override
    public boolean onScrewdriver(UseOnContext context) {
        if (!context.getLevel().isClientSide()) {
            setMode(mode.next());
        }
        return true;
    }

    // ---- screen ----

    public static final int DATA_COUNT = 3 + SIZE;

    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> torque;
                    case 1 -> omega;
                    case 2 -> mode.ordinal();
                    default -> index >= 3 && index < DATA_COUNT ? crafting[index - 3] : 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AutoCrafterMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(i));
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("mode", mode.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        mode = Mode.values()[Math.floorMod(tag.getInt("mode"), Mode.values().length)];
        java.util.Arrays.fill(plans, null);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writePower(tag);
        tag.putInt("mode", mode.ordinal());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readPower(tag);
        mode = Mode.values()[Math.floorMod(tag.getInt("mode"), Mode.values().length)];
    }

    /** The default would load the packet as if it were saved data, skipping {@link #handleUpdateTag}, so clients would never see the sync. */
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
