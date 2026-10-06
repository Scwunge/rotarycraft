package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.item.CoilItem;
import net.scwunge.rotarycraft.menu.OneSlotMenu;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.WeaponRegistry;

/**
 * The Winder, as the original: in winding mode shaft power winds the coil in its slot up, one unit of charge at a time (quicker the
 * faster the shaft, slower the more charged the coil), up to the torque divided by the coil's stiffness; a Spring may snap as it
 * nears full. In unwinding mode the coil turns back into shaft power, out of the back, at 8 N*m and 1024 rad/s times its power scale,
 * using up a unit of charge every 20 ticks times its stiffness.
 */
public class WinderBlockEntity extends PowerBlockEntity implements MenuProvider, OneSlotMenu.Host {
    public static final int FLAG_UNWINDING = 2;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide() && (getStackInSlot(0).getItem() instanceof CoilItem) != hasCoil) {
                hasCoil = getStackInSlot(0).getItem() instanceof CoilItem;
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof CoilItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private boolean winding = true;
    private final ContainerData data = OneSlotMenu.data(() -> omega, () -> torque, () -> winding ? 0 : FLAG_UNWINDING);
    private int ticks;
    private int syncedTorque, syncedOmega;
    private boolean hasCoil;

    public WinderBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.WINDER_BE.get(), pos, state);
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    public boolean winding() {
        return winding;
    }

    public void setWinding(boolean winding) {
        if (this.winding != winding) {
            this.winding = winding;
            ticks = 0;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** It passes power on only when unwinding, and then out of the side that takes it in when winding. */
    @Override
    public int getTorqueOut(Direction side) {
        return !isShutdown() && !winding && side == inputSide() ? torque : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return !isShutdown() && !winding && side == inputSide() ? omega : 0;
    }

    private CoilItem coil() {
        return items.getStackInSlot(0).getItem() instanceof CoilItem c ? c : null;
    }

    public int maxWind() {
        CoilItem coil = coil();
        return coil == null ? 0 : Math.min(CoilItem.MAX_CHARGE, torque / coil.stiffness());
    }

    /** Ticks per unit of winding: longer the more charged the coil is, shorter the faster the shaft turns. */
    public int operationTime() {
        ItemStack stack = items.getStackInSlot(0);
        if (stack.isEmpty() || omega <= 0) {
            return Integer.MAX_VALUE;
        }
        int base = (int) (Math.log(Math.max(1, CoilItem.charge(stack))) / Math.log(2));
        int speed = Math.max(1, (int) (Math.log(omega + 1) / Math.log(2)));
        return base / speed;
    }

    @Override
    public void serverTick() {
        step();
        if ((torque != syncedTorque || omega != syncedOmega) && level.getGameTime() % 20 == 0) {
            syncedTorque = torque;
            syncedOmega = omega;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private void step() {
        CoilItem coil = coil();
        ItemStack stack = items.getStackInSlot(0);
        if (winding) {
            IShaftPowerOutput.Reading in = readInput();
            setPower(in.torque(), in.omega());
            if (coil == null) {
                return;
            }
            if (++ticks < operationTime()) {
                return;
            }
            ticks = 0;
            int charge = CoilItem.charge(stack);
            if (charge >= maxWind()) {
                return;
            }
            ItemStack wound = stack.copy();
            CoilItem.setCharge(wound, charge + 1);
            items.setStackInSlot(0, wound);
            if (coil.breakable() && level.random.nextDouble() * 100 < (charge + 1) / (double) CoilItem.MAX_CHARGE) {
                items.setStackInSlot(0, ItemStack.EMPTY);
                level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1, 1);
            }
        } else {
            if (coil == null || CoilItem.charge(stack) <= 0) {
                setPower(0, 0);
                return;
            }
            setPower(8 * coil.powerScale(), 1024 * coil.powerScale());
            if (++ticks < 20 * coil.stiffness()) {
                return;
            }
            ticks = 0;
            ItemStack unwound = stack.copy();
            CoilItem.setCharge(unwound, CoilItem.charge(stack) - 1);
            items.setStackInSlot(0, unwound);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OneSlotMenu(WeaponRegistry.WINDER_MENU.get(), id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putBoolean("winding", winding);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        winding = !tag.contains("winding") || tag.getBoolean("winding");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("winding", winding);
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        tag.putBoolean("coil", coil() != null);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        winding = tag.getBoolean("winding");
        torque = tag.getInt("torque");
        omega = tag.getInt("omega");
        hasCoil = tag.getBoolean("coil");
    }

    /** Whether it holds a coil (known to clients, which draw the coil). */
    public boolean hasCoil() {
        return hasCoil;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
