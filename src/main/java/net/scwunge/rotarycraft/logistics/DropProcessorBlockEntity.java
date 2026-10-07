package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.blockentity.MachineEnchantments;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Drop Processor (TileEntityDropProcessor): turns the blocks put in it into what they drop when they are broken, as if mined with a Fortune tool if it has been
 * given Fortune from an enchanted book (Efficiency from a book makes it quicker). A block's drops that do not fit in the output slot wait their turn. Needs
 * 32 N*m and 1 kW; a run takes 300 ticks less 20 for each doubling of the speed. (The original's handlers for Thaumcraft, IC2, Mystcraft and similar mods'
 * items are not ported.)
 */
public class DropProcessorBlockEntity extends InventoryMachineBlockEntity implements MachineInteractions {
    public static final String NAME = "drop_processor";
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(32, 1, 1024);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).slot(52, 35).slot(112, 35).bar(0, 1, 75, 34, 176, 14, 48, 16, true).build();

    private final MachineEnchantments enchantments = new MachineEnchantments(Enchantments.FORTUNE, Enchantments.EFFICIENCY);
    private final List<ItemStack> overflow = new ArrayList<>();
    private int progress;

    public DropProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.DROP_PROCESSOR.type().get(), pos, state, 2, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public MachineEnchantments enchantments() {
        return enchantments;
    }

    public int overflowCount() {
        return overflow.size();
    }

    public int progress() {
        return progress;
    }

    public static boolean isProcessable(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return slot == INPUT && isProcessable(stack);
    }

    @Override
    protected boolean mayExtract(int slot) {
        return slot > INPUT;
    }

    /** The ticks a block takes at this speed. */
    public int operationTime() {
        int base = PowerRequirement.operationTime(300, 20, omega);
        return Math.max(1, (int) (base / Math.pow(1.3, enchantments.level(Enchantments.EFFICIENCY))));
    }

    /** Blocks done a tick: one, and more once the time formula has run out of ticks. */
    public int operationsPerTick() {
        double raw = 300 - 20 * (Math.log(omega + 1D) / Math.log(2));
        return raw >= 1 ? 1 : 1 + (int) Math.min(8, Math.floor(1 - raw));
    }

    @Override
    public int extra(int index) {
        return index == 0 ? progress : 2 * operationTime();
    }

    @Override
    protected int extraCount() {
        return 2;
    }

    /** What the block of {@code stack} drops when it is broken here, with this machine's Fortune. */
    public List<ItemStack> dropsOf(ServerLevel server, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return List.of();
        }
        BlockState state = item.getBlock().defaultBlockState();
        LootParams.Builder params = new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withParameter(LootContextParams.TOOL, enchantments.tool(server.registryAccess()));
        return state.getDrops(params);
    }

    private void process(ServerLevel server) {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack drop : dropsOf(server, items.getStackInSlot(INPUT))) {
            if (drop.isEmpty()) {
                continue;
            }
            boolean merged = false;
            for (ItemStack there : drops) {
                if (ItemStack.isSameItemSameComponents(there, drop) && there.getCount() + drop.getCount() <= there.getMaxStackSize()) {
                    there.grow(drop.getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                drops.add(drop.copy());
            }
        }
        if (!drops.isEmpty()) {
            items.setStackInSlot(OUTPUT, drops.remove(0));
            overflow.addAll(drops);
        }
        items.extractItem(INPUT, 1, false);
    }

    private boolean operate(ServerLevel server, boolean multiple) {
        if (items.getStackInSlot(INPUT).isEmpty()) {
            progress = 0;
            return false;
        }
        if (!items.getStackInSlot(OUTPUT).isEmpty()) {
            progress = Math.max(0, Math.min(progress, operationTime() - 1));
            return false;
        }
        if (!overflow.isEmpty() || !isProcessable(items.getStackInSlot(INPUT))) {
            progress = 0;
            return false;
        }
        progress++;
        if (multiple || progress >= operationTime()) {
            progress = 0;
            process(server);
        }
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!overflow.isEmpty() && items.getStackInSlot(OUTPUT).isEmpty()) {
            items.setStackInSlot(OUTPUT, overflow.remove(0));
        } else if (powered && MachineConfig.enabled("dropProcessor")) {
            int n = operationsPerTick();
            boolean worked = false;
            for (int i = 0; i < n; i++) {
                worked |= operate(server, n > 1);
            }
            if (worked) {
                setChanged();
            }
        } else {
            progress = 0;
        }
    }

    // ---- enchanted books, and what is left when it is broken ----

    @Override
    public boolean onItemUse(ItemStack stack, Player player, InteractionHand hand) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            if (enchantments.apply(stack)) {
                setChanged();
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onBroken(ServerLevel server) {
        for (ItemStack stack : overflow) {
            Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
        }
        overflow.clear();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("enchants", enchantments.save());
        tag.putInt("progress", progress);
        ListTag list = new ListTag();
        for (ItemStack stack : overflow) {
            list.add(stack.save(registries));
        }
        tag.put("overflow", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        enchantments.load(MachineEnchantments.listTag(tag, "enchants"));
        progress = tag.getInt("progress");
        overflow.clear();
        for (Tag t : tag.getList("overflow", Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, t).ifPresent(overflow::add);
        }
    }
}
