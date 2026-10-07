package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.scwunge.rotarycraft.api.BorerDigEvent;
import net.scwunge.rotarycraft.block.MiningPipeBlock;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.menu.BorerMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Borer, as the original: it bores a tunnel in the direction it faces, a slice at a time. The first slice is the whole 7 wide by 5 high
 * face (its bottom row level with the borer); after that it cuts only the cells picked on its screen, so the tunnel can be any profile.
 * Every cell it cuts is left holding a mining pipe, which is also how it finds its place again after the power cuts out. Each slice needs
 * power and torque in proportion to the hardness of the blocks in it (Sharpness lowers the torque), and it takes longer the slower the
 * shaft turns. Enchanted books give it Fortune, Efficiency, Silk Touch and Sharpness. What it digs goes into a neighbouring inventory, or
 * is thrown out of its top. It acts as its owner: where a claim says no, or mobGriefing is off, it jams and tells them.
 */
public class BorerBlockEntity extends ConsumerBlockEntity implements MenuProvider, Owned {
    public static final int COLS = 7;
    public static final int ROWS = 5;
    public static final int MAINTENANCE_LIFE = 256;
    /** Power to break a block, per 0.1 of hardness. */
    public static final int BASE_DIG_POWER = 64;
    private static final int MAX_SKIP = 128;
    private static final int PROTECTION_NOTICES = 10;

    private final MachineEnchantments enchantments = new MachineEnchantments(Enchantments.FORTUNE, Enchantments.EFFICIENCY, Enchantments.SILK_TOUCH,
            Enchantments.SHARPNESS);
    private final boolean[][] cutShape = new boolean[COLS][ROWS];
    private boolean drops = true;
    private int step = 1;
    private boolean jammed;
    private boolean hitProtection;
    private int notifiedPlayer;
    private int durability = Integer.MAX_VALUE;
    private int requiredPower;
    private int requiredTorque;
    private int tickCount;
    private boolean miningAir;
    private int syncedStep = -1;
    @Nullable
    private WorldGuard.Owner owner;

    public BorerBlockEntity(BlockPos pos, BlockState state) {
        super(WorldMachineRegistry.BORER_BE.get(), pos, state);
        if (RotaryConfig.get(RotaryConfig.BORER_MAINTENANCE)) {
            durability = MAINTENANCE_LIFE;
        }
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(1, 1, 1);
    }

    @Override
    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    public MachineEnchantments enchantments() {
        return enchantments;
    }

    // ---- settings ----

    public boolean cell(int col, int row) {
        return cutShape[col][row];
    }

    public void toggleCell(int col, int row) {
        cutShape[col][row] = !cutShape[col][row];
        setChanged();
    }

    public void toggleAll() {
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                cutShape[i][j] = !cutShape[i][j];
            }
        }
        setChanged();
    }

    public boolean drops() {
        return drops;
    }

    public void toggleDrops() {
        drops = !drops;
        setChanged();
    }

    /** The cut shape as bits, cell (i, j) at bit i * ROWS + j. */
    public long cutMask() {
        long mask = 0;
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                if (cutShape[i][j]) {
                    mask |= 1L << (i * ROWS + j);
                }
            }
        }
        return mask;
    }

    public void setCutMask(long mask) {
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                cutShape[i][j] = (mask & (1L << (i * ROWS + j))) != 0;
            }
        }
        setChanged();
    }

    public boolean isJammed() {
        return jammed;
    }

    public int step() {
        return step;
    }

    public int requiredTorque() {
        return requiredTorque;
    }

    public int requiredPower() {
        return requiredPower;
    }

    public int durability() {
        return durability;
    }

    /** Starts over from the borer's face; it finds the end of its pipes again by itself. */
    public void reset() {
        step = 1;
        setChanged();
    }

    /** A new drill: whether it was needed. */
    public boolean repair() {
        if (durability > 0) {
            return false;
        }
        durability = RotaryConfig.get(RotaryConfig.BORER_MAINTENANCE) ? MAINTENANCE_LIFE : Integer.MAX_VALUE;
        setChanged();
        return true;
    }

    public boolean isIdle() {
        return !hasCells();
    }

    private boolean hasCells() {
        for (boolean[] column : cutShape) {
            for (boolean cell : column) {
                if (cell) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The block position of cell ({@code col}, {@code row}) in the slice {@code at} blocks from the borer. */
    public BlockPos cellPos(int at, int col, int row) {
        Direction dir = facing();
        Direction.Axis lateral = dir.getAxis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        BlockPos p = worldPosition.relative(dir, at).above(ROWS - 1 - row);
        return p.relative(Direction.fromAxisAndDirection(lateral, Direction.AxisDirection.POSITIVE), col - COLS / 2);
    }

    /** Whether the cell is dug in this slice: the first slice takes the whole face. */
    private boolean cutsCell(int col, int row) {
        return cutShape[col][row] || step == 1;
    }

    /** Ticks per slice (the original's 720 - 40 * log2(speed + 1), faster with Efficiency). */
    public int operationTime() {
        int base = PowerRequirement.operationTime(720, 40, omega);
        return (int) (base / Math.pow(1.3, enchantments.level(Enchantments.EFFICIENCY)));
    }

    public static int torqueForHardness(float hardness, int sharpness) {
        float c = 10 - 0.5F * sharpness;
        int add = ceilPseudo2Exp((int) (c * hardness));
        if (sharpness > 0) {
            add = Math.min(add, 1 << (10 - sharpness / 3));
        }
        return add;
    }

    /** The next power of two, or the one and a half times the one before it, that is at least {@code val} (as the original's helper). */
    private static int ceilPseudo2Exp(int val) {
        int pow = val <= 0 ? 0 : (val == 1 ? 1 : Integer.highestOneBit(val - 1) << 1);
        int log = pow > 0 ? 31 - Integer.numberOfLeadingZeros(pow) : 0;
        int prev = log - 1 < 0 ? 1 : 1 << (log - 1);
        int mid = prev * 3 / 2;
        if (prev >= val) {
            return prev;
        } else if (mid >= val) {
            return mid;
        }
        return pow;
    }

    // ---- the work ----

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !RotaryConfig.diggerEnabled("borer")) {
            return;
        }
        tickCount++;
        if (enchantments.any() && server.getGameTime() % 5 == 0) {
            server.sendParticles(ParticleTypes.PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 3, 0.5, 0.4, 0.5, 0.3);
        }
        if (getPower() <= 0) {
            setJammed(false);
            step = 1;
            return;
        }
        if (hitProtection && notifiedPlayer < PROTECTION_NOTICES && server.getGameTime() % 100 == 0 && owner != null) {
            ServerPlayer player = server.getServer().getPlayerList().getPlayer(owner.id());
            if (player != null) {
                notifiedPlayer++;
                BlockPos head = worldPosition.relative(facing(), step);
                player.sendSystemMessage(Component.translatable("message.rotarycraft.borer.protected", head.getX(), head.getZ()));
            }
        }
        if (durability <= 0) {
            if (tickCount % 5 == 0) {
                worn(server, 0.05f);
            }
            return;
        }
        if (jammed && tickCount % 5 == 0) {
            worn(server, 1f);
        }
        if (!hasCells() || omega <= 0) {
            return;
        }
        if (tickCount == 1 || step == 1) {
            miningAir = checkMiningAir(server);
        }
        if (tickCount >= operationTime() || (miningAir && tickCount % 5 == 0)) {
            skipMiningPipes(server);
            calculateRequiredPower(server);
            if (getPower() >= requiredPower && requiredPower != -1) {
                dig(server);
                if (!miningAir) {
                    net.scwunge.rotarycraft.sound.MachineSounds.playOnce(server, worldPosition, "rumble", 1F, 1F);
                }
                if (!miningAir && durability != Integer.MAX_VALUE) {
                    durability--;
                }
            } else {
                setJammed(true);
            }
            tickCount = 0;
            miningAir = false;
            syncIfChanged(server);
        }
    }

    private void worn(ServerLevel server, float pitch) {
        server.playSound(null, worldPosition, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.75f, pitch);
        server.sendParticles(ParticleTypes.SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.01);
        server.sendParticles(ParticleTypes.CRIT, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.01);
    }

    private void setJammed(boolean jam) {
        if (jam != jammed) {
            jammed = jam;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    }

    private void syncIfChanged(ServerLevel server) {
        if (step != syncedStep) {
            syncedStep = step;
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** The block at {@code pos}, loading or generating its chunk (the borer digs ahead of anything the players have loaded). */
    private static BlockState stateAt(ServerLevel server, BlockPos pos) {
        if (pos.getY() < server.getMinBuildHeight() || pos.getY() >= server.getMaxBuildHeight()) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        ChunkAccess chunk = server.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);
        return chunk.getBlockState(pos);
    }

    /** The chunks round the head are made as it comes to them, as far out as the config says. */
    private void generateAround(ServerLevel server) {
        int radius = RotaryConfig.get(RotaryConfig.BORER_CHUNK_RADIUS);
        if (radius > 0) {
            BlockPos head = worldPosition.relative(facing(), step);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    server.getChunk((head.getX() >> 4) + dx, (head.getZ() >> 4) + dz, ChunkStatus.FULL, true);
                }
            }
        }
    }

    private boolean checkMiningAir(ServerLevel server) {
        generateAround(server);
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                if (cutsCell(i, j) && !stateAt(server, cellPos(step, i, j)).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Steps over slices that are already all pipes along its own line (so it picks up where it left off). */
    private void skipMiningPipes(ServerLevel server) {
        Direction.Axis axis = facing().getAxis();
        for (int skipped = 0; skipped < MAX_SKIP; skipped++) {
            for (int i = 0; i < COLS; i++) {
                for (int j = 0; j < ROWS; j++) {
                    if (cutsCell(i, j)) {
                        BlockState state = stateAt(server, cellPos(step, i, j));
                        if (!state.is(WorldMachineRegistry.MINING_PIPE.get())) {
                            return;
                        }
                        MiningPipeBlock.Kind kind = state.getValue(MiningPipeBlock.KIND);
                        if (kind != MiningPipeBlock.Kind.COLLAR && kind.axis() != axis) {
                            return;
                        }
                    }
                }
            }
            step++;
        }
    }

    private void calculateRequiredPower(ServerLevel server) {
        requiredPower = 0;
        requiredTorque = 0;
        int sharpness = enchantments.level(Enchantments.SHARPNESS);
        double factor = RotaryConfig.get(RotaryConfig.BORER_POWER_FACTOR);
        int digPower = (int) (BASE_DIG_POWER * factor);
        try {
            for (int i = 0; i < COLS; i++) {
                for (int j = 0; j < ROWS; j++) {
                    if (!cutsCell(i, j)) {
                        continue;
                    }
                    BlockPos pos = cellPos(step, i, j);
                    if (step > RotaryConfig.get(RotaryConfig.BORER_MAX_LENGTH)) {
                        requiredPower = -1;
                        return;
                    }
                    BlockState state = stateAt(server, pos);
                    if (ignorable(state)) {
                        continue;
                    }
                    float hardness = state.getDestroySpeed(server, pos);
                    if (hardness < 0 || server.getBlockEntity(pos) instanceof PowerBlockEntity) {
                        requiredPower = -1;
                        return;
                    }
                    requiredPower += (int) (digPower * 10 * hardness);
                    requiredTorque += torqueForHardness(hardness, sharpness);
                }
            }
        } catch (RuntimeException e) {
            requiredPower = -1;
            return;
        }
        if (torque < requiredTorque) {
            requiredPower = -1;
        }
    }

    private static boolean ignorable(BlockState state) {
        return state.isAir() || !state.getFluidState().isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock;
    }

    private void dig(ServerLevel server) {
        BlockState pipe = WorldMachineRegistry.MINING_PIPE.get().defaultBlockState().setValue(MiningPipeBlock.KIND,
                step == 1 ? MiningPipeBlock.Kind.COLLAR : MiningPipeBlock.Kind.along(facing().getAxis()));
        ItemStack tool = enchantments.tool(server.registryAccess());
        boolean blocked = false;
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                if (!cutsCell(i, j)) {
                    continue;
                }
                BlockPos pos = cellPos(step, i, j);
                BlockState state = stateAt(server, pos);
                if (state.is(WorldMachineRegistry.MINING_PIPE.get())) {
                    continue;
                }
                if (!digCell(server, pos, state, tool)) {
                    blocked = true;
                    continue;
                }
                server.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
                server.setBlock(pos, pipe, 3);
            }
        }
        if (blocked) {
            setJammed(true);
            return;
        }
        setJammed(false);
        hitProtection = false;
        NeoForge.EVENT_BUS.post(new BorerDigEvent(this, step, worldPosition.relative(facing(), step),
                enchantments.has(Enchantments.SILK_TOUCH)));
        step++;
        setChanged();
    }

    /** Clears one block for the pipe to take its place, handling its drops; false if it may not (bedrock, a claim, a machine). */
    private boolean digCell(ServerLevel server, BlockPos pos, BlockState state, ItemStack tool) {
        if (state.isAir()) {
            return true;
        }
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME) || state.getDestroySpeed(server, pos) < 0) {
            return false;
        }
        BlockEntity be = server.getBlockEntity(pos);
        if (be instanceof PowerBlockEntity) {
            return false;
        }
        if (!WorldGuard.mayChange(server, pos, owner)) {
            hitProtection = true;
            return false;
        }
        if (drops) {
            List<ItemStack> items = new ArrayList<>();
            if (be instanceof Container container) {
                for (int i = 0; i < container.getContainerSize(); i++) {
                    items.add(container.removeItemNoUpdate(i));
                }
            }
            if (state.is(Blocks.SPAWNER) && be instanceof SpawnerBlockEntity spawner) {
                ItemStack spawnerItem = new ItemStack(Items.SPAWNER);
                CompoundTag data = spawner.saveCustomOnly(server.registryAccess());
                data.putString("id", "minecraft:mob_spawner");
                spawnerItem.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
                items.add(spawnerItem);
            } else {
                items.addAll(net.minecraft.world.level.block.Block.getDrops(state, server, pos, be, WorldGuard.actor(server, owner), tool));
            }
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    deliver(server, stack);
                }
            }
        } else if (be instanceof Container container) {
            container.clearContent();
        }
        return true;
    }

    /** Into an inventory next to the borer if one takes it, else out of its top. */
    private void deliver(ServerLevel server, ItemStack stack) {
        ItemStack left = stack;
        for (Direction side : Direction.values()) {
            IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
            if (handler != null) {
                left = ItemHandlerHelper.insertItem(handler, left, false);
                if (left.isEmpty()) {
                    return;
                }
            }
        }
        ItemEntity entity = new ItemEntity(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.125, worldPosition.getZ() + 0.5, left);
        entity.setDeltaMovement((server.random.nextDouble() - 0.5) * 0.2, 0.2, (server.random.nextDouble() - 0.5) * 0.2);
        server.addFreshEntity(entity);
    }

    // ---- screen ----

    public static final int DATA_COUNT = 10;

    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                long mask = cutMask();
                return switch (index) {
                    case 0 -> omega;
                    case 1 -> torque;
                    case 2 -> requiredPower;
                    case 3 -> requiredTorque;
                    case 4 -> (jammed ? 1 : 0) | (drops ? 2 : 0) | (durability <= 0 ? 4 : 0);
                    case 5 -> (int) (mask & 0xFFFFFFFFL);
                    case 6 -> (int) (mask >>> 32);
                    case 7 -> step;
                    case 8 -> getPower() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) getPower();
                    default -> 0;
                };
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

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BorerMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("drops", drops);
        tag.putLong("cut", cutMask());
        tag.putInt("step", step);
        tag.putBoolean("jammed", jammed);
        tag.putInt("durability", durability);
        tag.putBoolean("protected", hitProtection);
        tag.put("enchants", enchantments.save());
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        drops = !tag.contains("drops") || tag.getBoolean("drops");
        long mask = tag.getLong("cut");
        for (int i = 0; i < COLS; i++) {
            for (int j = 0; j < ROWS; j++) {
                cutShape[i][j] = (mask & (1L << (i * ROWS + j))) != 0;
            }
        }
        step = Math.max(1, tag.getInt("step"));
        jammed = tag.getBoolean("jammed");
        durability = tag.contains("durability") ? tag.getInt("durability") : durability;
        hitProtection = tag.getBoolean("protected");
        enchantments.load(MachineEnchantments.listTag(tag, "enchants"));
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("jammed", jammed);
        tag.putInt("step", step);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        jammed = tag.getBoolean("jammed");
        step = tag.getInt("step");
    }

    /** The default would load the packet as if it were saved data, skipping {@link #handleUpdateTag}, so clients would never see the sync. */
    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
