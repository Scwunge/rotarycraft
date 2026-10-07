package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Item Filter (TileEntityItemFilter): an inventory of one slot that takes only the items that match the item in its template slot (see {@link MatchData}), and
 * none that are in its sixteen blacklist slots; a redstone signal turns that round, so that it takes what does not match. Pipes and hoppers feed it and empty it,
 * while it has 1 kW of power. (The original could also pull matching items out of an Applied Energistics network; that is not ported.)
 */
public class ItemFilterBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "item_filter";
    public static final int TEMPLATE = 0;
    public static final int BUFFER = 1;
    public static final int BLACKLIST = 2;
    public static final int BLACKLIST_SLOTS = 16;
    public static final int MIN_POWER = 1024;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).size(256, 217).inventoryAt(8, 135).customScreen().slot(8, 8).slot(8, 104).grid(176, 135, 4, 4).build();

    @Nullable
    private MatchData data;
    private ItemStack seen = ItemStack.EMPTY;

    public ItemFilterBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.ITEM_FILTER.type().get(), pos, state, 2 + BLACKLIST_SLOTS, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    protected boolean omniSided() {
        return true;
    }

    @Nullable
    public MatchData matchData() {
        return data;
    }

    public boolean isPowered() {
        return getPower() >= MIN_POWER && MachineConfig.enabled("itemFilter");
    }

    private boolean blacklisted(ItemStack stack) {
        for (int i = BLACKLIST; i < BLACKLIST + BLACKLIST_SLOTS; i++) {
            ItemStack entry = items.getStackInSlot(i);
            if (!entry.isEmpty() && ItemStack.isSameItemSameComponents(entry, stack)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the stack passes the filter: it matches the template (or, with a redstone signal on, does not), and is not blacklisted. */
    public boolean matchItem(ItemStack stack) {
        return level != null && data != null && !blacklisted(stack) && data.match(stack, level.registryAccess()) != level.hasNeighborSignal(worldPosition);
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return slot != BUFFER || matchItem(stack);
    }

    @Override
    protected boolean acceptsFromAutomation(int slot, ItemStack stack) {
        return slot == BUFFER && isPowered() && matchItem(stack);
    }

    @Override
    protected boolean mayExtract(int slot) {
        return slot == BUFFER;
    }

    /** The template slot changed: the data is the new item's, keeping the settings chosen for the old one. */
    private void reloadData() {
        ItemStack template = items.getStackInSlot(TEMPLATE);
        if (template.getItem() instanceof net.scwunge.rotarycraft.handheld.MatchFilterItem) {
            // a match filter stands for the item it holds
            template = net.scwunge.rotarycraft.handheld.MatchFilterItem.template(template);
        }
        if (level == null) {
            return;
        }
        MatchData now = template.isEmpty() ? null : MatchData.of(template, level.registryAccess()).loadFrom(data);
        if (now != null && data != null && now.toTag().equals(data.toTag())) {
            return;
        }
        data = now;
        markClientDirty();
    }

    @Override
    protected void machineTick(boolean powered) {
        if (level != null && !level.isClientSide()) {
            ItemStack template = items.getStackInSlot(TEMPLATE);
            if (!ItemStack.isSameItemSameComponents(seen, template)) {
                seen = template.copy();
                reloadData();
            }
        }
    }

    /** A button of the screen: a row of a page to step to its next setting ({@code page * 10000 + row}), or a whole page to set ({@code 200000 + page * 10 + setting}). */
    @Override
    public boolean menuButton(Player player, int id) {
        if (data == null) {
            return false;
        }
        if (id >= 200000) {
            int page = (id - 200000) / 10;
            int type = (id - 200000) % 10;
            if (page >= MatchData.Page.values().length || type >= MatchData.MatchType.values().length) {
                return false;
            }
            data.setAll(MatchData.Page.values()[page], MatchData.MatchType.values()[type]);
        } else {
            int page = id / 10000;
            if (page >= MatchData.Page.values().length) {
                return false;
            }
            data.increment(MatchData.Page.values()[page], id % 10000);
        }
        setChanged();
        markClientDirty();
        return true;
    }

    // ---- saving, and what the screen shows ----

    @Override
    protected void writeClient(CompoundTag tag) {
        super.writeClient(tag);
        tag.putBoolean("hasData", data != null);
        if (data != null) {
            tag.put("data", data.toTag());
        }
    }

    @Override
    protected void readClient(CompoundTag tag) {
        super.readClient(tag);
        data = tag.getBoolean("hasData") ? MatchData.fromTag(tag.getCompound("data")) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (data != null) {
            tag.put("data", data.toTag());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        data = tag.contains("data") ? MatchData.fromTag(tag.getCompound("data")) : null;
    }
}
