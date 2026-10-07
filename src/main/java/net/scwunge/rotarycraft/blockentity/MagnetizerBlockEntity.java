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
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/**
 * Magnetizer: needs 2048 rad/s and 16 kW plus an alternating redstone signal (a fast clock). Every cycle
 * (400 - 20 x log2(speed) ticks) it has a chance to add 1 uT to the shaft core inside, up to speed / 2 uT.
 */
public class MagnetizerBlockEntity extends ConsumerBlockEntity implements MenuProvider, OneSlotMenu.Host, net.scwunge.rotarycraft.upgrade.Upgradable {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 2048, 16384);
    private static final int MIN_DURATION = 2;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof net.scwunge.rotarycraft.item.Magnetizable m && m.chargeChance() > 0;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final AlternatingRedstone redstone = new AlternatingRedstone();
    private final ContainerData data = OneSlotMenu.data(() -> omega, () -> torque, () -> redstone.isAlternating() ? OneSlotMenu.FLAG_AC : 0);
    private int progress;
    private boolean lodestone;

    public MagnetizerBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.MAGNETIZER.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    /** A lodestone upgrade makes it work as if it turned twice as fast. */
    private int effectiveSpeed() {
        return lodestone ? omega * 2 : omega;
    }

    public int operationTime() {
        return Math.max(MIN_DURATION, PowerRequirement.operationTime(400, 20, effectiveSpeed()));
    }

    @Override
    public boolean canUpgradeWith(ItemStack stack) {
        if (!(stack.getItem() instanceof net.scwunge.rotarycraft.item.EngineUpgradeItem up)) {
            return false;
        }
        return up.kind() == net.scwunge.rotarycraft.item.EngineUpgradeItem.Kind.REDSTONE ? !redstone.hasIntegrated() : up.kind() == net.scwunge.rotarycraft.item.EngineUpgradeItem.Kind.LODESTONE && !lodestone;
    }

    @Override
    public void upgradeWith(ItemStack stack) {
        if (((net.scwunge.rotarycraft.item.EngineUpgradeItem) stack.getItem()).kind() == net.scwunge.rotarycraft.item.EngineUpgradeItem.Kind.REDSTONE) {
            redstone.addIntegrated();
        } else {
            lodestone = true;
        }
        setChanged();
    }

    public boolean hasLodestoneUpgrade() {
        return lodestone;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered) {
            progress = 0;
            return;
        }
        if (!redstone.update(level, worldPosition)) {
            return;
        }
        if (++progress < operationTime()) {
            return;
        }
        progress = 0;
        ItemStack core = items.getStackInSlot(0);
        if (core.getItem() instanceof net.scwunge.rotarycraft.item.Magnetizable item && level.random.nextInt(item.chargeChance()) == 0) {
            int m = ShaftCoreItem.magnetization(core);
            if (m < effectiveSpeed() / ShaftCoreItem.SPEED_PER_MICROTESLA) {
                ItemStack charged = core.copy();
                ShaftCoreItem.setMagnetization(charged, m + 1);
                items.setStackInSlot(0, charged);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.magnetizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new OneSlotMenu(RotaryMenus.MAGNETIZER.get(), id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putBoolean("lodestone", lodestone);
        tag.putBoolean("integratedClock", redstone.hasIntegrated());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        progress = tag.getInt("progress");
        lodestone = tag.getBoolean("lodestone");
        if (tag.getBoolean("integratedClock")) {
            redstone.addIntegrated();
        }
    }

    private boolean hasCoreClient;

    public boolean hasCoreClient() {
        return hasCoreClient;
    }

    @Override
    protected int statusKey() {
        return items.getStackInSlot(0).isEmpty() ? 0 : 1;
    }

    @Override
    protected void writeStatus(CompoundTag tag) {
        tag.putBoolean("core", !items.getStackInSlot(0).isEmpty());
    }

    @Override
    protected void readStatus(CompoundTag tag) {
        hasCoreClient = tag.getBoolean("core");
    }
}
