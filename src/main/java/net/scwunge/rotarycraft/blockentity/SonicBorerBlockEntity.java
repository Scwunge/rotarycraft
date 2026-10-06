package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sonic Borer, as the original: it builds up air pressure from the shaft power (power / 65536 each tick, against the air leaking away),
 * and each time it holds 4 atm it fires a pulse in the direction it faces. The pulse flies 2 blocks a tick to the first layer of the world
 * ahead that is not empty and shatters the 7 by 7 square of blocks there, dropping them; creatures near the impact are bruised. It will not
 * fire if there is anything unbreakable or liquid in any layer before that, or into the bottom of the world. Past 10 atm it blows itself apart.
 * (The pulse is carried by the machine, not by an entity, so it shows as a trail of shock particles.) Acts as its owner: claims and mobGriefing
 * keep it from breaking blocks, and the overpressure blast only breaks blocks where weaponBlockDamage allows.
 */
public class SonicBorerBlockEntity extends ConsumerBlockEntity implements Owned {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(4096, 1, 65536);
    public static final int FIRE_PRESSURE = 400;
    public static final int MAX_PRESSURE = 1000;
    public static final int FOV = 3;
    private static final int PULSE_SPEED = 2;
    private static final int RETRY_TICKS = 10;

    private int pressure;
    private final List<int[]> pulses = new ArrayList<>(); // {distance flown, range}
    @Nullable
    private WorldGuard.Owner owner;

    public SonicBorerBlockEntity(BlockPos pos, BlockState state) {
        super(WorldMachineRegistry.SONIC_BORER_BE.get(), pos, state);
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

    public int pressure() {
        return pressure;
    }

    public void addPressure(int amount) {
        pressure += amount;
        setChanged();
    }

    /** The most blocks ahead a pulse will look. */
    public static int maxRange() {
        return Math.max(RotaryConfig.get(RotaryConfig.SONIC_BORER_RANGE), 64);
    }

    /** Whether a block can be shattered by a pulse. */
    public static boolean canDrop(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        return state.getDestroySpeed(level, pos) >= 0 && !(state.getBlock() instanceof LiquidBlock) && state.getFluidState().isEmpty();
    }

    /** Blocks out to the first layer with something in it, -1 if a layer on the way has something that cannot be broken, or it is not loaded. */
    public int distanceToSurface(Level level) {
        Direction dir = facing();
        for (int m = 1; m < maxRange(); m++) {
            BlockPos centre = worldPosition.relative(dir, m);
            if (!level.hasChunkAt(centre)) {
                return -1;
            }
            boolean solid = false;
            for (BlockPos pos : layer(centre, dir)) {
                if (!level.hasChunkAt(pos) || !canDrop(level, pos)) {
                    return -1;
                }
                if (!level.getBlockState(pos).isAir()) {
                    solid = true;
                }
            }
            if (solid) {
                return m;
            }
        }
        return maxRange();
    }

    /** The 7 by 7 square of blocks across the direction of fire, centred on {@code centre}. */
    private static List<BlockPos> layer(BlockPos centre, Direction dir) {
        List<BlockPos> out = new ArrayList<>();
        for (int i = -FOV; i <= FOV; i++) {
            for (int j = -FOV; j <= FOV; j++) {
                out.add(switch (dir.getAxis()) {
                    case X -> centre.offset(0, j, i);
                    case Z -> centre.offset(i, j, 0);
                    default -> centre.offset(i, 0, j);
                });
            }
        }
        return out;
    }

    // ---- the work ----

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !RotaryConfig.diggerEnabled("sonicBorer")) {
            return;
        }
        updatePressure(server, powered);
        flyPulses(server);
        boolean ready = powered && pressure >= FIRE_PRESSURE;
        if (ready && (pulses.isEmpty() || server.getGameTime() % RETRY_TICKS == 0)) {
            int range = distanceToSurface(server);
            boolean intoTheVoid = facing() == Direction.DOWN && worldPosition.getY() - Math.max(range, 0) <= server.getMinBuildHeight();
            if (range >= 0 && !intoTheVoid) {
                pulses.add(new int[] {0, range});
                pressure -= FIRE_PRESSURE;
                server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1);
                setChanged();
            }
        }
        if (pressure > MAX_PRESSURE) {
            overpressure(server);
        }
    }

    private void updatePressure(ServerLevel server, boolean powered) {
        int ambient = server.dimensionType().ultraWarm() ? 2000 : 101;
        int excess = pressure - ambient;
        pressure -= excess > 0 ? excess / 384 + 1 : -1;
        if (powered) {
            pressure += (int) Math.min(Integer.MAX_VALUE / 2, getPower() / 65536);
        }
    }

    private void flyPulses(ServerLevel server) {
        if (pulses.isEmpty()) {
            return;
        }
        Direction dir = facing();
        List<int[]> done = new ArrayList<>();
        for (int[] pulse : pulses) {
            pulse[0] += PULSE_SPEED;
            int at = Math.min(pulse[0], pulse[1]);
            BlockPos pos = worldPosition.relative(dir, Math.max(1, at));
            server.sendParticles(ParticleTypes.SONIC_BOOM, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            if (pulse[0] >= pulse[1]) {
                impact(server, worldPosition.relative(dir, pulse[1]));
                done.add(pulse);
            }
        }
        pulses.removeAll(done);
    }

    private void impact(ServerLevel server, BlockPos centre) {
        Direction dir = facing();
        ItemStack tool = ItemStack.EMPTY;
        for (BlockPos pos : layer(centre, dir)) {
            if (!server.hasChunkAt(pos) || pos.getY() <= server.getMinBuildHeight()) {
                continue;
            }
            BlockState state = server.getBlockState(pos);
            if (state.isAir() || !canDrop(server, pos) || !WorldGuard.mayChange(server, pos, owner)) {
                continue;
            }
            BlockEntity be = server.getBlockEntity(pos);
            if (be instanceof PowerBlockEntity) {
                continue;
            }
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
                items.addAll(Block.getDrops(state, server, pos, be, WorldGuard.actor(server, owner), tool));
            }
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    server.addFreshEntity(new ItemEntity(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
                }
            }
        }
        server.playSound(null, centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1, 1);
        server.sendParticles(ParticleTypes.EXPLOSION, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5, 1, 0, 0, 0, 0);
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(centre).inflate(3))) {
            e.hurt(server.damageSources().inWall(), 1);
        }
    }

    /** Past 10 atm it bursts, with a blast on each side (as the original) where block damage is allowed. */
    private void overpressure(ServerLevel server) {
        pressure = 0;
        Level.ExplosionInteraction mode = RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE) && server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)
                ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        for (double[] offset : new double[][] {{0, 0, 0}, {0, 1, 0}, {0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}}) {
            server.explode(null, x + offset[0], y + offset[1], z + offset[2], 4, mode);
        }
        setChanged();
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("pressure", pressure);
        ListTag list = new ListTag();
        for (int[] pulse : pulses) {
            CompoundTag p = new CompoundTag();
            p.putInt("flown", pulse[0]);
            p.putInt("range", pulse[1]);
            list.add(p);
        }
        tag.put("pulses", list);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pressure = tag.getInt("pressure");
        pulses.clear();
        ListTag list = tag.getList("pulses", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            pulses.add(new int[] {list.getCompound(i).getInt("flown"), list.getCompound(i).getInt("range")});
        }
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }
}
