package net.scwunge.rotarycraft.blockentity;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.menu.TerraformerMenu;
import net.scwunge.rotarycraft.power.BiomeTransforms;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import net.scwunge.rotarycraft.weapon.turret.OmniConsumerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Terraformer, as the original: given a redstone signal and a target biome it works through the land around it, turning the biome of one place
 * at a time into the target. Each step of biome to biome (see {@link BiomeTransforms}) needs a minimum of shaft power (from any side), some
 * water in its tank, and the right plants or blocks in its 54 slots, which are used up by chance. With a diamond inside (and block editing on) it
 * also remakes the surface to suit the new biome. Off unless the server enables it (config, world_machines); acts as its owner, so claims
 * can stop it.
 * <p>
 * One difference from the original: 1.21 keeps biomes in 4 by 4 columns, so each step changes one such 4 by 4 patch (the original did a single
 * block column), whatever the cost listed for the step.
 */
public class TerraformerBlockEntity extends OmniConsumerBlockEntity implements MenuProvider, Owned, net.scwunge.rotarycraft.handheld.SelectableTiles {
    public static final int SLOTS = 54;
    public static final int TANK = 24000;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    public static final int DEFAULT_RADIUS = 16;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank tank = new FluidTank(TANK, fluid -> fluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    @Nullable
    private ResourceKey<Biome> target;
    private int radius = DEFAULT_RADIUS;
    /** The 4 by 4 patches of land left to do, as packed (x, z) biome-cell coordinates. */
    private final LongArrayList cells = new LongArrayList();
    /** The patches picked out with a Tile Selector; when there are any they are done instead of the radius. */
    private final LongArrayList selected = new LongArrayList();
    private boolean built;
    private int tickCount;
    @Nullable
    private WorldGuard.Owner owner;

    public TerraformerBlockEntity(BlockPos pos, BlockState state) {
        super(WorldMachineRegistry.TERRAFORMER_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    public ItemStackHandler items() {
        return items;
    }

    public FluidTank tank() {
        return tank;
    }

    /** What automation may do: put things in (nothing comes out). */
    public net.neoforged.neoforge.items.IItemHandler automationItems() {
        return new net.neoforged.neoforge.items.IItemHandler() {
            @Override
            public int getSlots() {
                return items.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return items.isItemValid(slot, stack);
            }
        };
    }

    @Nullable
    public ResourceKey<Biome> target() {
        return target;
    }

    public int radius() {
        return radius;
    }

    /** How many patches are still to do. */
    public int remaining() {
        return cells.size();
    }

    public void setTarget(@Nullable ResourceKey<Biome> biome) {
        target = biome;
        built = false;
        setChanged();
    }

    @Override
    public boolean addTile(BlockPos pos) {
        if (level == null || level.hasNeighborSignal(worldPosition)) {
            return false;
        }
        long cell = pack(pos.getX() >> 2, pos.getZ() >> 2);
        if (!selected.contains(cell)) {
            selected.add(cell);
            built = false;
            setChanged();
        }
        return true;
    }

    @Override
    public String selectionName() {
        return "the Terraformer at " + worldPosition.getX() + ", " + worldPosition.getY() + ", " + worldPosition.getZ();
    }

    /** How many patches the Tile Selector has picked. */
    public int selectedCount() {
        return selected.size();
    }

    public void setRadius(int r) {
        selected.clear();
        radius = Math.max(1, Math.min(r, RotaryConfig.get(RotaryConfig.TERRAFORMER_MAX_RADIUS)));
        built = false;
        setChanged();
    }

    /** The biome at the machine. */
    public ResourceKey<Biome> centralBiome() {
        return level == null ? Biomes.PLAINS : level.getBiome(worldPosition).unwrapKey().orElse(Biomes.PLAINS);
    }

    private static long pack(int qx, int qz) {
        return ((long) qx << 32) | (qz & 0xFFFFFFFFL);
    }

    private void rebuild() {
        cells.clear();
        if (!selected.isEmpty()) {
            cells.addAll(selected);
            built = true;
            return;
        }
        int minX = (worldPosition.getX() - radius) >> 2;
        int maxX = (worldPosition.getX() + radius) >> 2;
        int minZ = (worldPosition.getZ() - radius) >> 2;
        int maxZ = (worldPosition.getZ() + radius) >> 2;
        for (int qx = minX; qx <= maxX; qx++) {
            for (int qz = minZ; qz <= maxZ; qz++) {
                cells.add(pack(qx, qz));
            }
        }
        built = true;
    }

    // ---- the work ----

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !RotaryConfig.worldMachineEnabled("terraformer")) {
            return;
        }
        tickCount++;
        if (target == null || !server.hasNeighborSignal(worldPosition)) {
            return;
        }
        if (!built) {
            rebuild();
        }
        if (cells.isEmpty()) {
            return;
        }
        if (tickCount >= PowerRequirement.operationTime(800, 40, omega)) {
            int index = server.random.nextInt(cells.size());
            long cell = cells.getLong(index);
            if (convert(server, (int) (cell >> 32), (int) cell)) {
                cells.set(index, cells.getLong(cells.size() - 1));
                cells.removeLong(cells.size() - 1);
            }
            tickCount = 0;
        }
    }

    private BlockPos surface(ServerLevel server, int x, int z) {
        return server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z)).below();
    }

    private boolean hasItem(net.minecraft.world.item.Item item) {
        return slotOf(item) >= 0;
    }

    private int slotOf(net.minecraft.world.item.Item item) {
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).is(item)) {
                return i;
            }
        }
        return -1;
    }

    /** Works on one 4 by 4 patch; true if it is done (changed, or already the target), false if it must wait or cannot be done. */
    private boolean convert(ServerLevel server, int qx, int qz) {
        ResourceKey<Biome> goal = target;
        int bx = qx * 4 + 2;
        int bz = qz * 4 + 2;
        BlockPos probe = new BlockPos(bx, worldPosition.getY(), bz);
        if (goal == null || !server.hasChunkAt(probe)) {
            return false;
        }
        BlockPos top = surface(server, bx, bz);
        ResourceKey<Biome> current = server.getBiome(probe).unwrapKey().orElse(null);
        if (current == null) {
            return false;
        }
        if (current.equals(goal)) {
            return true;
        }
        BiomeTransforms.Step step = BiomeTransforms.step(current, goal);
        if (step == null || getPower() < step.power() || tank.getFluidAmount() < step.waterMb() || !WorldGuard.mayChange(server, top, owner)) {
            return false;
        }
        for (BiomeTransforms.Requirement req : step.items()) {
            if (!hasItem(req.item())) {
                return false;
            }
        }
        for (BiomeTransforms.Requirement req : step.items()) {
            if (req.chance() >= 1 || server.random.nextFloat() < req.chance()) {
                int slot = slotOf(req.item());
                if (slot >= 0) {
                    items.extractItem(slot, 1, false);
                }
            }
        }
        if (step.waterMb() > 0) {
            tank.drain(step.waterMb(), IFluidHandler.FluidAction.EXECUTE);
        }
        setBiome(server, qx, qz, goal);
        if (RotaryConfig.get(RotaryConfig.TERRAFORMER_EDITS_BLOCKS) && hasItem(Items.DIAMOND)) {
            remakeSurface(server, qx, qz, goal);
        }
        setChanged();
        return true;
    }

    /** Sets the biome of the 4 by 4 patch (at biome-cell coordinates {@code qx}, {@code qz}) from the bottom of the world to the top. */
    public static void setBiome(ServerLevel server, int qx, int qz, ResourceKey<Biome> goal) {
        Holder<Biome> holder = server.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(goal);
        LevelChunk chunk = server.getChunk(qx >> 2, qz >> 2);
        int lx = qx & 3;
        int lz = qz & 3;
        for (LevelChunkSection section : chunk.getSections()) {
            @SuppressWarnings("unchecked")
            PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) section.getBiomes();
            for (int ly = 0; ly < 4; ly++) {
                biomes.getAndSetUnchecked(lx, ly, lz, holder);
            }
        }
        chunk.setUnsaved(true);
        server.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
    }

    private static boolean isSnowy(ResourceKey<Biome> biome) {
        return biome.equals(Biomes.SNOWY_PLAINS) || biome.equals(Biomes.SNOWY_TAIGA) || biome.equals(Biomes.FROZEN_OCEAN) || biome.equals(Biomes.ICE_SPIKES);
    }

    /** The block a biome's ground is made of, or null if it should be left alone. */
    @Nullable
    private static BlockState groundFor(ResourceKey<Biome> biome) {
        if (biome.equals(Biomes.DESERT)) {
            return Blocks.SAND.defaultBlockState();
        }
        if (biome.equals(Biomes.BADLANDS)) {
            return Blocks.RED_SAND.defaultBlockState();
        }
        if (biome.equals(Biomes.MUSHROOM_FIELDS)) {
            return Blocks.MYCELIUM.defaultBlockState();
        }
        if (biome.equals(Biomes.OCEAN) || biome.equals(Biomes.DEEP_OCEAN) || biome.equals(Biomes.FROZEN_OCEAN)) {
            return null;
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    /** Remakes the topmost layer of each column of the patch to suit the new biome (ground, and snow where it is cold). */
    private void remakeSurface(ServerLevel server, int qx, int qz, ResourceKey<Biome> goal) {
        BlockState ground = groundFor(goal);
        for (int x = qx * 4; x < qx * 4 + 4; x++) {
            for (int z = qz * 4; z < qz * 4 + 4; z++) {
                BlockPos top = surface(server, x, z);
                BlockState state = server.getBlockState(top);
                boolean soil = state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                        || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL) || state.is(Blocks.COARSE_DIRT);
                if (ground != null && soil && !state.is(ground.getBlock())) {
                    WorldGuard.setBlock(server, top, ground, owner);
                }
                BlockPos above = top.above();
                if (server.getBlockState(above).isAir() && isSnowy(goal) && !server.getBlockState(top).isAir() && server.getFluidState(top).isEmpty()) {
                    WorldGuard.setBlock(server, above, Blocks.SNOW.defaultBlockState(), owner);
                } else if (!isSnowy(goal) && server.getBlockState(above).is(Blocks.SNOW)) {
                    WorldGuard.setBlock(server, above, Blocks.AIR.defaultBlockState(), owner);
                }
            }
        }
    }

    // ---- screen ----

    public static final int DATA_COUNT = 8;

    public ContainerData data() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (level == null) {
                    return 0;
                }
                var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
                return switch (index) {
                    case 0 -> omega;
                    case 1 -> torque;
                    case 2 -> target == null ? -1 : biomes.getId(biomes.get(target));
                    case 3 -> biomes.getId(biomes.get(centralBiome()));
                    case 4 -> radius;
                    case 5 -> tank.getFluidAmount();
                    case 6 -> cells.size();
                    case 7 -> level.getBestNeighborSignal(worldPosition) > 0 || level.hasNeighborSignal(worldPosition) ? 1 : 0;
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
        return new TerraformerMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("radius", radius);
        tag.putLongArray("selected", selected.toLongArray());
        if (target != null) {
            tag.putString("target", target.location().toString());
        }
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        tank.readFromNBT(registries, tag.getCompound("tank"));
        radius = tag.contains("radius") ? tag.getInt("radius") : DEFAULT_RADIUS;
        selected.clear();
        for (long cell : tag.getLongArray("selected")) {
            selected.add(cell);
        }
        ResourceLocation id = tag.contains("target") ? ResourceLocation.tryParse(tag.getString("target")) : null;
        target = id == null ? null : ResourceKey.create(Registries.BIOME, id);
        built = false;
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writePower(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readPower(tag);
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
