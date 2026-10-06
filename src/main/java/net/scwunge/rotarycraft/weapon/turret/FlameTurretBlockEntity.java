package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.FlameShot;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Flame Turret, as the original: needs 512 rad/s and 32.8 kW, holds a bucket of fuel and every tick it has a target (at least six
 * blocks away) lobs a burst of flame, using 1 mB. Each fuel burns differently: damage, how long things burn, range and how long
 * the fires it lights last.
 */
public class FlameTurretBlockEntity extends TurretBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 512, 32768);
    public static final int CAPACITY = 1000;
    public static final int MIN_RANGE = 6;
    private static final int BASE_RANGE = 32;

    /** What each fuel does (the original's damage multipliers, burn seconds, range multiplier and fire block age). */
    public record Attack(float damageMultiplier, int burnSeconds, float rangeMultiplier, int fireAge) {
        public int damage() {
            return Math.round(5 * damageMultiplier);
        }
    }

    private static final int DEFAULT_FIRE_AGE = 4;
    /** Fuel fluid tags and their attacks. Oil burns hot and long but short-ranged; ethanol and jet fuel burn harder. */
    public static final Map<TagKey<Fluid>, Attack> ATTACKS = new LinkedHashMap<>();

    private static void fuel(String tag, float damage, int burn, float range, int fireAge) {
        ATTACKS.put(TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", tag)), new Attack(damage, burn, range, fireAge));
    }

    static {
        fuel("crude_oil", 0.75f, 6, 0.4f, 0);
        fuel("oil", 0.75f, 6, 0.4f, 0);
        fuel("diesel", 1, 3, 1, DEFAULT_FIRE_AGE);
        fuel("gasoline", 1, 3, 1, DEFAULT_FIRE_AGE);
        fuel("fuel", 1, 3, 1, DEFAULT_FIRE_AGE);
        fuel("ethanol", 1.2f, 4, 1, 8);
        fuel("bioethanol", 1.35f, 4, 1, 8);
        fuel("jet_fuel", 1.8f, 6, 1, DEFAULT_FIRE_AGE);
        fuel("rocket_fuel", 2, 10, 1, DEFAULT_FIRE_AGE);
    }

    @Nullable
    public static Attack attackFor(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return null;
        }
        for (Map.Entry<TagKey<Fluid>, Attack> e : ATTACKS.entrySet()) {
            if (fluid.is(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private final FluidTank tank = new FluidTank(CAPACITY, fs -> attackFor(fs) != null) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler intake = FluidAccess.fillOnly(tank);

    public FlameTurretBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.FLAME_TURRET_BE.get(), pos, state, "flameTurret");
    }

    public FluidTank tank() {
        return tank;
    }

    public IFluidHandler intake() {
        return intake;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public int range() {
        Attack a = attackFor(tank.getFluid());
        return a == null ? 0 : (int) (BASE_RANGE * a.rangeMultiplier());
    }

    @Override
    protected double minRange() {
        return MIN_RANGE;
    }

    @Override
    protected double thetaOffset() {
        return 20;
    }

    @Override
    public int operationTime() {
        return 0;
    }

    @Override
    protected void turretTick() {
        Attack attack = attackFor(tank.getFluid());
        if (target == null || attack == null) {
            return;
        }
        double dist = target.distanceTo(new Vec3(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()));
        double speed = 0.25 * (Math.pow(dist, 0.7) / 7D) * (1 + (level.random.nextDouble() * 2 - 1) * 0.125);
        tank.drain(1, IFluidHandler.FluidAction.EXECUTE);
        double elevation = Math.toRadians(theta + 20);
        double azimuth = Math.toRadians(90 - phi);
        Vec3 v = new Vec3(speed * Math.cos(elevation) * Math.cos(azimuth), speed * Math.sin(elevation), speed * Math.cos(elevation) * Math.sin(azimuth));
        level.addFreshEntity(new FlameShot(level, worldPosition.getX() + 0.5 + v.x, firingY(v.y), worldPosition.getZ() + 0.5 + v.z, v, worldPosition, owner(), attack));
        if (level.getGameTime() % 34 == 0) {
            level.playSound(null, worldPosition, WeaponRegistry.FLAME_TURRET_SOUND.get(), SoundSource.BLOCKS, 1, 1);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
    }
}
