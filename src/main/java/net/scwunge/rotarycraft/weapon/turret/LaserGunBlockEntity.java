package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.WorldGuard;

/**
 * Laser Gun, as the original: needs 8.4 MW and, while powered, fires a beam out to 256 blocks every tick along its barrel (it
 * also turns to follow hostile targets in reach of the beam). The beam sets creatures alight and hurts them (4 damage), strips
 * their freeze, and changes the first block it can: sand to glass, stone and cobblestone to lava, grass to dirt, dirt to sand,
 * gravel to cobblestone, plants to nothing, wood to fire, ice to water; netherrack explodes and TNT is lit. Any opaque block
 * stops it. Block changes need weaponBlockDamage, mobGriefing and the owner's permission.
 */
public class LaserGunBlockEntity extends TurretBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 8388608);
    public static final int MAX_RANGE = 256;
    private static final float DAMAGE = 4;
    private static final int BURN_SECONDS = 7;

    /** How far the beam reached last tick (it is stopped by blocks); synced for the beam's look. */
    private int range;

    public LaserGunBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.LASER_GUN_BE.get(), pos, state, "laserGun");
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    /** The turret can only pick targets the beam can currently reach, as the original. */
    @Override
    public int range() {
        return range;
    }

    public int beamLength() {
        return range;
    }

    private void setRange(int r) {
        if (r != range) {
            range = r;
            syncNow();
        }
    }

    @Override
    protected void unpoweredTick() {
        setRange(0);
    }

    @Override
    protected void turretTick() {
        setRange(MAX_RANGE);
        fire();
    }

    /** The beam's direction, from the barrel's aim. */
    public Vec3 direction() {
        double elevation = Math.toRadians(theta);
        double azimuth = Math.toRadians(90 - phi);
        return new Vec3(Math.cos(elevation) * Math.cos(azimuth), Math.sin(elevation), Math.cos(elevation) * Math.sin(azimuth));
    }

    public Vec3 origin() {
        return new Vec3(worldPosition.getX() + 0.5, firingY(0), worldPosition.getZ() + 0.5);
    }

    private void fire() {
        ServerLevel server = (ServerLevel) level;
        Vec3 from = origin();
        Vec3 d = direction();
        boolean blocks = RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE);
        int reach = MAX_RANGE;
        for (float i = 0; i <= MAX_RANGE; i += 0.5f) {
            BlockPos pos = BlockPos.containing(from.add(d.scale(i)));
            if (!server.isLoaded(pos)) {
                reach = (int) i;
                break;
            }
            BlockState state = server.getBlockState(pos);
            if (pos.equals(worldPosition)) {
                continue;
            }
            if (blocks && affect(server, pos, state)) {
                reach = (int) i + 1;
                break;
            }
            if (!state.isAir() && state.isSolidRender(server, pos)) {
                reach = (int) i + 1;
                break;
            }
        }
        setRange(reach);
        Vec3 to = from.add(d.scale(reach));
        for (Entity e : server.getEntities((Entity) null, new AABB(from, to).inflate(1), e -> e instanceof LivingEntity)) {
            AABB box = e.getBoundingBox().inflate(1);
            if (box.contains(from) || box.clip(from, to).isPresent()) {
                e.hurt(server.damageSources().onFire(), DAMAGE);
                e.igniteForSeconds(BURN_SECONDS);
                ((LivingEntity) e).removeEffect(WeaponRegistry.FREEZE);
            }
        }
    }

    /** Changes the block if the beam does anything to it; true if it did (and so stops here). */
    private boolean affect(ServerLevel server, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        BlockState to = null;
        if (state.is(BlockTags.SAND) && !state.is(Blocks.SANDSTONE)) {
            to = Blocks.GLASS.defaultBlockState();
        } else if (state.is(Blocks.STONE) || state.is(Blocks.STONE_BRICKS) || state.is(Blocks.SANDSTONE) || state.is(Blocks.COBBLESTONE)) {
            to = Blocks.LAVA.defaultBlockState();
        } else if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)) {
            to = Blocks.DIRT.defaultBlockState();
        } else if (state.is(Blocks.DIRT) || state.is(Blocks.FARMLAND)) {
            to = Blocks.SAND.defaultBlockState();
        } else if (state.is(Blocks.GRAVEL)) {
            to = Blocks.COBBLESTONE.defaultBlockState();
        } else if (state.is(BlockTags.REPLACEABLE_BY_TREES) && !state.is(BlockTags.LEAVES) || state.is(Blocks.COBWEB) || state.is(Blocks.SNOW)
                || state.is(BlockTags.FLOWERS) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.DEAD_BUSH)
                || state.is(BlockTags.CROPS) || state.is(Blocks.VINE) || state.is(Blocks.MELON_STEM) || state.is(Blocks.PUMPKIN_STEM)
                || state.is(Blocks.LILY_PAD) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)) {
            to = Blocks.AIR.defaultBlockState();
        } else if (state.isFlammable(server, pos, Direction.UP)) {
            to = Blocks.FIRE.defaultBlockState();
        } else if (state.is(Tags.Blocks.NETHERRACKS)) {
            if (WorldGuard.mayChange(server, pos, owner())) {
                server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, true, Level.ExplosionInteraction.BLOCK);
                return true;
            }
            return false;
        } else if (state.is(Blocks.TNT)) {
            if (WorldGuard.setBlock(server, pos, Blocks.AIR.defaultBlockState(), owner())) {
                PrimedTnt tnt = new PrimedTnt(server, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, null);
                server.addFreshEntity(tnt);
                return true;
            }
            return false;
        } else if (state.is(BlockTags.ICE)) {
            to = Blocks.WATER.defaultBlockState();
        }
        return to != null && !to.equals(state) && WorldGuard.setBlock(server, pos, to, owner());
    }

    @Override
    protected void writeSync(CompoundTag tag) {
        tag.putInt("range", range);
    }

    @Override
    protected void readSync(CompoundTag tag) {
        range = tag.getInt("range");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("range", range);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        range = tag.getInt("range");
    }
}
