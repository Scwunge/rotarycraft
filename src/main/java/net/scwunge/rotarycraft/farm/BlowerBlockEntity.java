package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.menu.FarmMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * The Item Pump (the original's Blower): it moves items from the inventory behind it to the one in front, up to 1 item a tick for each 1024 W
 * (from any side; it needs at least 1024 W and 256 rad/s), through any pumps in a line, and only the items its 18 pattern slots allow: a
 * whitelist or a blacklist, matching on the item alone, on its data too, or on any shared common tag. With no inventory in front, and
 * air there, it sprays the items out (the config can stop that).
 */
public class BlowerBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 256, 1024);
    public static final int PATTERNS = 18;
    public static final int WHITELIST = 0, METADATA = 1, NBT = 2, EXACT = 3;

    private final ItemStackHandler patterns = new ItemStackHandler(PATTERNS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private boolean whitelist, useMetadata, useNbt, exact;

    public BlowerBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.BLOWER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "blower";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public ItemStackHandler items() {
        return patterns;
    }

    @Override
    protected boolean dropsItems() {
        return false;
    }

    @Override
    public FarmUi ui() {
        int[] slots = new int[PATTERNS * 2];
        for (int i = 0; i < PATTERNS; i++) {
            slots[2 * i] = 8 + (i % 9) * 18;
            slots[2 * i + 1] = 21 + (i / 9) * 18;
        }
        return FarmUi.panel("blower", 192, 110, slots);
    }

    @Override
    public Slot slot(ItemStackHandler handler, int index, int x, int y) {
        return new FarmMenu.GhostSlot(handler, index, x, y);
    }

    @Override
    public boolean menuButton(Player player, int id) {
        switch (id) {
            case WHITELIST -> whitelist = !whitelist;
            case METADATA -> useMetadata = !useMetadata;
            case NBT -> useNbt = !useNbt;
            case EXACT -> exact = !exact;
            default -> {
                return false;
            }
        }
        setChanged();
        return true;
    }

    public boolean isWhitelist() {
        return whitelist;
    }

    public boolean usesMetadata() {
        return useMetadata;
    }

    public boolean usesNbt() {
        return useNbt;
    }

    /** Whether items must match exactly, rather than by any tag they share. */
    public boolean isExact() {
        return exact;
    }

    @Override
    protected int[] status() {
        return new int[] {whitelist ? 1 : 0, useMetadata ? 1 : 0, useNbt ? 1 : 0, exact ? 1 : 0};
    }

    public Direction front() {
        return facing();
    }

    public int itemsPerTick() {
        return (int) (getPower() / 1024);
    }

    /** Whether the patterns let this item through. */
    public boolean isTransferrable(ItemStack stack) {
        boolean any = false;
        boolean matched = false;
        for (int i = 0; i < PATTERNS && !matched; i++) {
            ItemStack pattern = patterns.getStackInSlot(i);
            if (pattern.isEmpty()) {
                continue;
            }
            any = true;
            matched = matches(pattern, stack);
        }
        if (!any) {
            return true;
        }
        return whitelist == matched;
    }

    private boolean matches(ItemStack pattern, ItemStack stack) {
        if (useNbt) {
            return ItemStack.isSameItemSameComponents(pattern, stack);
        }
        if (useMetadata) {
            return ItemStack.isSameItem(pattern, stack) && pattern.getDamageValue() == stack.getDamageValue();
        }
        if (ItemStack.isSameItem(pattern, stack)) {
            return true;
        }
        if (exact) {
            return false;
        }
        return pattern.getTags().anyMatch(t -> t.location().getNamespace().equals("c") && stack.is(t));
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered || getOmega() < REQUIREMENT.minOmega()) {
            return;
        }
        ServerLevel server = server();
        int max = itemsPerTick();
        if (max <= 0) {
            return;
        }
        Direction dir = front();
        IItemHandler source = server.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir.getOpposite()), dir);
        if (source == null) {
            return;
        }
        BlockPos targetPos = worldPosition.relative(dir);
        Set<BlockPos> seen = new HashSet<>();
        seen.add(worldPosition);
        Direction out = dir;
        while (server.getBlockEntity(targetPos) instanceof BlowerBlockEntity next) {
            if (!seen.add(targetPos)) {
                return;
            }
            out = next.front();
            targetPos = targetPos.relative(out);
        }
        IItemHandler target = server.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, out.getOpposite());
        if (target != null) {
            if (target == source) {
                return;
            }
            transfer(source, target, max);
        } else if (RotaryConfig.get(FarmConfig.BLOWER_SPILLS) && server.getBlockState(targetPos).isAir() && server.getBlockEntity(targetPos) == null) {
            spray(server, source, targetPos, out, max);
        }
    }

    private void transfer(IItemHandler source, IItemHandler target, int max) {
        for (int slot = 0; slot < source.getSlots() && max > 0; slot++) {
            ItemStack here = source.getStackInSlot(slot);
            if (here.isEmpty() || !isTransferrable(here)) {
                continue;
            }
            ItemStack want = source.extractItem(slot, Math.min(max, here.getCount()), true);
            if (want.isEmpty()) {
                continue;
            }
            ItemStack left = ItemHandlerHelper.insertItem(target, want, true);
            int moved = want.getCount() - left.getCount();
            if (moved > 0) {
                ItemStack taken = source.extractItem(slot, moved, false);
                ItemStack rest = ItemHandlerHelper.insertItem(target, taken, false);
                if (!rest.isEmpty()) {
                    ItemHandlerHelper.insertItem(source, rest, false);
                }
                max -= moved - rest.getCount();
            }
        }
    }

    private void spray(ServerLevel server, IItemHandler source, BlockPos at, Direction dir, int max) {
        for (int slot = 0; slot < source.getSlots() && max > 0; slot++) {
            ItemStack here = source.getStackInSlot(slot);
            if (here.isEmpty() || !isTransferrable(here)) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, Math.min(max, here.getCount()), false);
            max -= taken.getCount();
            for (int i = 0; i < taken.getCount(); i++) {
                ItemEntity e = new ItemEntity(server, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, taken.copyWithCount(1));
                double v = (2 + server.random.nextDouble() * 4) * 0.1;
                e.setDeltaMovement(dir.getStepX() * v + (server.random.nextDouble() - 0.5) * 0.05, dir.getStepY() * v + 0.05, dir.getStepZ() * v + (server.random.nextDouble() - 0.5) * 0.05);
                server.addFreshEntity(e);
            }
        }
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("white", whitelist);
        tag.putBoolean("meta", useMetadata);
        tag.putBoolean("nbt", useNbt);
        tag.putBoolean("exact", exact);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        whitelist = tag.getBoolean("white");
        useMetadata = tag.getBoolean("meta");
        useNbt = tag.getBoolean("nbt");
        exact = tag.getBoolean("exact");
    }
}
