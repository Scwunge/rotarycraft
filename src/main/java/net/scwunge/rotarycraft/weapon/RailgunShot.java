package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * A rail gun slug, as the original: on impact it smashes the blocks round it by its tier (plants first, then glass, wood, sand
 * and gravel, then stone and dirt, and at the top tiers anything breakable) and strikes every creature within six blocks. In
 * flight it clears soft blocks (plants, snow) it passes through and bursts in water.
 */
public class RailgunShot extends TurretShot {
    private int power;
    private boolean explosive;

    public RailgunShot(EntityType<? extends RailgunShot> type, Level level) {
        super(type, level);
    }

    public RailgunShot(Level level, double x, double y, double z, Vec3 velocity, int power, BlockPos gun, @Nullable WorldGuard.Owner owner) {
        this(WeaponRegistry.RAILGUN_SHOT.get(), level);
        launch(x, y, z, velocity, gun, owner);
        this.power = power;
    }

    /** A shell: blows up on impact. */
    public RailgunShot explosive() {
        explosive = true;
        return this;
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

    private static boolean blockDamage() {
        return RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE) && RotaryConfig.get(RotaryConfig.RAILGUN_BLOCK_DAMAGE);
    }

    @Override
    protected void flightTick(ServerLevel level, BlockPos at, BlockState state) {
        if (!state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty() && blockDamage()) {
            breakConnected(level, at, state.getBlock(), 4);
        }
        level.explode(this, getX(), getY(), getZ(), 0, Level.ExplosionInteraction.NONE);
    }

    @Override
    protected void inWater(ServerLevel level) {
        level.explode(this, getX(), getY(), getZ(), 3, Level.ExplosionInteraction.NONE);
        level.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 4, 0, 0, 0, 0);
    }

    @Override
    protected void impact(ServerLevel level) {
        if (explosive) {
            level.explode(this, getX(), getY(), getZ(), ExplosiveShellItem.EXPLOSION, true, blockDamage() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
            return;
        }
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
    }

    /** What the impact does to one block, by tier (the original's rules; its hit counters on stone and dirt become chances). */
    private void smash(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(WeaponRegistry.RAILGUN.get())) {
            return;
        }
        if (state.canBeReplaced() && !state.getFluidState().isSource()) {
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
            if (!seen.add(pos) || pos.distSqr(start) > radius * radius || !level.getBlockState(pos).is(block)
                    || !WorldGuard.breakBlock(level, pos, owner, true)) {
                continue;
            }
            for (Direction d : Direction.values()) {
                open.add(pos.relative(d));
            }
        }
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
}
