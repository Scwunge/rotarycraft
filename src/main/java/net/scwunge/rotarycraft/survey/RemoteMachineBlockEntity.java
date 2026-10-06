package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.item.CoilItem;
import org.jetbrains.annotations.Nullable;

/**
 * A camera that a Screen can call up, as the original's remote control machines (CCTV and Spy Cam): a wound coil in the first
 * slot runs it, and three dyes in the other slots say which Screen it answers to (a Screen with the same three dyes, in the same
 * order, within 64 blocks).
 */
public abstract class RemoteMachineBlockEntity extends SurveyBlockEntity implements MenuProvider {
    public static final int BASE_DISCHARGE_TIME = 120;
    public static final int SIZE = 4;

    private final ItemStackHandler items = new ItemStackHandler(SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                updateColors();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? stack.getItem() instanceof CoilItem : stack.getItem() instanceof DyeItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : 64;
        }
    };
    private final int[] colors = {-1, -1, -1};
    private int ticks;
    private boolean on;

    protected RemoteMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    public boolean isOn() {
        return on;
    }

    /** The three dye ids that name this camera, or -1 where a slot is empty. */
    public int[] colors() {
        return colors;
    }

    public boolean isNamed() {
        return colors[0] >= 0 && colors[1] >= 0 && colors[2] >= 0;
    }

    public boolean hasCoil() {
        ItemStack coil = items.getStackInSlot(0);
        return coil.getItem() instanceof CoilItem && CoilItem.charge(coil) > 0;
    }

    private void updateColors() {
        for (int i = 0; i < 3; i++) {
            ItemStack dye = items.getStackInSlot(i + 1);
            colors[i] = dye.getItem() instanceof DyeItem item ? item.getDyeColor().getId() : -1;
        }
        syncNow();
    }

    @Override
    public void serverTick() {
        if (level.getGameTime() % 100 == 0) {
            syncNow();
        }
        if (!hasCoil()) {
            if (on) {
                on = false;
                syncNow();
            }
            return;
        }
        if (!on) {
            on = true;
            syncNow();
        }
        ItemStack coil = items.getStackInSlot(0);
        if (++ticks > BASE_DISCHARGE_TIME * ((CoilItem) coil.getItem()).stiffness()) {
            ItemStack unwound = coil.copy();
            CoilItem.setCharge(unwound, CoilItem.charge(coil) - 1);
            items.setStackInSlot(0, unwound);
            ticks = 0;
        }
        remoteTick();
    }

    /** One tick of what the camera does while it has a coil. */
    protected void remoteTick() {
    }

    /** What a Screen asks for when it calls this camera up for a player. Server side. */
    public abstract void activate(Player player);

    protected final void syncNow() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RemoteMenu(id, inventory, this);
    }

    /** What the camera adds to the data its renderer is sent. */
    protected void writeClientData(CompoundTag tag) {
    }

    protected void readClientData(CompoundTag tag) {
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("ticks", ticks);
        writeClientData(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        ticks = tag.getInt("ticks");
        readClientData(tag);
        for (int i = 0; i < 3; i++) {
            ItemStack dye = items.getStackInSlot(i + 1);
            colors[i] = dye.getItem() instanceof DyeItem item ? item.getDyeColor().getId() : -1;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("on", on);
        tag.putIntArray("colors", colors);
        writeClientData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        on = tag.getBoolean("on");
        int[] c = tag.getIntArray("colors");
        for (int i = 0; i < 3 && i < c.length; i++) {
            colors[i] = c[i];
        }
        readClientData(tag);
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
