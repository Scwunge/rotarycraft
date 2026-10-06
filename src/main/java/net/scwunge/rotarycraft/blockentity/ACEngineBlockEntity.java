package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.item.ShaftCoreItem;
import net.scwunge.rotarycraft.menu.OneSlotMenu;
import net.scwunge.rotarycraft.power.AlternatingRedstone;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/**
 * AC Electric Engine: 512 N*m at 256 rad/s (131 kW). Runs on an alternating redstone signal with a magnetized shaft core
 * inside, which loses 1 uT every 600 ticks of running (a tungsten core only half the time).
 */
public class ACEngineBlockEntity extends EngineBlockEntity implements MenuProvider, OneSlotMenu.Host {
    public static final int TORQUE = 512;
    public static final int SPEED = 256;
    public static final int FUEL_UNIT_TICKS = 600;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof ShaftCoreItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final AlternatingRedstone redstone = new AlternatingRedstone();
    private final ContainerData data = OneSlotMenu.data(() -> omega, () -> torque, () -> redstone.isAlternating() ? OneSlotMenu.FLAG_AC : 0);
    private int fuelTicks;

    public ACEngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.AC_ENGINE.get(), pos, state);
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    @Override
    protected int ratedTorque() {
        return TORQUE;
    }

    @Override
    protected int targetSpeed() {
        return SPEED;
    }

    @Override
    protected boolean canRun() {
        boolean ac = redstone.update(level, worldPosition);
        return ac && ShaftCoreItem.magnetization(items.getStackInSlot(0)) > 0;
    }

    @Override
    protected void afterTick(boolean running) {
        if (!running || ++fuelTicks < FUEL_UNIT_TICKS) {
            return;
        }
        fuelTicks = 0;
        ItemStack core = items.getStackInSlot(0);
        if (core.getItem() instanceof ShaftCoreItem item && (!item.durable() || level.random.nextBoolean())) {
            ItemStack used = core.copy();
            ShaftCoreItem.setMagnetization(used, ShaftCoreItem.magnetization(core) - 1);
            items.setStackInSlot(0, used);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.ac_engine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OneSlotMenu(RotaryMenus.AC_ENGINE.get(), id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("fuelTicks", fuelTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        fuelTicks = tag.getInt("fuelTicks");
    }
}
