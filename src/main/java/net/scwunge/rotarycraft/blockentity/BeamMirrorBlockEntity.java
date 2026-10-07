package net.scwunge.rotarycraft.blockentity;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.MachineGuard;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * Beam Mirror (TileEntityBeamMirror): a mirror that follows the sun and throws daylight along the way it faces, a line of light as far as the sun is
 * strong (two to the power of seven times the sun's strength, up to the flood light's range) or the first opaque block, which it only does with open sky
 * above it, and not at night. Undead in the beam catch fire. It needs no power. Light and fire both stop with the switch.
 */
public class BeamMirrorBlockEntity extends ConsumerBlockEntity implements Owned, MachineInteractions {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(0, 0, 0);

    private final LongArrayList light = new LongArrayList();
    private int lastRange = -1;
    @Nullable
    private WorldGuard.Owner owner;

    public BeamMirrorBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.BEAM_MIRROR.type().get(), pos, state);
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

    public int lightBlocks() {
        return light.size();
    }

    public static int maxRange() {
        return Math.max(64, MachineConfig.get(MachineConfig.FLOODLIGHT_RANGE));
    }

    /** How strong the sun is, 0 to 1: the world's own sky darkening (it counts rain, thunder and the time), full in the middle of the day. */
    public static double sunStrength(Level level) {
        return Mth.clamp((11 - level.getSkyDarken()) / 11.0, 0, 1);
    }

    /** The mirror's tilt for the sun at this time of day, in degrees: half the sun's angle above the horizon, and twelve and a half. */
    public static float tilt(Level level, float partialTick) {
        float t = level.getTimeOfDay(partialTick);
        if (t > 0.5F) {
            t -= 1;
        }
        float sunAngle = Mth.clamp(90 + 360 * t, 0, 180);
        return sunAngle / 2 + 12.5F;
    }

    /** How far the sun carries the beam before anything gets in its way: nothing with no sky above or at night, else two to the power of seven times the sun's strength. */
    public static int reach(boolean sky, long dayTime, double sun, int max) {
        long time = dayTime % 24000;
        if (!sky || time > 13500 && time < 22500) {
            return 0;
        }
        return (int) Math.min(Math.pow(2, 7 * sun), max);
    }

    /** The length of the beam now, in blocks. */
    public int range() {
        if (level == null) {
            return 0;
        }
        int reach = reach(level.canSeeSky(worldPosition.above()), level.getDayTime(), sunStrength(level), maxRange());
        if (reach == 0) {
            return 0;
        }
        Direction dir = facing();
        for (int i = 1; i < reach; i++) {
            BlockPos at = worldPosition.relative(dir, i);
            if (!level.isLoaded(at)) {
                return i;
            }
            BlockState state = level.getBlockState(at);
            if (!state.isAir() && state.isSolidRender(level, at)) {
                return i;
            }
        }
        return reach;
    }

    private void rebuild(ServerLevel server, int range) {
        light.clear();
        if (range <= 0) {
            return;
        }
        Direction dir = facing();
        for (int i = 1; i <= range; i++) {
            BlockPos at = worldPosition.relative(dir, i);
            BlockState state = server.getBlockState(at);
            if (!state.isAir() && !state.is(Blocks.LIGHT)) {
                break;
            }
            if (server.isLoaded(at) && MachineGuard.mayChange(server, at, owner)) {
                light.add(at.asLong());
            }
        }
    }

    /** Puts out the light of its own. */
    public void lightsOut(ServerLevel server) {
        for (long packed : light) {
            BlockPos at = BlockPos.of(packed);
            if (server.isLoaded(at) && server.getBlockState(at).is(Blocks.LIGHT)) {
                server.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        light.clear();
        lastRange = -1;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!MachineConfig.enabled("beamMirror")) {
            if (!light.isEmpty()) {
                lightsOut(server);
            }
            return;
        }
        int range = range();
        if (range != lastRange) {
            lightsOut(server);
            rebuild(server, range);
            lastRange = range;
        }
        BlockState lit = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
        for (long packed : light) {
            BlockPos at = BlockPos.of(packed);
            if (server.isLoaded(at) && server.getBlockState(at).isAir()) {
                server.setBlock(at, lit, 2);
            }
        }
        if (range > 0) {
            Direction dir = facing();
            BlockPos far = worldPosition.relative(dir, range);
            AABB beam = new AABB(worldPosition).minmax(new AABB(far));
            for (LivingEntity mob : server.getEntitiesOfClass(LivingEntity.class, beam)) {
                if (mob.getType().is(EntityTypeTags.UNDEAD) && !mob.fireImmune()) {
                    mob.setRemainingFireTicks(200);
                }
            }
        }
    }

    @Override
    public void onBroken(ServerLevel server) {
        lightsOut(server);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray("light", light.toLongArray());
        if (owner != null) {
            tag.putUUID("owner_id", owner.id());
            tag.putString("owner_name", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        light.clear();
        for (long l : tag.getLongArray("light")) {
            light.add(l);
        }
        lastRange = -1;
        owner = tag.hasUUID("owner_id") ? new WorldGuard.Owner(tag.getUUID("owner_id"), tag.getString("owner_name")) : null;
    }
}
