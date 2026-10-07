package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.item.OreProduct;
import net.scwunge.rotarycraft.item.OreProductItem;
import net.scwunge.rotarycraft.menu.ExtractorMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.ExtractionRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.List;
import java.util.Optional;

/**
 * Extractor: four stages, each with its own power need and timing (the original's values), from one shaft:
 * <ol>
 * <li>ore -> dust: 512 N*m, 64 kW, 900 - 60 log2(w) ticks</li>
 * <li>dust -> slurry: 2048 rad/s, 16 kW, 400 - 20 log2(w), 125 mB water</li>
 * <li>slurry -> solution: 8192 rad/s, 32 kW, 600 - 30 log2(w), 125 mB water</li>
 * <li>solution -> flakes: 256 N*m, 64 kW, 1200 - 80 log2(w), with a chance of a bonus item</li>
 * </ol>
 * Every stage doubles its output by chance (50% common ores, 80% nether, 90% rare), so one ore averages about 5 flakes.
 * A stage's output moves on to the next stage's input by itself. Flakes smelt into the metal in a furnace.
 */
public class ExtractorBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement[] STAGE_REQUIREMENTS = {
            new PowerRequirement(512, 1, 65536),
            new PowerRequirement(1, 2048, 16384),
            new PowerRequirement(1, 8192, 32768),
            new PowerRequirement(256, 1, 65536),
    };
    private static final int[][] DURATIONS = {{900, 60}, {400, 20}, {600, 30}, {1200, 80}};
    public static final int WATER_CAPACITY = 16_000;
    public static final int WATER_PER_OPERATION = 125;
    public static final int STAGES = 4;
    public static final int SLOT_BONUS = 8;
    /** With extractorWear on, the first stage wears a drill out after this many operations; a new one goes in this slot. */
    public static final int SLOT_DRILL = 9;
    public static final int DRILL_LIFE = 4096;

    private int drillTime = DRILL_LIFE;

    private final ItemStackHandler items = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) {
                return findRecipeForOre(stack).isPresent();
            }
            if (slot == SLOT_DRILL) {
                return stack.is(net.scwunge.rotarycraft.registry.RotaryParts.part("drill").get());
            }
            if (slot < STAGES) {
                return stack.getItem() == stageInputItem(slot) && stack.has(RotaryComponents.ORE_PRODUCT.get());
            }
            return false;
        }
    };
    private final FluidTank water = new FluidTank(WATER_CAPACITY, s -> s.is(FluidTags.WATER)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** Automation: ore goes into stage 1, finished flakes and bonus items come out. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOT_DRILL;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? items.insertItem(0, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 7 || slot == SLOT_BONUS ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && items.isItemValid(0, stack);
        }
    };

    private final int[] progress = new int[STAGES];
    private final int[] operationTime = {900, 400, 600, 1200};

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < STAGES) {
                return progress[index];
            }
            if (index < 2 * STAGES) {
                return operationTime[index - STAGES];
            }
            return switch (index - 2 * STAGES) {
                case 0 -> water.getFluidAmount();
                case 1 -> torque & 0xFFFF;
                case 2 -> torque >>> 16;
                case 3 -> omega & 0xFFFF;
                case 4 -> omega >>> 16;
                case 5 -> drillTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return ExtractorMenu.DATA_COUNT;
        }
    };

    public ExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.EXTRACTOR.get(), pos, state);
    }

    public static boolean wears() {
        return net.scwunge.rotarycraft.config.RotaryConfig.get(net.scwunge.rotarycraft.config.RotaryConfig.EXTRACTOR_WEAR);
    }

    /** How many operations the drill in the machine has left. */
    public int drillTime() {
        return drillTime;
    }

    public void setDrillTime(int time) {
        drillTime = time;
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidTank water() {
        return water;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    public int progress(int stage) {
        return progress[stage];
    }

    /** The extractor runs each stage whose own requirement is met, so the base check isn't used. */
    @Override
    public PowerRequirement requirement() {
        return STAGE_REQUIREMENTS[0];
    }

    public boolean stagePowered(int stage) {
        return STAGE_REQUIREMENTS[stage].isMetBy(torque, omega);
    }

    static Item stageInputItem(int stage) {
        return switch (stage) {
            case 1 -> RotaryItems.ORE_DUST.get();
            case 2 -> RotaryItems.ORE_SLURRY.get();
            case 3 -> RotaryItems.ORE_SOLUTION.get();
            default -> null;
        };
    }

    static Item stageOutputItem(int stage) {
        return switch (stage) {
            case 0 -> RotaryItems.ORE_DUST.get();
            case 1 -> RotaryItems.ORE_SLURRY.get();
            case 2 -> RotaryItems.ORE_SOLUTION.get();
            default -> RotaryItems.ORE_FLAKES.get();
        };
    }

    private Optional<ExtractionRecipe> findRecipeForOre(ItemStack ore) {
        if (level == null || ore.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.EXTRACTION.get(), new SingleRecipeInput(ore), level).map(RecipeHolder::value);
    }

    private Optional<ExtractionRecipe> findRecipeForType(String type) {
        if (level == null) {
            return Optional.empty();
        }
        List<RecipeHolder<ExtractionRecipe>> all = level.getRecipeManager().getAllRecipesFor(RotaryRecipes.EXTRACTION.get());
        return all.stream().map(RecipeHolder::value).filter(r -> r.type().equals(type)).findFirst();
    }

    /** The ore recipe behind whatever is in a stage's input slot. */
    private Optional<ExtractionRecipe> recipeFor(int stage) {
        ItemStack in = items.getStackInSlot(stage);
        if (in.isEmpty()) {
            return Optional.empty();
        }
        if (stage == 0) {
            return findRecipeForOre(in);
        }
        OreProduct product = in.get(RotaryComponents.ORE_PRODUCT.get());
        return product == null ? Optional.empty() : findRecipeForType(product.type());
    }

    private boolean fits(int slot, ItemStack add) {
        ItemStack cur = items.getStackInSlot(slot);
        return cur.isEmpty() || (ItemStack.isSameItemSameComponents(cur, add) && cur.getCount() + add.getCount() <= cur.getMaxStackSize());
    }

    private void addTo(int slot, ItemStack add) {
        ItemStack cur = items.getStackInSlot(slot);
        if (cur.isEmpty()) {
            items.setStackInSlot(slot, add.copy());
        } else {
            ItemStack grown = cur.copy();
            grown.grow(add.getCount());
            items.setStackInSlot(slot, grown);
        }
    }

    @Override
    protected void machineTick(boolean basePowered) {
        if (!wears()) {
            drillTime = DRILL_LIFE;
        } else if (drillTime <= 0 && !items.getStackInSlot(SLOT_DRILL).isEmpty()) {
            items.extractItem(SLOT_DRILL, 1, false);
            drillTime = DRILL_LIFE;
        }
        for (int stage = 0; stage < STAGES; stage++) {
            tickStage(stage);
        }
        // finished output of each stage moves on to the next stage's input
        for (int stage = 1; stage < STAGES; stage++) {
            ItemStack out = items.getStackInSlot(stage + 3);
            if (!out.isEmpty()) {
                ItemStack leftover = items.insertItem(stage, out, false);
                items.setStackInSlot(stage + 3, leftover);
            }
        }
    }

    private void tickStage(int stage) {
        Optional<ExtractionRecipe> recipe = recipeFor(stage);
        boolean usesWater = stage == 1 || stage == 2;
        // room for a doubled result (as the original checks)
        boolean canRun = stagePowered(stage) && recipe.isPresent() && (stage != 0 || drillTime > 0)
                && (!usesWater || water.getFluidAmount() >= WATER_PER_OPERATION)
                && fits(stage + 4, OreProductItem.of(stageOutputItem(stage), recipe.get().product(), 2));
        if (!canRun) {
            if (progress[stage] != 0) {
                progress[stage] = 0;
                setChanged();
            }
            return;
        }
        operationTime[stage] = PowerRequirement.operationTime(DURATIONS[stage][0], DURATIONS[stage][1], omega);
        if (++progress[stage] < operationTime[stage]) {
            setChanged();
            return;
        }
        progress[stage] = 0;
        ExtractionRecipe r = recipe.get();
        int count = level.random.nextDouble() < r.doublingChance() ? 2 : 1;
        items.extractItem(stage, 1, false);
        if (stage == 0 && wears()) {
            drillTime--;
        }
        addTo(stage + 4, OreProductItem.of(stageOutputItem(stage), r.product(), count));
        if (usesWater) {
            water.drain(WATER_PER_OPERATION, FluidTank.FluidAction.EXECUTE);
        }
        if (stage == 3 && !r.bonus().isEmpty() && level.random.nextFloat() < r.bonusChance() && fits(SLOT_BONUS, r.bonus())) {
            addTo(SLOT_BONUS, r.bonus());
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.extractor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ExtractorMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.putIntArray("progress", progress);
        tag.putInt("drill", drillTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        water.readFromNBT(registries, tag.getCompound("water"));
        drillTime = tag.contains("drill") ? tag.getInt("drill") : DRILL_LIFE;
        int[] saved = tag.getIntArray("progress");
        System.arraycopy(saved, 0, progress, 0, Math.min(saved.length, STAGES));
    }
}
