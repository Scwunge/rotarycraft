package net.scwunge.rotarycraft.farm;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.blockentity.MachineEnchantments;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.weapon.WorldGuard;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Woodcutter, as the original: set facing a tree (its logs within the 3 by 3 in front, at its own height), with 16 kW and 64 N*m, it reads
 * the whole tree (logs and the natural leaves joined to them, up to 4096 blocks), and takes it down from the top, leaves first, one block each
 * 40 - 4 x log2(speed) ticks (and several at once on a very fast shaft). What a block drops goes into the chest or hopper below the machine
 * (or on the ground under it); saplings that match the tree are kept in its one slot, and a sapling there is planted where the trunk
 * stood, one for each tree (a book of infinity makes it free; fortune also makes apples more likely, and efficiency makes it quicker).
 * It cuts as its owner: claims and mobGriefing stop it. Off by default in the farm config. Differences from the original: trees
 * only fall instantly (never as falling blocks), leaves must not be player placed, and the saw does not hurt creatures.
 */
public class WoodcutterBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(64, 1, 16384);
    public static final int MAX_BLOCKS = 4096;

    private final ItemStackHandler sapling = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };
    private final MachineEnchantments enchantments = new MachineEnchantments(Enchantments.INFINITY, Enchantments.FORTUNE, Enchantments.EFFICIENCY);
    /** The blocks still to cut, as positions, in the order they will be. */
    private final LongArrayList queue = new LongArrayList();
    private int timer;
    /** The sapling the tree being cut grows from, or null. */
    private ResourceLocation treeSapling;

    public WoodcutterBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.WOODCUTTER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "woodcutter";
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public ItemStackHandler items() {
        return sapling;
    }

    @Override
    public FarmUi ui() {
        return FarmUi.custom("one_slot", 80, 35);
    }

    @Override
    public MachineEnchantments enchantments() {
        return enchantments;
    }

    public int remaining() {
        return queue.size();
    }

    /** The log of a tree in the 3 by 3 in front, at the machine's height, or null. */
    private BlockPos findLog(ServerLevel server) {
        BlockPos centre = worldPosition.relative(facing());
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos at = centre.offset(i, 0, j);
                if (server.getBlockState(at).is(BlockTags.LOGS)) {
                    return at;
                }
            }
        }
        return null;
    }

    public boolean hasWood() {
        return level instanceof ServerLevel server && findLog(server) != null;
    }

    /** The sapling that grows into the tree this log belongs to, or null. */
    public static ResourceLocation saplingFor(Block log) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(log);
        String name = id.getPath().replace("stripped_", "").replace("_log", "").replace("_wood", "").replace("_stem", "").replace("_hyphae", "");
        for (String suffix : new String[] {"_sapling", "_propagule"}) {
            ResourceLocation candidate = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), name + suffix);
            if (BuiltInRegistries.ITEM.containsKey(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** Reads the tree whose log is at {@code start}: every log and natural leaf joined to it, top first, leaves before logs, near before far. */
    private void readTree(ServerLevel server, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        List<BlockPos> found = new ArrayList<>();
        BlockPos centre = worldPosition.relative(facing());
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos at = centre.offset(i, 0, j);
                if (server.getBlockState(at).is(BlockTags.LOGS) && seen.add(at)) {
                    open.add(at);
                }
            }
        }
        while (!open.isEmpty() && found.size() < MAX_BLOCKS) {
            BlockPos at = open.poll();
            found.add(at);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = at.offset(dx, dy, dz);
                        if (!seen.contains(next) && server.isLoaded(next) && isTreeBlock(server.getBlockState(next)) && next.getY() >= worldPosition.getY()) {
                            seen.add(next);
                            open.add(next);
                        }
                    }
                }
            }
        }
        found.sort(Comparator.<BlockPos>comparingInt(p -> -p.getY())
                .thenComparingInt(p -> server.getBlockState(p).is(BlockTags.LEAVES) ? 0 : 1)
                .thenComparingDouble(p -> p.distSqr(worldPosition)));
        queue.clear();
        for (BlockPos p : found) {
            queue.add(p.asLong());
        }
        treeSapling = saplingFor(server.getBlockState(start).getBlock());
        BlockItem held = sapling.getStackInSlot(0).getItem() instanceof BlockItem b ? b : null;
        if (held != null && !BuiltInRegistries.ITEM.getKey(held).equals(treeSapling)) {
            dumpSapling(server);
        }
        setChanged();
    }

    private static boolean isTreeBlock(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) && state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT);
    }

    private void dumpSapling(ServerLevel server) {
        ItemStack held = sapling.getStackInSlot(0);
        if (!held.isEmpty()) {
            sapling.setStackInSlot(0, ItemStack.EMPTY);
            server.addFreshEntity(new ItemEntity(server, worldPosition.getX() + 0.5, worldPosition.getY() - 0.25, worldPosition.getZ() + 0.5, held));
        }
    }

    public int operationTime() {
        double raw = 40 - 4 * (Math.log(Math.max(0, omega) + 1D) / Math.log(2));
        int base = (int) Math.max(1, raw);
        double efficiency = 1 + enchantLevel(Enchantments.EFFICIENCY) * 0.3;
        return (int) Math.max(1, base / efficiency);
    }

    public int operations() {
        double raw = 40 - 4 * (Math.log(Math.max(0, omega) + 1D) / Math.log(2));
        return 1 + (int) Math.max(0, 1 - raw);
    }

    @Override
    protected void machineTick(boolean powered) {
        ServerLevel server = server();
        if (!powered || getTorque() < REQUIREMENT.minTorque()) {
            return;
        }
        if (queue.isEmpty()) {
            BlockPos log = findLog(server);
            if (log == null) {
                return;
            }
            readTree(server, log);
        }
        if (++timer < operationTime()) {
            return;
        }
        timer = 0;
        for (int i = 0; i < operations() && !queue.isEmpty(); i++) {
            cut(server, BlockPos.of(queue.removeLong(0)));
        }
        if (queue.isEmpty()) {
            setChanged();
        }
    }

    private void cut(ServerLevel server, BlockPos at) {
        BlockState state = server.getBlockState(at);
        if (!isTreeBlock(state) || !server.isLoaded(at)) {
            return;
        }
        boolean log = state.is(BlockTags.LOGS);
        List<ItemStack> drops = Block.getDrops(state, server, at, null, null, enchantments.tool(server.registryAccess()));
        if (!WorldGuard.breakBlock(server, at, owner, false)) {
            return;
        }
        if (server.getGameTime() % 4 == 0) {
            server.playSound(null, worldPosition, state.is(BlockTags.LEAVES) ? SoundEvents.GRASS_BREAK : SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.5F + server.random.nextFloat() * 0.5F, 1F);
        }
        if (log && server.random.nextInt(3) == 0) {
            drops.add(new ItemStack(RotaryItems.SAWDUST.get(), 1 + server.random.nextInt(4)));
        }
        for (ItemStack drop : drops) {
            deliver(server, drop);
        }
        if (log && at.getY() == worldPosition.getY() && !sapling.getStackInSlot(0).isEmpty()) {
            plant(server, at);
        }
    }

    private void deliver(ServerLevel server, ItemStack drop) {
        if (treeSapling != null && BuiltInRegistries.ITEM.getKey(drop.getItem()).equals(treeSapling)) {
            ItemStack held = sapling.getStackInSlot(0);
            if (enchantLevel(Enchantments.INFINITY) == 0 && (held.isEmpty() || ItemStack.isSameItemSameComponents(held, drop) && held.getCount() < held.getMaxStackSize())) {
                int room = held.isEmpty() ? drop.getMaxStackSize() : held.getMaxStackSize() - held.getCount();
                int add = Math.min(room, drop.getCount());
                sapling.setStackInSlot(0, held.isEmpty() ? drop.copyWithCount(add) : held.copyWithCount(held.getCount() + add));
                drop.shrink(add);
            } else if (held.isEmpty() && enchantLevel(Enchantments.INFINITY) > 0) {
                sapling.setStackInSlot(0, drop.copyWithCount(1));
                drop.shrink(1);
            }
            if (drop.isEmpty()) {
                return;
            }
        }
        IItemHandler chest = server.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.below(), Direction.UP);
        ItemStack rest = chest == null ? drop : ItemHandlerHelper.insertItem(chest, drop, false);
        if (!rest.isEmpty()) {
            server.addFreshEntity(new ItemEntity(server, worldPosition.getX() + 0.5, worldPosition.getY() - 0.25, worldPosition.getZ() + 0.5, rest));
        }
    }

    private void plant(ServerLevel server, BlockPos at) {
        ItemStack held = sapling.getStackInSlot(0);
        if (!(held.getItem() instanceof BlockItem item) || !server.getBlockState(at).isAir()) {
            return;
        }
        BlockState plant = item.getBlock().defaultBlockState();
        if (plant.canSurvive(server, at) && WorldGuard.setBlock(server, at, plant, owner)) {
            if (enchantLevel(Enchantments.INFINITY) == 0) {
                sapling.extractItem(0, 1, false);
            }
        }
    }

    @Override
    protected int[] status() {
        return new int[] {queue.size(), hasWood() ? 1 : 0};
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("tree", queue.toLongArray());
        tag.putInt("timer", timer);
        if (treeSapling != null) {
            tag.putString("treeSapling", treeSapling.toString());
        }
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        queue.clear();
        for (long l : tag.getLongArray("tree")) {
            queue.add(l);
        }
        timer = tag.getInt("timer");
        treeSapling = tag.contains("treeSapling") ? ResourceLocation.tryParse(tag.getString("treeSapling")) : null;
    }
}
