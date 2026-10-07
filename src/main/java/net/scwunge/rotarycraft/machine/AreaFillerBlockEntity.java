package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The base of the Block Filler and the Spiller (TileEntityAreaFiller): it finds the connected space beneath itself (air, for a Spiller also the
 * flowing fluid it is about to place, for a Block Filler also any fluid) within the configured range, and fills it from the lowest layer up, nearest the
 * machine first, one block each time it works. What it places and what that costs is up to the subclass.
 */
public abstract class AreaFillerBlockEntity extends InventoryMachineBlockEntity {
    /** The most blocks one search will look at, so a filler above a great cavern cannot stall the server. */
    private static final int SEARCH_LIMIT = 40_000;

    private final List<BlockPos> targets = new ArrayList<>();
    private int tickCount;
    private final String switchName;

    protected AreaFillerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, String menuKey, String switchName) {
        super(type, pos, state, slots, menuKey);
        this.switchName = switchName;
    }

    /** Whether the fluid it places may overwrite other fluids (the Block Filler fills over them). */
    protected abstract boolean allowFluidOverwrite();

    /** The power the next block needs. */
    protected abstract long requiredPower();

    /** Whether there is anything left to place. */
    protected abstract boolean hasRemainingBlocks();

    /** The block it places next, or null. */
    protected abstract BlockState nextBlock();

    /** It has placed a block: use up what that took. */
    protected abstract void onBlockPlaced();

    protected abstract boolean isFluidBlock(BlockState state);

    /** The ticks between blocks: 22 - 2 log2(speed), and for the Spiller slowed by thick fluids. */
    public int operationTime() {
        return Math.max(1, PowerRequirement.operationTime(22, 2, omega));
    }

    public int pending() {
        return targets.size();
    }

    /** Looks for the space to fill. Positions are returned lowest layer first, and nearest the machine first within a layer. */
    protected List<BlockPos> findSpace(ServerLevel server, BlockState placed) {
        int range = MachineConfig.get(MachineConfig.AREA_FILLER_RANGE);
        List<BlockPos> found = new ArrayList<>();
        if (range <= 0) {
            return found;
        }
        BlockPos start = worldPosition.below();
        if (!fillable(server, start, placed)) {
            return found;
        }
        int minY = server.getMinBuildHeight() + 1;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && found.size() < SEARCH_LIMIT) {
            BlockPos at = queue.poll();
            found.add(at);
            for (Direction d : Direction.values()) {
                BlockPos next = at.relative(d);
                if (seen.contains(next) || Math.abs(next.getX() - worldPosition.getX()) > range || Math.abs(next.getZ() - worldPosition.getZ()) > range
                        || next.getY() >= worldPosition.getY() || next.getY() < minY || !server.isLoaded(next)) {
                    continue;
                }
                seen.add(next);
                if (fillable(server, next, placed)) {
                    queue.add(next);
                }
            }
        }
        BlockPos origin = worldPosition;
        found.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingDouble(p -> p.distSqr(origin)));
        return found;
    }

    /** Whether the space at {@code pos} can be filled: empty, or the fluid being replaced. */
    private boolean fillable(ServerLevel server, BlockPos pos, BlockState placed) {
        BlockState state = server.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (!isFluidBlock(state)) {
            return false;
        }
        return allowFluidOverwrite() || placed != null && state.getBlock() == placed.getBlock() && !state.getFluidState().isSource();
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !MachineConfig.enabled(switchName)) {
            return;
        }
        tickCount++;
        if (!powered || !hasRemainingBlocks()) {
            return;
        }
        BlockState next = nextBlock();
        if (next == null || getPower() < requiredPower()) {
            return;
        }
        if (targets.isEmpty() && tickCount >= operationTime()) {
            targets.addAll(findSpace(server, next));
        }
        if (tickCount >= operationTime() && !targets.isEmpty()) {
            tickCount = 0;
            BlockPos at = targets.remove(0);
            BlockState there = server.getBlockState(at);
            if (!(there.isAir() || isFluidBlock(there)) || !MachineGuard.mayChange(server, at, owner)) {
                return;
            }
            server.setBlock(at, next, 3);
            server.playSound(null, at, isFluidBlock(next) ? SoundEvents.BUCKET_EMPTY : next.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1F, 1F);
            onBlockPlaced();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ticks", tickCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tickCount = tag.getInt("ticks");
    }
}
