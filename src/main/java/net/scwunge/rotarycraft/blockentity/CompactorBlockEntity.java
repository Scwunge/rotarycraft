package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.menu.CompactorMenu;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.CompactingRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Compactor: needs 4096 N*m and 262 kW. Four matching items are squeezed into the result once the pressure and
 * temperature are high enough, as in the original: pressure rises by 128 x log2(torque) kPa a second while it turns and
 * leaks back towards the air's pressure (so about 25600 x log2(torque) kPa can be held: real coal compaction needs
 * enormous torque); the temperature comes from outside (lava +4 C/s, fire +2 C/s, a Friction Heater) and cools towards
 * ambient. Coal stages need 550 MPa and 800 C. Past 600 MPa it blows up; past 1000 C it melts into scrap.
 */
public class CompactorBlockEntity extends ConsumerBlockEntity implements MenuProvider, Heatable {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(4096, 1, 262144);
    public static final int MAX_TEMPERATURE = 1000;
    public static final int MAX_PRESSURE = 600_000;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOTS = 5;
    private static final int AMBIENT_PRESSURE = 101;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != SLOT_OUTPUT;
        }
    };
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
            return slot == SLOT_OUTPUT ? stack : items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_OUTPUT ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };

    private int pressure = AMBIENT_PRESSURE;
    private int temperature = Integer.MIN_VALUE;
    private int progress;
    private int operationTime = 1;
    private int envTicks;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> operationTime;
                case 2 -> temperature();
                case 3 -> pressure & 0xFFFF;
                case 4 -> pressure >>> 16;
                case 5 -> torque & 0xFFFF;
                case 6 -> torque >>> 16;
                case 7 -> omega & 0xFFFF;
                case 8 -> omega >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return CompactorMenu.DATA_COUNT;
        }
    };

    public CompactorBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.COMPACTOR.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    public int pressure() {
        return pressure;
    }

    public void setPressure(int p) {
        pressure = p;
        setChanged();
    }

    public int temperature() {
        return temperature == Integer.MIN_VALUE ? Ambient.temperature(level, worldPosition) : temperature;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    @Override
    public int getTemperature() {
        return temperature();
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature = temperature() + amount;
        setChanged();
    }

    /** The recipe the four inputs make, if they all match. */
    private Optional<CompactingRecipe> recipe() {
        ItemStack first = items.getStackInSlot(0);
        if (first.isEmpty()) {
            return Optional.empty();
        }
        for (int i = 1; i < 4; i++) {
            if (!ItemStack.isSameItemSameComponents(first, items.getStackInSlot(i))) {
                return Optional.empty();
            }
        }
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.COMPACTING.get(), new SingleRecipeInput(first), level).map(RecipeHolder::value);
    }

    private boolean roomFor(ItemStack result) {
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    @Override
    protected void machineTick(boolean powered) {
        if (++envTicks >= 20) {
            envTicks = 0;
            updatePressure();
            if (level.getBlockEntity(worldPosition) != this) {
                return;
            }
            updateTemperature();
            if (level.getBlockEntity(worldPosition) != this) {
                return;
            }
        }
        Optional<CompactingRecipe> r = recipe();
        if (!powered || r.isEmpty() || pressure < r.get().pressure() || temperature() < r.get().temperature() || !roomFor(r.get().result())) {
            progress = 0;
            return;
        }
        int stage = r.get().stage();
        operationTime = Math.max(4, PowerRequirement.operationTime(300 * stage, 15 * stage, omega));
        if (++progress < operationTime) {
            return;
        }
        progress = 0;
        ItemStack result = r.get().result();
        ItemStack out = items.getStackInSlot(SLOT_OUTPUT);
        items.setStackInSlot(SLOT_OUTPUT, out.isEmpty() ? result.copy() : out.copyWithCount(out.getCount() + result.getCount()));
        for (int i = 0; i < 4; i++) {
            items.extractItem(i, 1, false);
        }
        setChanged();
    }

    private void updatePressure() {
        boolean nether = level.dimensionType().ultraWarm();
        if (pressure > AMBIENT_PRESSURE) {
            pressure -= Math.max((pressure - AMBIENT_PRESSURE) / (nether ? 600 : 200), 1);
        } else if (pressure < AMBIENT_PRESSURE) {
            pressure += Math.max((AMBIENT_PRESSURE - pressure) / 40, 1);
        }
        if (omega > 0 && torque > 0) {
            pressure += (int) (128 * (Math.log(torque) / Math.log(2)));
        }
        if (pressure > MAX_PRESSURE) {
            pressure = MAX_PRESSURE;
            level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 4, interaction());
        }
        setChanged();
    }

    private void updateTemperature() {
        int tAmb = Ambient.temperature(level, worldPosition);
        int t = temperature();
        if (t > tAmb) {
            t -= Math.max((t - tAmb) / 200, 1);
        } else if (t < tAmb) {
            t += Math.max((tAmb - t) / 40, 1);
        }
        for (Direction d : Direction.values()) {
            BlockPos p = worldPosition.relative(d);
            BlockState s = level.getBlockState(p);
            if (s.getFluidState().is(FluidTags.LAVA)) {
                t += 4;
                break;
            }
        }
        for (Direction d : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(d)).getBlock() instanceof BaseFireBlock) {
                t += 2;
                break;
            }
        }
        if (tAmb >= 100) {
            t++; // fire is hotter in the Nether
        }
        for (Direction d : Direction.values()) {
            BlockPos p = worldPosition.relative(d);
            BlockState s = level.getBlockState(p);
            if (t > 600 && s.getFluidState().is(FluidTags.WATER)) {
                t--;
                if (level.random.nextInt(4000) == 0) {
                    level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                }
            } else if (t > 0 && (s.is(Blocks.ICE) || s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK))) {
                t -= 2;
                if (level.random.nextInt(s.is(Blocks.ICE) ? 200 : 100) == 0) {
                    level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
                }
            }
        }
        temperature = t;
        setChanged();
        if (t > MAX_TEMPERATURE) {
            Level lvl = level;
            BlockPos pos = worldPosition;
            lvl.removeBlock(pos, false);
            lvl.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, interaction());
            int scrap = lvl.random.nextInt(18);
            for (int i = 0; i < scrap; i++) {
                Containers.dropItemStack(lvl, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(RotaryItems.SCRAP.get()));
            }
        }
    }

    private static Level.ExplosionInteraction interaction() {
        return RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.compactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CompactorMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("pressure", pressure);
        tag.putInt("temperature", temperature());
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        pressure = tag.contains("pressure") ? tag.getInt("pressure") : AMBIENT_PRESSURE;
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        progress = tag.getInt("progress");
    }
}
