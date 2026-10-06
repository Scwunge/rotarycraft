package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * A rail gun slug, as the original: it flies straight and fast and, on hitting a creature or a solid block (or after four seconds),
 * smashes the blocks round the impact by its tier (plants first, then glass, wood, sand and gravel, then stone and dirt, and
 * at the top tiers anything breakable) and strikes every creature within six blocks.
 */
public class RailgunShot extends Entity {
    private int power;
    @Nullable
    private WorldGuard.Owner owner;
    @Nullable
    private BlockPos gun;

    public RailgunShot(EntityType<? extends RailgunShot> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public RailgunShot(Level level, double x, double y, double z, Vec3 velocity, int power, BlockPos gun, @Nullable WorldGuard.Owner owner) {
        this(WeaponRegistry.RAILGUN_SHOT.get(), level);
        setPos(x, y, z);
        setDeltaMovement(velocity);
        this.power = power;
        this.gun = gun;
        this.owner = owner;
    }

    public int power() {
        return power;
    }

    /** The original's damage: 1 + tier + 4^tier / 16384; the top tier kills anything. */
    public float attackDamage() {
        if (power == 15) {
            return Float.MAX_VALUE;
        }
        return (int) (1 + power + Math.pow(4, power) / 16384D);
    }

    @Override
    public void tick() {
        if (level().isClientSide) {
            super.tick();
            setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
            return;
        }
        tickCount++;
        ServerLevel level = (ServerLevel) level();
        BlockPos at = blockPosition();
        BlockState state = level.getBlockState(at);
        boolean mobs = !level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1)).isEmpty();
        boolean solid = !state.isAir() && !soft(state) && !state.is(WeaponRegistry.RAILGUN.get());
        if (!state.isAir() && soft(state) && state.getFluidState().isEmpty() && blockDamage()) {
            breakConnected(level, at, state.getBlock(), 4);
        }
        if (mobs || solid) {
            impact();
            return;
        }
        level.explode(this, getX(), getY(), getZ(), 0, Level.ExplosionInteraction.NONE);
        if (gun != null && level.isLoaded(gun) && !level.getBlockState(gun).is(WeaponRegistry.RAILGUN.get()) || !level.isLoaded(at)) {
            discard();
            return;
        }
        if (tickCount > 80) {
            impact();
            return;
        }
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) {
            to = blockHit.getLocation();
        }
        Entity hit = null;
        double nearest = 0;
        for (Entity e : level.getEntities(this, getBoundingBox().expandTowards(getDeltaMovement()).inflate(1))) {
            if (!e.isPickable()) {
                continue;
            }
            var clip = e.getBoundingBox().inflate(0.3).clip(from, to);
            if (clip.isPresent()) {
                double d = from.distanceTo(clip.get());
                if (hit == null || d < nearest) {
                    hit = e;
                    nearest = d;
                }
            }
        }
        if (hit != null || blockHit.getType() != HitResult.Type.MISS) {
            if (blockHit.getType() == HitResult.Type.BLOCK && hit == null && level.getBlockState(blockHit.getBlockPos()).is(WeaponRegistry.RAILGUN.get())) {
                discard();
                return;
            }
            impact();
            return;
        }
        setPos(to);
        if (isInWater()) {
            level.explode(this, getX(), getY(), getZ(), 3, Level.ExplosionInteraction.NONE);
            for (int i = 0; i < 4; i++) {
                level.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
            }
        }
    }

    private static boolean soft(BlockState state) {
        return state.canBeReplaced();
    }

    private static boolean blockDamage() {
        return RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE) && RotaryConfig.get(RotaryConfig.RAILGUN_BLOCK_DAMAGE);
    }

    private void impact() {
        ServerLevel level = (ServerLevel) level();
        BlockPos o = blockPosition();
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        if (blockDamage()) {
            for (int i = -3; i <= 3; i++) {
                for (int j = -3; j <= 3; j++) {
                    for (int k = -3; k <= 3; k++) {
                        if (i * j * k < 9 && i * j * k > -9) {
                            smash(level, o.offset(i, j, k));
                        }
                    }
                }
            }
        }
        for (Entity e : level.getEntities(this, new AABB(position(), position()).inflate(6))) {
            if (e instanceof LivingEntity living) {
                strike(living);
            } else if (e instanceof EndCrystal || e instanceof HangingEntity) {
                e.hurt(damageSource(), attackDamage());
            }
        }
        for (int m = 0; m < 20; m++) {
            level.sendParticles(ParticleTypes.LAVA, getX() - 3 + 6 * random.nextFloat(), getY() - 3 + 6 * random.nextFloat(), getZ() - 3 + 6 * random.nextFloat(), 1, 0, 0, 0, 0);
        }
        discard();
    }

    /** What the impact does to one block, by tier (the original's rules; its hit counters on stone and dirt become chances). */
    private void smash(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(WeaponRegistry.RAILGUN.get())) {
            return;
        }
        if (soft(state) && !state.getFluidState().isSource()) {
            breakConnected(level, pos, state.getBlock(), 5);
            return;
        }
        if (power >= 1 && (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.LILY_PAD)
                || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM) || state.is(BlockTags.SAPLINGS) || state.is(Blocks.COBWEB)
                || state.is(Blocks.CACTUS) || state.is(BlockTags.FLOWER_POTS))
                || power >= 2 && (state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES) || state.is(Blocks.GLOWSTONE)
                || state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.LADDER) || state.is(BlockTags.SIGNS))
                || power >= 3 && (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.WOOL)
                || state.is(Blocks.CRAFTING_TABLE) || state.is(BlockTags.WOODEN_DOORS) || state.is(Blocks.NETHERRACK))
                || power >= 4 && (state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SOUL_SAND))) {
            breakConnected(level, pos, state.getBlock(), 5);
            return;
        }
        if (power >= 3 && state.is(Blocks.GRASS_BLOCK)) {
            if (power >= 5) {
                WorldGuard.breakBlock(level, pos, owner, true);
            } else {
                WorldGuard.setBlock(level, pos, Blocks.DIRT.defaultBlockState(), owner);
            }
            return;
        }
        if (power >= 3 && state.is(Blocks.DIRT)) {
            if (power > 9 || random.nextInt(4) == 0) {
                WorldGuard.breakBlock(level, pos, owner, true);
            }
            return;
        }
        if (power >= 5 && state.is(Blocks.STONE)) {
            if (power > 10 || random.nextInt(3) == 0) {
                if (power <= 12) {
                    WorldGuard.setBlock(level, pos, Blocks.COBBLESTONE.defaultBlockState(), owner);
                } else {
                    WorldGuard.breakBlock(level, pos, owner, true);
                }
            }
            return;
        }
        if (power >= 5 && (state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLESTONE_WALL) || state.is(Blocks.MOSSY_COBBLESTONE))) {
            if (power > 11 || random.nextInt(4) == 0) {
                WorldGuard.breakBlock(level, pos, owner, true);
            }
            return;
        }
        if (power >= 14 && state.getFluidState().isEmpty() && state.getDestroySpeed(level, pos) >= 0) {
            breakConnected(level, pos, state.getBlock(), power == 15 ? 6 : 3);
        }
    }

    /** Breaks the blocks of one kind joined to {@code start} within {@code radius} of it, as the original's recursive break. */
    private void breakConnected(ServerLevel level, BlockPos start, Block block, int radius) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(start);
        while (!open.isEmpty() && seen.size() < 4096) {
            BlockPos pos = open.poll();
            if (!seen.add(pos) || pos.distSqr(start) > radius * radius || !level.getBlockState(pos).is(block)) {
                continue;
            }
            if (!WorldGuard.breakBlock(level, pos, owner, true)) {
                continue;
            }
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                open.add(pos.relative(d));
            }
        }
    }

    private DamageSource damageSource() {
        return new DamageSource(level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(WeaponRegistry.RAILGUN_DAMAGE), this);
    }

    /** Strips a creature's effects (and a mob's gear, flung away), hurts it and knocks it along the shot's path. */
    private void strike(LivingEntity e) {
        e.removeAllEffects();
        if (!(e instanceof Player)) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack held = e.getItemBySlot(slot);
                if (!held.isEmpty()) {
                    e.setItemSlot(slot, ItemStack.EMPTY);
                    ItemEntity item = new ItemEntity(level(), e.getX(), e.getY(), e.getZ(), held);
                    item.setDeltaMovement((random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.4);
                    item.setPickUpDelay(300);
                    level().addFreshEntity(item);
                }
            }
        }
        if (e instanceof EnderDragon dragon) {
            dragon.hurt(dragon.head, damageSource(), attackDamage());
        } else {
            e.hurt(damageSource(), attackDamage());
        }
        e.setDeltaMovement(getDeltaMovement().scale(power / 15D));
        e.hurtMarked = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
