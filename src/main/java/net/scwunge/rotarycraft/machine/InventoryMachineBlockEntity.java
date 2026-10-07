package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A shaft-driven machine with a screen (see {@link GuiLayout}): an item inventory, any number of fluid tanks and the numbers its
 * progress bars show, all saved and kept in step with the menu. Subclasses say what they accept, what automation may take, and do
 * the work in {@link #machineTick}.
 */
public abstract class InventoryMachineBlockEntity extends ConsumerBlockEntity implements MenuProvider, MachineHost, Owned {
    protected final ItemStackHandler items;
    private final List<FluidTank> tanks = new ArrayList<>();
    private final IItemHandler automation;
    private final String menuKey;
    /** Who placed it: what it breaks or places is checked against claims as this player. */
    @Nullable
    protected WorldGuard.Owner owner;

    protected InventoryMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, String menuKey) {
        super(type, pos, state);
        this.menuKey = menuKey;
        this.items = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return acceptsItem(slot, stack);
            }
        };
        this.automation = new IItemHandler() {
            @Override
            public int getSlots() {
                return items.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return acceptsFromAutomation(slot, stack) ? items.insertItem(slot, stack, simulate) : stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return mayExtract(slot) ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return acceptsFromAutomation(slot, stack);
            }
        };
    }

    @Override
    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    @Nullable
    public WorldGuard.Owner owner() {
        return owner;
    }

    /** Whether it takes shaft power from any side, adding it up (the original's "summative sided power"), not just from its back. */
    protected boolean omniSided() {
        return false;
    }

    @Override
    protected IShaftPowerOutput.Reading readInput() {
        if (!omniSided() || level == null) {
            return super.readInput();
        }
        long total = 0;
        int fastest = 0;
        for (Direction side : Direction.values()) {
            IShaftPowerOutput.Reading in = IShaftPowerOutput.readInput(level, worldPosition, side);
            total += (long) in.torque() * in.omega();
            fastest = Math.max(fastest, in.omega());
        }
        return fastest == 0 ? IShaftPowerOutput.Reading.NONE : new IShaftPowerOutput.Reading((int) Math.min(Integer.MAX_VALUE, total / fastest), fastest);
    }

    // ---- what the machine accepts ----

    /** Whether a player may put {@code stack} in {@code slot}. */
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return true;
    }

    /** Whether pipes and hoppers may. */
    protected boolean acceptsFromAutomation(int slot, ItemStack stack) {
        return acceptsItem(slot, stack);
    }

    /** Whether pipes and hoppers may take from {@code slot}. */
    protected boolean mayExtract(int slot) {
        return true;
    }

    protected FluidTank addTank(int capacity, Predicate<FluidStack> valid) {
        FluidTank tank = new FluidTank(capacity, valid) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        tanks.add(tank);
        return tank;
    }

    /** The fluid handler pipes see on {@code side} (null: the block itself). Null for none. */
    @Nullable
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return null;
    }

    /** A tank that only takes fluid in. */
    protected static IFluidHandler fillOnly(FluidTank tank) {
        return FluidAccess.fillOnly(tank);
    }

    /** A tank that only gives fluid out. */
    protected static IFluidHandler drainOnly(FluidTank tank) {
        return FluidAccess.drainOnly(tank);
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    @Override
    public List<FluidTank> tanks() {
        return tanks;
    }

    @Override
    public int hostTorque() {
        return torque;
    }

    @Override
    public int hostOmega() {
        return omega;
    }

    /** How many numbers the machine reports for its bars. */
    protected int extraCount() {
        return 0;
    }

    // ---- the screen ----

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < 4) {
                return switch (index) {
                    case 0 -> omega & 0xFFFF;
                    case 1 -> omega >>> 16;
                    case 2 -> torque & 0xFFFF;
                    default -> torque >>> 16;
                };
            }
            int i = index - 4;
            if (i < 3 * tanks.size()) {
                FluidTank tank = tanks.get(i / 3);
                return switch (i % 3) {
                    case 0 -> BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
                    case 1 -> tank.getFluidAmount() & 0xFFFF;
                    default -> tank.getFluidAmount() >>> 16;
                };
            }
            i -= 3 * tanks.size();
            int value = extra(i / 2);
            return i % 2 == 0 ? value & 0xFFFF : value >>> 16;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return layout().dataCount();
        }
    };

    public ContainerData menuData() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft." + menuKey);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return LayoutMenus.create(menuKey, id, inventory, this);
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        ListTag list = new ListTag();
        for (FluidTank tank : tanks) {
            list.add(tank.writeToNBT(registries, new CompoundTag()));
        }
        tag.put("tanks", list);
        if (owner != null) {
            tag.putUUID("owner_id", owner.id());
            tag.putString("owner_name", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        ListTag list = tag.getList("tanks", Tag.TAG_COMPOUND);
        for (int i = 0; i < tanks.size() && i < list.size(); i++) {
            tanks.get(i).readFromNBT(registries, list.getCompound(i));
        }
        owner = tag.hasUUID("owner_id") ? new WorldGuard.Owner(tag.getUUID("owner_id"), tag.getString("owner_name")) : null;
    }

    // ---- keeping clients in step (renderers need a few numbers; the default would read the packet as saved data and miss them) ----

    private boolean clientDirty;
    private int syncedOmega;

    /** Something the renderer shows has changed: the clients get an update within a few ticks. */
    protected void markClientDirty() {
        clientDirty = true;
    }

    /** What the client needs: its torque and speed, plus whatever the machine adds. */
    protected void writeClient(CompoundTag tag) {
        tag.putInt("omega", omega);
        tag.putInt("torque", torque);
    }

    protected void readClient(CompoundTag tag) {
        omega = tag.getInt("omega");
        torque = tag.getInt("torque");
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (omega != syncedOmega) {
            syncedOmega = omega;
            clientDirty = true;
        }
        if (clientDirty && level != null && level.getGameTime() % 5 == 0) {
            clientDirty = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeClient(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClient(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
