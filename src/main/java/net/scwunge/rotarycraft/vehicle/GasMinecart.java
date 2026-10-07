package net.scwunge.rotarycraft.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.registry.GadgetRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;

/**
 * The Ethanol Minecart (EntityGasMinecart): a furnace minecart that burns ethanol crystals instead of coal, 15 seconds each, and runs faster. Use a crystal on it,
 * and push it in the direction you want it to go.
 */
public class GasMinecart extends AbstractMinecart {
    /** Ticks of running one ethanol crystal gives (a gas engine's fuel unit, 12 ticks, times 25). */
    public static final int CRYSTAL_TICKS = 300;
    private static final int MAX_FUEL = 32000;
    private static final EntityDataAccessor<Boolean> DATA_FUEL = SynchedEntityData.defineId(GasMinecart.class, EntityDataSerializers.BOOLEAN);

    private int fuel;
    public double xPush;
    public double zPush;

    public GasMinecart(EntityType<? extends GasMinecart> type, Level level) {
        super(type, level);
    }

    public GasMinecart(Level level, double x, double y, double z) {
        super(GadgetRegistry.ETHANOL_MINECART_ENTITY.get(), level, x, y, z);
    }

    @Override
    public Type getMinecartType() {
        return Type.FURNACE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUEL, false);
    }

    public int fuel() {
        return fuel;
    }

    public void addFuel(int ticks) {
        fuel = Math.min(MAX_FUEL, fuel + ticks);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (fuel > 0) {
                fuel--;
            }
            if (fuel <= 0) {
                xPush = 0;
                zPush = 0;
            }
            entityData.set(DATA_FUEL, fuel > 0);
        }
        if (entityData.get(DATA_FUEL) && random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.8, getZ(), 0, 0, 0);
        }
    }

    @Override
    protected double getMaxSpeed() {
        return (isInWater() ? 4.0 : 6.0) / 20.0;
    }

    @Override
    public float getMaxCartSpeedOnRail() {
        return 0.3F;
    }

    @Override
    protected Item getDropItem() {
        return GadgetRegistry.ETHANOL_MINECART.get();
    }

    @Override
    protected void moveAlongTrack(BlockPos pos, BlockState state) {
        super.moveAlongTrack(pos, state);
        Vec3 motion = getDeltaMovement();
        double along = motion.horizontalDistanceSqr();
        double push = xPush * xPush + zPush * zPush;
        if (push > 1.0E-4 && along > 0.001) {
            double speed = Math.sqrt(along);
            double length = Math.sqrt(push);
            xPush = motion.x / speed * length;
            zPush = motion.z / speed * length;
        }
    }

    @Override
    protected void applyNaturalSlowdown() {
        double push = xPush * xPush + zPush * zPush;
        if (push > 1.0E-7) {
            push = Math.sqrt(push);
            xPush /= push;
            zPush /= push;
            Vec3 motion = getDeltaMovement().multiply(0.8, 0, 0.8).add(xPush, 0, zPush);
            if (isInWater()) {
                motion = motion.scale(0.1);
            }
            setDeltaMovement(motion);
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.98, 0, 0.98));
        }
        super.applyNaturalSlowdown();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        InteractionResult result = super.interact(player, hand);
        if (result.consumesAction()) {
            return result;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(RotaryItems.ETHANOL_CRYSTALS.get()) && fuel + CRYSTAL_TICKS <= MAX_FUEL) {
            held.consume(1, player);
            fuel += CRYSTAL_TICKS;
        }
        if (fuel > 0) {
            xPush = getX() - player.getX();
            zPush = getZ() - player.getZ();
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("PushX", xPush);
        tag.putDouble("PushZ", zPush);
        tag.putShort("Fuel", (short) fuel);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        xPush = tag.getDouble("PushX");
        zPush = tag.getDouble("PushZ");
        fuel = tag.getShort("Fuel");
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return RotaryBlocks.BLAST_FURNACE.get().defaultBlockState();
    }
}
