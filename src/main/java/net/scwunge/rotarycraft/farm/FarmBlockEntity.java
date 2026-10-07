package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * The base of the farming and automation machines: a shaft-power consumer (from its back, or from any side) that remembers who placed it
 * so what it changes can be checked against claims, has an off switch in the farm config, and tells its clients its power now and then
 * (for the animation and screens) and whatever else the machine adds.
 */
public abstract class FarmBlockEntity extends ConsumerBlockEntity implements Owned, net.minecraft.world.MenuProvider {
    public static final int DATA_COUNT = 8;

    @Nullable
    protected WorldGuard.Owner owner;
    private int syncedTorque, syncedOmega;
    /** Client side: the angle of the machine's turning parts, and the game time it was last moved on. */
    public float phi;
    public long phiTime;

    protected FarmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The key of this machine's off switch in the farm config. */
    protected abstract String switchName();

    /** The machine's name, which the screen picks its words by. */
    public final String kind() {
        return switchName();
    }

    /** Whether power comes in from every side, summed (the original's "summative" machines), rather than from the back only. */
    protected boolean anySide() {
        return false;
    }

    public final boolean isSwitchedOn() {
        return FarmConfig.enabled(switchName());
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

    @Override
    public void serverTick() {
        if (anySide()) {
            long total = 0;
            int fastest = 0;
            for (Direction side : Direction.values()) {
                IShaftPowerOutput.Reading in = IShaftPowerOutput.readInput(level, worldPosition, side);
                total += (long) in.torque() * in.omega();
                fastest = Math.max(fastest, in.omega());
            }
            setPower(fastest == 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, total / fastest), fastest);
        } else {
            IShaftPowerOutput.Reading in = readInput();
            setPower(in.torque(), in.omega());
        }
        if (usesShaftPower() && (torque != syncedTorque || omega != syncedOmega) && level.getGameTime() % 10 == 0) {
            syncedTorque = torque;
            syncedOmega = omega;
            syncNow();
        }
        if (isShutdown() || !isSwitchedOn()) {
            return;
        }
        machineTick(hasEnoughPower());
    }

    /** Machines that run on water or items alone return false, and have no shaft to read. */
    protected boolean usesShaftPower() {
        return true;
    }

    @Override
    public boolean hasEnoughPower() {
        return !usesShaftPower() || super.hasEnoughPower();
    }

    /** The enchantments the machine can take from books, or null if it takes none. */
    @Nullable
    public net.scwunge.rotarycraft.blockentity.MachineEnchantments enchantments() {
        return null;
    }

    public final int enchantLevel(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        var e = enchantments();
        return e == null ? 0 : e.level(enchantment);
    }

    /** A player uses an item on the block: enchanted books give it their enchantments. Whether the item was taken. */
    public boolean interact(Player player, net.minecraft.world.item.ItemStack stack) {
        var e = enchantments();
        if (e != null && stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
            if (e.apply(stack)) {
                setChanged();
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return true;
        }
        return false;
    }

    /** Whether {@link #interact} takes this item (so the client lets the click through to the server). */
    public boolean wouldTake(net.minecraft.world.item.ItemStack stack) {
        return enchantments() != null && stack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK);
    }

    /** The machine's slots, or null if it has none. */
    @Nullable
    public net.neoforged.neoforge.items.ItemStackHandler items() {
        return null;
    }

    /** Rows of nine slots on its screen, or 0 if it has no screen. */
    public int menuRows() {
        return 0;
    }

    /** Up to six numbers the machine's screen shows: written after the torque and speed in the menu's data. */
    protected int[] status() {
        return new int[0];
    }

    public final net.minecraft.world.inventory.ContainerData data() {
        return new net.minecraft.world.inventory.ContainerData() {
            @Override
            public int get(int index) {
                if (index == 0) {
                    return torque;
                }
                if (index == 1) {
                    return omega;
                }
                int[] status = status();
                return index - 2 < status.length ? status[index - 2] : 0;
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

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory, Player player) {
        return menuRows() > 0 ? new net.scwunge.rotarycraft.menu.FarmMenu(id, inventory, this) : null;
    }

    /** Drops what the machine holds, when its block is broken. */
    public void dropContents() {
        var items = items();
        if (items != null) {
            for (int i = 0; i < items.getSlots(); i++) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(i));
            }
        }
    }

    protected final ServerLevel server() {
        return (ServerLevel) level;
    }

    protected final void syncNow() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** What the machine adds to the data its clients are sent. */
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    /** What the machine keeps in its saved data besides the power and the owner. */
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
        var items = items();
        if (items != null) {
            tag.put("items", items.serializeNBT(registries));
        }
        if (enchantments() != null) {
            tag.put("enchants", enchantments().save());
        }
        writeData(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
        var items = items();
        if (items != null) {
            items.deserializeNBT(registries, tag.getCompound("items"));
        }
        if (enchantments() != null) {
            enchantments().load(net.scwunge.rotarycraft.blockentity.MachineEnchantments.listTag(tag, "enchants"));
        }
        readData(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        writeClientData(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        torque = tag.getInt("torque");
        omega = tag.getInt("omega");
        readClientData(tag, registries);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
