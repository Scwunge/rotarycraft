package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.BlastFurnaceMenu;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.recipe.BlastCraftingRecipe;
import net.scwunge.rotarycraft.recipe.BlastFurnaceRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Blast Furnace, as in the original. Not shaft-powered: it works by temperature, which settles towards the surroundings
 * (+200 C next to fire, +600 C next to lava, halved next to water, ice melts) at 1-2 C a second, or is driven up fast by a
 * Friction Heater. HSLA steel needs 600 C. An operation takes 2 x (1500 - (T - 600)) / 12 ticks and turns one item from each
 * filled grid slot into the result; additives are used up by chance per item made.
 */
public class BlastFurnaceBlockEntity extends PowerBlockEntity implements MenuProvider, Heatable {
    public static final int SMELT_TEMPERATURE = 600;
    public static final int MAX_TEMPERATURE = 2000;
    public static final int SLOT_CENTER_ADDITIVE = 0;
    public static final int SLOT_OUTPUT_CENTER = 10;
    public static final int SLOT_LOWER_ADDITIVE = 11;
    public static final int SLOT_OUTPUT_UPPER = 12;
    public static final int SLOT_OUTPUT_LOWER = 13;
    public static final int SLOT_UPPER_ADDITIVE = 14;
    public static final int SLOTS = 15;
    private static final int[] ADDITIVE_SLOTS = {SLOT_CENTER_ADDITIVE, SLOT_LOWER_ADDITIVE, SLOT_UPPER_ADDITIVE};
    private static final int[] OUTPUT_SLOTS = {SLOT_OUTPUT_CENTER, SLOT_OUTPUT_UPPER, SLOT_OUTPUT_LOWER};

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !isOutput(slot);
        }
    };
    /** Automation: from above into the grid, from the sides into the additives, outputs out of any side. */
    private final IItemHandler automation = new IItemHandler() {
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
            return isOutput(slot) ? stack : items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isOutput(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !isOutput(slot);
        }
    };

    private int temperature = Integer.MIN_VALUE;
    private int smeltTime;
    private int operationTime = 1;
    private int tickCount;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> getTemperature();
                case 1 -> smeltTime;
                case 2 -> operationTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return BlastFurnaceMenu.DATA_COUNT;
        }
    };

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.BLAST_FURNACE.get(), pos, state);
    }

    static boolean isOutput(int slot) {
        return slot == SLOT_OUTPUT_CENTER || slot == SLOT_OUTPUT_UPPER || slot == SLOT_OUTPUT_LOWER;
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    @Override
    protected boolean outputsPower() {
        return false;
    }

    @Override
    public int getTemperature() {
        return temperature == Integer.MIN_VALUE ? ambient() : temperature;
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature = Math.min(MAX_TEMPERATURE, getTemperature() + amount);
        setChanged();
    }

    /** Test hook and command use: set the temperature directly. */
    public void setTemperature(int t) {
        temperature = Math.max(-273, Math.min(MAX_TEMPERATURE, t));
        setChanged();
    }

    public static int operationTime(int temperature) {
        return Math.max(1, 2 * ((1500 - (temperature - SMELT_TEMPERATURE)) / 12));
    }

    private int ambient() {
        return Ambient.temperature(level, worldPosition);
    }

    private void updateTemperature() {
        int tAmb = ambient();
        boolean water = false;
        boolean fire = false;
        boolean lava = false;
        Direction iceSide = null;
        for (Direction d : Direction.values()) {
            BlockPos p = worldPosition.relative(d);
            BlockState s = level.getBlockState(p);
            if (s.getFluidState().is(FluidTags.WATER)) {
                water = true;
            }
            if (s.getFluidState().is(FluidTags.LAVA)) {
                lava = true;
            }
            if (s.getBlock() instanceof BaseFireBlock) {
                fire = true;
            }
            if (iceSide == null && (s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE))) {
                iceSide = d;
            }
        }
        if (water) {
            tAmb /= 2;
        }
        if (iceSide != null) {
            if (tAmb > 0) {
                tAmb /= 4;
            }
            level.setBlockAndUpdate(worldPosition.relative(iceSide), Blocks.WATER.defaultBlockState());
        }
        int add = 0;
        if (fire) {
            add += tAmb >= 100 ? 100 : 200;
        }
        if (lava) {
            add += tAmb >= 100 ? 400 : 600;
        }
        tAmb += add;
        int t = getTemperature();
        if (t > tAmb) {
            t--;
        }
        if (t > tAmb * 2) {
            t--;
        }
        if (t < tAmb) {
            t++;
        }
        if (t * 2 < tAmb) {
            t++;
        }
        temperature = Math.min(MAX_TEMPERATURE, t);
        setChanged();
    }

    private record Match(BlastFurnaceRecipe recipe, int mainItems, int produced) {
    }

    private Optional<Match> findRecipe() {
        int t = getTemperature();
        for (RecipeHolder<BlastFurnaceRecipe> holder : level.getRecipeManager().getAllRecipesFor(RotaryRecipes.BLAST_FURNACE.get())) {
            BlastFurnaceRecipe r = holder.value();
            if (t < r.temperature()) {
                continue;
            }
            int main = 0;
            boolean stray = false;
            for (int slot = 1; slot <= 9; slot++) {
                ItemStack s = items.getStackInSlot(slot);
                if (s.isEmpty()) {
                    continue;
                }
                if (r.main().test(s)) {
                    main++;
                } else {
                    stray = true;
                }
            }
            if (stray || main == 0 || r.produced(main) <= 0) {
                continue;
            }
            boolean additivesOk = true;
            for (int i = 0; i < 3; i++) {
                Optional<BlastFurnaceRecipe.Additive> a = r.additives().get(i);
                if (a.isPresent() && !a.get().ingredient().test(items.getStackInSlot(ADDITIVE_SLOTS[i]))) {
                    additivesOk = false;
                }
            }
            if (!additivesOk) {
                continue;
            }
            int produced = r.produced(main) * r.result().getCount();
            if (outputSlotFor(r.result().copyWithCount(produced)) < 0) {
                continue;
            }
            return Optional.of(new Match(r, main, produced));
        }
        return Optional.empty();
    }

    /** A shaped recipe laid out in the grid that the furnace is hot enough for, with room for its result. */
    private Optional<BlastCraftingRecipe> findCrafting() {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int slot = 1; slot <= 9; slot++) {
            grid.add(items.getStackInSlot(slot));
        }
        CraftingInput input = CraftingInput.of(3, 3, grid);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        int t = getTemperature();
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.BLAST_CRAFTING.get(), input, level)
                .map(RecipeHolder::value)
                .filter(r -> t >= r.temperature() && outputSlotFor(r.result()) >= 0);
    }

    private void craft(BlastCraftingRecipe c) {
        ItemStack out = c.result().copy();
        int slot = outputSlotFor(out);
        if (slot < 0) {
            return;
        }
        ItemStack cur = items.getStackInSlot(slot);
        items.setStackInSlot(slot, cur.isEmpty() ? out : cur.copyWithCount(cur.getCount() + out.getCount()));
        for (int s = 1; s <= 9; s++) {
            if (!items.getStackInSlot(s).isEmpty()) {
                items.extractItem(s, 1, false);
            }
        }
        if (c.xp() > 0 && level instanceof ServerLevel server) {
            ExperienceOrb.award(server, Vec3.atCenterOf(worldPosition).add(0, 0.7, 0), Math.round(c.xp() * out.getCount()));
        }
    }

    private int outputSlotFor(ItemStack out) {
        for (int slot : OUTPUT_SLOTS) {
            ItemStack cur = items.getStackInSlot(slot);
            if (cur.isEmpty() || (ItemStack.isSameItemSameComponents(cur, out) && cur.getCount() + out.getCount() <= cur.getMaxStackSize())) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }
        if (++tickCount % 20 == 0) {
            updateTemperature();
        }
        Optional<BlastCraftingRecipe> crafting = findCrafting();
        if (crafting.isPresent()) {
            BlastCraftingRecipe c = crafting.get();
            operationTime = operationTime(getTemperature());
            if (c.speed() <= 1 || level.getGameTime() % c.speed() == 0) {
                smeltTime++;
            }
            if (smeltTime >= operationTime) {
                smeltTime = 0;
                craft(c);
            }
            setChanged();
            return;
        }
        Optional<Match> match = findRecipe();
        if (match.isEmpty()) {
            if (smeltTime != 0) {
                smeltTime = 0;
                setChanged();
            }
            return;
        }
        operationTime = operationTime(getTemperature());
        if (++smeltTime < operationTime) {
            setChanged();
            return;
        }
        smeltTime = 0;
        make(match.get());
    }

    private void make(Match m) {
        BlastFurnaceRecipe r = m.recipe();
        int num = m.produced();
        int made = num;
        if (r.bonusYield() > 0) {
            double chance = Math.pow(1.005, (double) num * num) - 1;
            if (level.random.nextDouble() < chance) {
                num = (int) (num * (1 + level.random.nextFloat() * r.bonusYield()));
            }
        }
        ItemStack out = r.result().copyWithCount(num);
        int slot = outputSlotFor(out);
        if (slot < 0) {
            out = r.result().copyWithCount(made);
            slot = outputSlotFor(out);
            if (slot < 0) {
                return;
            }
        }
        ItemStack cur = items.getStackInSlot(slot);
        if (cur.isEmpty()) {
            items.setStackInSlot(slot, out);
        } else {
            items.setStackInSlot(slot, cur.copyWithCount(cur.getCount() + out.getCount()));
        }
        for (int i = 0; i < 3; i++) {
            Optional<BlastFurnaceRecipe.Additive> a = r.additives().get(i);
            if (a.isEmpty()) {
                continue;
            }
            float chance = Math.max(a.get().chance(), Math.min(1, a.get().chance() * made));
            for (int n = 0; n < a.get().count(); n++) {
                if (level.random.nextFloat() < chance) {
                    items.extractItem(ADDITIVE_SLOTS[i], 1, false);
                }
            }
        }
        for (int s = 1; s <= 9; s++) {
            if (!items.getStackInSlot(s).isEmpty()) {
                items.extractItem(s, 1, false);
            }
        }
        if (r.xp() > 0 && level instanceof ServerLevel server) {
            ExperienceOrb.award(server, Vec3.atCenterOf(worldPosition).add(0, 0.7, 0), Math.round(r.xp() * made));
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.blast_furnace");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BlastFurnaceMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("temperature", getTemperature());
        tag.putInt("smeltTime", smeltTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        smeltTime = tag.getInt("smeltTime");
    }
}
