package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.power.Shockable;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

import java.util.Comparator;
import java.util.List;

/**
 * Van de Graaff Generator, as in the original: any shaft power below it builds up charge (4 x the square root of the watts a
 * tick). It picks a neighbour to discharge into at random, weighted towards what conducts: a {@link Shockable} machine
 * (1000), TNT (100, which it lights), metal and anvils (50) and water (20). Failing that it zaps the nearest mob (never a
 * player) in range, and charge past 2 million makes it blow itself up in a lightning bolt. Rain halves its charge.
 */
public class VanDeGraffBlockEntity extends ConsumerBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(0, 0, 0);
    public static final int MAX_RANGE = 16;
    public static final int EXPLODE_CHARGE = 2097152;
    private static final Direction[] SIDES = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private int charge;

    public VanDeGraffBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.VAN_DE_GRAAFF.get(), pos, state);
    }

    /** The shaft comes in from below, whichever way it was placed. */
    @Override
    public Direction inputSide() {
        return Direction.DOWN;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public int charge() {
        return charge;
    }

    public void setCharge(int charge) {
        this.charge = charge;
        setChanged();
    }

    public int range() {
        return Math.min(charge / 1024, MAX_RANGE);
    }

    @Override
    protected void machineTick(boolean powered) {
        ServerLevel server = (ServerLevel) level;
        charge += (int) (4 * Math.sqrt((long) torque * omega));
        int r = range();
        if (r > 0) {
            Direction dir = pickSide();
            if (dir != null) {
                shock(dir);
                return;
            }
            for (int i = 2; i < 4; i++) {
                BlockPos up = worldPosition.above(i);
                if (level.getBlockEntity(up) instanceof Shockable s && s.canDischargeLongRange()) {
                    dischargeTo(up, s);
                    return;
                }
            }
        }
        if (charge <= 0) {
            return;
        }
        AABB box = new AABB(worldPosition).inflate(r);
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !(e instanceof Player));
        LivingEntity target = near.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(Vec3.atCenterOf(worldPosition)))).orElse(null);
        if (target != null) {
            Vec3 to = new Vec3(target.getX(), target.getY() + target.getEyeHeight() * 0.8, target.getZ());
            bolt(server, to);
            shock(target);
            charge = 0;
            setChanged();
        }
        if (charge > EXPLODE_CHARGE) {
            detonate(server);
            return;
        }
        if (level.isRainingAt(worldPosition.above())) {
            charge /= 2;
        }
    }

    /** The side to discharge into, picked by weight, or null for none (the "nowhere" entry weighs 1). */
    private Direction pickSide() {
        int[] weights = new int[SIDES.length];
        int total = 1;
        for (int i = 0; i < SIDES.length; i++) {
            weights[i] = weight(SIDES[i]);
            total += weights[i];
        }
        int roll = level.random.nextInt(total);
        for (int i = 0; i < SIDES.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return SIDES[i];
            }
        }
        return null;
    }

    private int weight(Direction d) {
        BlockPos p = worldPosition.relative(d);
        BlockState s = level.getBlockState(p);
        if (s.isAir() || s.is(getBlockState().getBlock())) {
            return 0;
        }
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof Shockable) {
            return 1000;
        }
        if (s.is(BlockTags.ANVIL) || s.getSoundType() == SoundType.METAL || s.is(Blocks.IRON_BLOCK)) {
            return 50;
        }
        if (s.getFluidState().is(FluidTags.WATER)) {
            return 20;
        }
        if (s.is(Blocks.TNT)) {
            return 100;
        }
        return 0;
    }

    private void shock(Direction dir) {
        BlockPos p = worldPosition.relative(dir);
        BlockState s = level.getBlockState(p);
        if (s.isAir()) {
            return;
        }
        dischargeTo(p, level.getBlockEntity(p) instanceof Shockable sh ? sh : null);
        if (s.is(Blocks.TNT)) {
            level.removeBlock(p, false);
            PrimedTnt tnt = new PrimedTnt(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, null);
            level.addFreshEntity(tnt);
            level.playSound(null, p, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1F, 1F);
        }
    }

    /** Fires the whole charge at a block, letting a Shockable one use it (and refuse a bolt that is too weak). */
    public void dischargeTo(BlockPos target, Shockable s) {
        double ax = 0.5;
        double ay = 0.5;
        double az = 0.5;
        if (s != null) {
            if (charge < s.getMinDischarge()) {
                return;
            }
            s.onDischarge(charge, Math.sqrt(worldPosition.distSqr(target)));
            ax = s.getAimX();
            ay = s.getAimY();
            az = s.getAimZ();
        }
        level.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.25F, 2F);
        bolt((ServerLevel) level, new Vec3(target.getX() + ax, target.getY() + ay, target.getZ() + az));
        charge = 0;
        setChanged();
    }

    /** Electric sparks along the line from the sphere to the target. */
    private void bolt(ServerLevel server, Vec3 to) {
        Vec3 from = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 0.75, worldPosition.getZ() + 0.5);
        Vec3 d = to.subtract(from);
        int steps = Math.max(2, (int) (d.length() * 3));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(d.scale(i / (double) steps));
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x + (level.random.nextDouble() - 0.5) * 0.2, p.y + (level.random.nextDouble() - 0.5) * 0.2,
                    p.z + (level.random.nextDouble() - 0.5) * 0.2, 1, 0, 0, 0, 0);
        }
        level.playSound(null, worldPosition, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.25F, 2F);
    }

    private void shock(LivingEntity e) {
        float dmg = 1 + (float) (Math.pow(charge, 2) / (4194304D * 8));
        if (wearsFull(e, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS)) {
            dmg = 0;
        } else if (wearsFull(e, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS)) {
            dmg /= 2;
        }
        if (dmg > 0) {
            e.hurt(level.damageSources().lightningBolt(), dmg);
            if (e instanceof Creeper) {
                level.explode(e, e.getX(), e.getY(), e.getZ(), 3F, Level.ExplosionInteraction.NONE);
                e.kill();
            }
        }
    }

    private static boolean wearsFull(LivingEntity e, net.minecraft.world.item.Item head, net.minecraft.world.item.Item chest,
                                     net.minecraft.world.item.Item legs, net.minecraft.world.item.Item feet) {
        return e.getItemBySlot(EquipmentSlot.HEAD).is(head) && e.getItemBySlot(EquipmentSlot.CHEST).is(chest)
                && e.getItemBySlot(EquipmentSlot.LEGS).is(legs) && e.getItemBySlot(EquipmentSlot.FEET).is(feet);
    }

    private void detonate(ServerLevel server) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(worldPosition));
            bolt.setVisualOnly(true);
            server.addFreshEntity(bolt);
        }
        charge = 0;
        BlockPos pos = worldPosition;
        server.removeBlock(pos, false);
        server.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4F, true,
                RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("charge", charge);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charge = tag.getInt("charge");
    }
}
