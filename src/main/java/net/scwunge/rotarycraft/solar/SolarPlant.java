package net.scwunge.rotarycraft.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.registry.SolarRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A solar power plant, as the original's: every Solar Tower block and Solar Mirror joined face to face is one plant. Its towers are the columns of
 * tower blocks (the taller, the better; tower blocks at or below a mirror's height count for nothing), its mirrors each aim at the nearest tower,
 * and the plant's output grows with the number of mirrors that can see the sky, the light, and the towers' effective height. The plant is
 * found again whenever a block of it is placed or broken (the blocks hold on to their plant and let go of it when any of them is removed).
 */
public final class SolarPlant {
    /** Most blocks one plant is searched out to (a very large field would otherwise be walked on every change). */
    public static final int MAX_BLOCKS = 8192;
    public static final int MAX_TOWER_HEIGHT = 32;
    public static final int MAX_TOWER_VALUE = 96;
    private static final float TOWER_FALLOFF = 0.72f;
    private static final TreeMap<Integer, Integer> TOWER_ROUNDING = new TreeMap<>();

    static {
        for (int i = 0; i <= 6; i++) {
            TOWER_ROUNDING.put(i, i);
        }
        for (int i = 8; i <= 12; i += 2) {
            TOWER_ROUNDING.put(i, i);
        }
        for (int i = 16; i <= 24; i += 4) {
            TOWER_ROUNDING.put(i, i);
        }
        TOWER_ROUNDING.put(MAX_TOWER_HEIGHT, MAX_TOWER_HEIGHT);
    }

    /** One column of tower blocks. */
    public record Tower(int x, int z, int effectiveHeight, int bottom, int top) {
        public BlockPos topPos() {
            return new BlockPos(x, top, z);
        }

        long key() {
            return BlockPos.asLong(x, 0, z);
        }
    }

    private final List<Tower> towers = new ArrayList<>();
    private final Map<BlockPos, Tower> mirrors = new HashMap<>();
    private final Map<Long, Integer> highestMirror = new HashMap<>();
    private final Set<BlockPos> members = new HashSet<>();

    private SolarPlant() {}

    /** Walks out from {@code start} over every joined plant block, tells each of them it belongs to the plant, and measures it. */
    public static SolarPlant build(Level level, BlockPos start) {
        SolarPlant plant = new SolarPlant();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        List<BlockPos> mirrorPositions = new ArrayList<>();
        Map<Long, int[]> columns = new HashMap<>();
        while (!queue.isEmpty() && plant.members.size() < MAX_BLOCKS) {
            BlockPos pos = queue.poll();
            if (!level.isLoaded(pos)) {
                continue;
            }
            boolean tower = level.getBlockState(pos).is(SolarRegistry.SOLAR_TOWER.get());
            boolean mirror = level.getBlockState(pos).is(SolarRegistry.SOLAR_MIRROR.get());
            if (!tower && !mirror) {
                continue;
            }
            plant.members.add(pos);
            if (level.getBlockEntity(pos) instanceof SolarPlantMember member) {
                member.setPlant(plant);
            }
            if (mirror) {
                mirrorPositions.add(pos);
            } else {
                int[] ys = columns.computeIfAbsent(BlockPos.asLong(pos.getX(), 0, pos.getZ()), k -> new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE});
                ys[0] = Math.min(ys[0], pos.getY());
                ys[1] = Math.max(ys[1], pos.getY());
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        for (Map.Entry<Long, int[]> column : columns.entrySet()) {
            BlockPos at = BlockPos.of(column.getKey());
            int[] ys = column.getValue();
            int dy = ys[0];
            int height = 0;
            while (level.getBlockState(new BlockPos(at.getX(), dy, at.getZ())).is(SolarRegistry.SOLAR_TOWER.get()) && dy <= ys[0] + MAX_TOWER_HEIGHT) {
                dy++;
                if (mirrorBeside(level, new BlockPos(at.getX(), dy, at.getZ()))) {
                    height = 0;
                }
                height++;
            }
            plant.towers.add(new Tower(at.getX(), at.getZ(), height, ys[0], ys[1]));
        }
        plant.towers.sort(Comparator.comparingInt(Tower::effectiveHeight).reversed().thenComparingLong(Tower::key));
        for (BlockPos mirror : mirrorPositions) {
            Tower closest = closestTower(plant.towers, mirror);
            if (closest != null) {
                plant.mirrors.put(mirror, closest);
            }
            plant.highestMirror.merge(BlockPos.asLong(mirror.getX(), 0, mirror.getZ()), mirror.getY(), Math::max);
        }
        return plant;
    }

    private static boolean mirrorBeside(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(SolarRegistry.SOLAR_MIRROR.get())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Tower closestTower(List<Tower> towers, BlockPos from) {
        Tower best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Tower tower : towers) {
            double d = from.distSqr(new BlockPos(tower.x(), from.getY(), tower.z()));
            if (d < distance) {
                distance = d;
                best = tower;
            }
        }
        return best;
    }

    public int towerCount() {
        return towers.size();
    }

    public int mirrorCount() {
        return mirrors.size();
    }

    /** Tower height with every further tower worth less, rounded down to the sizes the original allowed. */
    public int effectiveTowerBlocks() {
        float falloff = 1;
        int sum = 0;
        for (Tower tower : towers) {
            sum += (int) (tower.effectiveHeight() * falloff);
            falloff *= TOWER_FALLOFF;
        }
        Map.Entry<Integer, Integer> entry = TOWER_ROUNDING.floorEntry(sum);
        return entry == null ? 0 : entry.getValue();
    }

    public int towerMultiplier() {
        return Math.min(effectiveTowerBlocks(), MAX_TOWER_VALUE);
    }

    /** The column that makes the power: the tallest tower. */
    @Nullable
    public Tower primaryTower() {
        return towers.isEmpty() ? null : towers.get(0);
    }

    /** Where a mirror aims: the top of the tower it is nearest, or null. */
    @Nullable
    public BlockPos aimingPosition(BlockPos mirror) {
        Tower tower = mirrors.get(mirror);
        return tower == null ? null : tower.topPos();
    }

    /** Whether this mirror is the highest of its column (the ones under it are shaded). */
    public boolean isHighestMirror(BlockPos mirror) {
        Integer highest = highestMirror.get(BlockPos.asLong(mirror.getX(), 0, mirror.getZ()));
        return highest != null && mirror.getY() >= highest;
    }

    /** The fraction of the plant's mirrors that work, times the light, from 0 to 1. */
    public float overallBrightness(Level level) {
        if (mirrors.isEmpty()) {
            return 0;
        }
        float working = 0;
        for (BlockPos pos : mirrors.keySet()) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof SolarMirrorBlockEntity mirror && mirror.isFunctional()) {
                working++;
            }
        }
        return working / mirrors.size() * lightLevel(level) / 15f;
    }

    /** How strong the light is, 0 to 15: the sun by day, the moon (by its phase) at night, none under a ceiling. */
    public static float lightLevel(Level level) {
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) {
            return 0;
        }
        double height = Math.cos(level.getSunAngle(1));
        double intensity = Math.max(0, height) * (1 - 0.75 * level.getRainLevel(1));
        double sun = intensity * 0.8 + 0.2;
        if (sun > 0.21) {
            return (int) (15 * sun);
        }
        float phase = switch (level.getMoonPhase()) {
            case 0 -> 1f;
            case 1, 7 -> 0.8f;
            case 2, 6 -> 0.5f;
            case 3, 5 -> 0.2f;
            case 4 -> 0.05f;
            default -> 0f;
        };
        return 15 * 0.2f * phase;
    }

    /** Lets every block of the plant know it is no longer one, so each finds its plant again (because something was placed or broken). */
    public void invalidate(Level level) {
        for (BlockPos pos : members) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof SolarPlantMember member) {
                member.setPlant(null);
            }
        }
        towers.clear();
        mirrors.clear();
        members.clear();
    }

    /** Whether the block entity at {@code pos} counts as part of this plant. */
    public boolean contains(BlockPos pos) {
        return members.contains(pos);
    }

}
