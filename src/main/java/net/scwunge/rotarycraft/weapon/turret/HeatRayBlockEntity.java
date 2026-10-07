package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.LaserBeam;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * Heat Ray, as the original: needs 2 MW and then burns along the way it faces, as far as 8 blocks plus one per 256 W of surplus
 * (at most 128), stopped by any opaque block. Every creature in the beam is set alight, for longer the more power it has. Blocks
 * in the beam are changed, each at its own pace (a block that is not ready yet holds the beam): plants and webs and water
 * vanish, wood and leaves catch fire, stone and cobblestone melt to lava, ice and snow to water, sand to glass, dirt to sand,
 * grass to dirt, gravel and moss to cobblestone, TNT is lit, and netherrack explodes. Block changes need weaponBlockDamage,
 * mobGriefing and the owner's permission.
 */
public class HeatRayBlockEntity extends ConsumerBlockEntity implements Owned {
    public static final long MIN_POWER = 2_097_152;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);
    /** One more block of range per this much power over the minimum. */
    public static final int FALLOFF = 256;
    public static final int MAX_RANGE = 128;

    private enum Effect {
        DELETE, IGNITE, MELT_LAVA, MELT_WATER, CHANGE, TNT, NETHERRACK
    }

    private int ticks;
    @Nullable
    private WorldGuard.Owner owner;

    public HeatRayBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.HEAT_RAY_BE.get(), pos, state);
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

    public int range() {
        long power = getPower();
        return (int) Math.min(net.scwunge.rotarycraft.config.RotaryConfig.get(net.scwunge.rotarycraft.config.RotaryConfig.HEAT_RAY_RANGE), 8L + (power - MIN_POWER) / FALLOFF);
    }

    /** Seconds of burning the beam gives a creature. */
    public int burnSeconds() {
        return 2 + (int) (16L * getPower() / MIN_POWER);
    }

    @Override
    protected void machineTick(boolean powered) {
        ticks++;
        if (!powered || getPower() < MIN_POWER || !RotaryConfig.weaponEnabled("heatRay")) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        Direction facing = facing();
        boolean blocks = RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE);
        int max = range();
        int step;
        boolean blocked = false;
        for (step = 1; step < max && !blocked; step++) {
            BlockPos pos = worldPosition.relative(facing, step);
            if (!server.isLoaded(pos)) {
                break;
            }
            BlockState state = server.getBlockState(pos);
            LaserBeam.Touch touch = state.isAir() ? LaserBeam.Touch.NONE : LaserBeam.touch(server, pos, state, getPower(), step);
            if (touch == LaserBeam.Touch.STOP) {
                blocked = true;
            }
            if (touch == LaserBeam.Touch.NONE && blocks && !state.isAir()) {
                if (state.isFlammable(server, pos, Direction.UP)) {
                    ignite(server, pos);
                }
                blocked = affect(server, pos, state, step);
            }
            state = server.getBlockState(pos);
            if (!state.isAir() && state.isSolidRender(server, pos)) {
                blocked = true;
            }
        }
        BlockPos end = worldPosition.relative(facing, Math.max(1, step - 1));
        AABB zone = new AABB(worldPosition.relative(facing)).minmax(new AABB(end)).inflate(0.25);
        for (Entity caught : server.getEntities((Entity) null, zone, e -> !(e instanceof ItemEntity))) {
            caught.igniteForSeconds(burnSeconds());
            LaserBeam.touch(server, caught, getPower(), step - 1);
        }
    }

    private static Effect effectFor(BlockState state) {
        Block b = state.getBlock();
        if (state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.CROPS) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.VINE) || state.is(Blocks.LILY_PAD) || state.is(Blocks.COBWEB) || state.is(Blocks.SNOW)
                || state.is(Blocks.PUMPKIN_STEM) || state.is(Blocks.MELON_STEM) || state.is(Blocks.ATTACHED_PUMPKIN_STEM) || state.is(Blocks.ATTACHED_MELON_STEM)
                || b instanceof LiquidBlock && state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
            return Effect.DELETE;
        }
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
            return Effect.IGNITE;
        }
        if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE) || state.is(Blocks.SANDSTONE) || state.is(Blocks.STONE_BRICKS)) {
            return Effect.MELT_LAVA;
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.SNOW_BLOCK)) {
            return Effect.MELT_WATER;
        }
        if (state.is(Blocks.GRAVEL) || state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)
                || state.is(Blocks.DIRT) || state.is(Blocks.FARMLAND) || state.is(BlockTags.SAND) && !state.is(Blocks.SANDSTONE)) {
            return Effect.CHANGE;
        }
        if (state.is(Blocks.TNT)) {
            return Effect.TNT;
        }
        if (state.is(Tags.Blocks.NETHERRACKS)) {
            return Effect.NETHERRACK;
        }
        return null;
    }

    private static BlockState changeTo(BlockState state) {
        if (state.is(Blocks.GRAVEL) || state.is(Blocks.MOSSY_COBBLESTONE)) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (state.is(Blocks.DIRT) || state.is(Blocks.FARMLAND)) {
            return Blocks.SAND.defaultBlockState();
        }
        return Blocks.GLASS.defaultBlockState();
    }

    /** Ticks a block must be in the beam before it is changed; the nearer and the more powered, the quicker. */
    private int delay(Effect effect, int distance) {
        long surplus = Math.max(1, getPower() / MIN_POWER);
        return switch (effect) {
            case DELETE, MELT_WATER -> (int) Math.min(Integer.MAX_VALUE, 4L * distance / (8 * surplus));
            case MELT_LAVA -> (int) Math.min(Integer.MAX_VALUE, 4L * distance / (2 * surplus));
            case CHANGE -> (int) Math.min(Integer.MAX_VALUE, 4L * distance / surplus);
            case IGNITE, TNT -> 0;
            case NETHERRACK -> 6;
        };
    }

    /** Applies the block's effect if there is one and it is ready; true if the beam is held (or stopped) here. */
    private boolean affect(ServerLevel server, BlockPos pos, BlockState state, int distance) {
        Effect effect = effectFor(state);
        if (effect == null) {
            return false;
        }
        if (ticks < delay(effect, distance)) {
            return true;
        }
        ticks = 0;
        switch (effect) {
            case DELETE -> {
                if (WorldGuard.setBlock(server, pos, Blocks.AIR.defaultBlockState(), owner)) {
                    server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f);
                }
                return false;
            }
            case IGNITE -> {
                ignite(server, pos);
                return false;
            }
            case MELT_LAVA -> {
                if (WorldGuard.setBlock(server, pos, Blocks.LAVA.defaultBlockState(), owner)) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
                    server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f);
                }
                return false;
            }
            case MELT_WATER -> {
                if (WorldGuard.setBlock(server, pos, Blocks.WATER.defaultBlockState(), owner)) {
                    server.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 2.6f);
                }
                return false;
            }
            case CHANGE -> {
                WorldGuard.setBlock(server, pos, changeTo(state), owner);
                return true;
            }
            case TNT -> {
                if (WorldGuard.setBlock(server, pos, Blocks.AIR.defaultBlockState(), owner)) {
                    server.addFreshEntity(new PrimedTnt(server, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, null));
                    server.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1, 1);
                }
                return false;
            }
            default -> {
                if (WorldGuard.mayChange(server, pos, owner)) {
                    server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, true, Level.ExplosionInteraction.BLOCK);
                }
                return true;
            }
        }
    }

    /** Sets fire to the block by lighting an open face of it (if the owner may). */
    private void ignite(ServerLevel server, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos at = pos.relative(d);
            if (server.getBlockState(at).isAir() && BaseFireBlock.canBePlacedAt(server, at, d.getOpposite())
                    && WorldGuard.setBlock(server, at, BaseFireBlock.getState(server, at), owner)) {
                return;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }
}
