package net.scwunge.rotarycraft.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.api.SodiumSolarUpgrades;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.SolarRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * A Solar Tower block, as the original's. A column of them, with a field of mirrors joined to it, is a solar plant (see {@link SolarPlant}). Fed water
 * (or, with a working sodium receiver on top of the column, liquid sodium) it turns the light the mirrors gather into shaft power out of the
 * bottom of its column: 512 rad/s on water (4096 on sodium) at a torque that grows with the mirrors, the light and the tower's height.
 * The plant's tallest column makes the power; the others only add their height. The fluid is used up as it works (more the more power it makes).
 * Departures from the original: it no longer melts or burns the blocks above and around it, and no longer turns into lava past 1500 degrees;
 * what stands in the open above it is only set alight.
 */
public class SolarTowerBlockEntity extends BlockEntity implements SolarPlantMember, IShaftPowerOutput {
    public static final int TANK = 4000;
    public static final int GEN_OMEGA = 512;
    public static final int GEN_OMEGA_SODIUM = 4096;
    public static final int MAX_TORQUE = 16384;
    public static final int MAX_TORQUE_SODIUM = 65536;
    /** What to treat as liquid sodium: ReactorCraft's, Mekanism's, or anything a pack adds to this tag. */
    public static final TagKey<Fluid> SODIUM = TagKey.create(Registries.FLUID, net.scwunge.rotarycraft.RotaryCraft.id("solar_sodium"));
    private static final int RECOUNT_TICKS = 100;
    private static final int SYNC_TICKS = 20;

    @Nullable
    private SolarPlant plant;
    private final FluidTank tank = new FluidTank(TANK, this::accepts) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int size = -1;
    private float overallBrightness;
    private int topLocation = -1;
    private int temperature;
    private int currentConsumption;
    private int torque;
    private int omega;
    private long power;
    private int recountTimer;
    private int syncedTorque = -1;

    public SolarTowerBlockEntity(BlockPos pos, BlockState state) {
        super(SolarRegistry.SOLAR_TOWER_BE.get(), pos, state);
    }

    @Nullable
    @Override
    public SolarPlant plant() {
        return plant;
    }

    @Override
    public void setPlant(@Nullable SolarPlant plant) {
        this.plant = plant;
        size = -1;
    }

    @Override
    public void searchForPlant() {
        if (plant == null && level != null) {
            SolarPlant.build(level, worldPosition);
            size = -1;
        }
    }

    public FluidTank tank() {
        return tank;
    }

    public int temperature() {
        return temperature;
    }

    public int currentConsumption() {
        return currentConsumption;
    }

    public int getTorque() {
        return torque;
    }

    public int getOmega() {
        return omega;
    }

    public int arraySize() {
        return size;
    }

    public float brightness() {
        return overallBrightness;
    }

    /** What the tower may be filled with: water, or sodium when a working sodium receiver is on top of the column. */
    private boolean accepts(FluidStack stack) {
        if (stack.getFluid() == Fluids.WATER) {
            return true;
        }
        return stack.is(SODIUM) && canUseSodium();
    }

    private boolean canUseSodium() {
        if (level == null) {
            return false;
        }
        BlockPos above = new BlockPos(worldPosition.getX(), topOfTower() + 1, worldPosition.getZ());
        return level.getBlockEntity(above) instanceof SodiumSolarUpgrades.SodiumSolarReceiver receiver && receiver.isActive();
    }

    private boolean isTower(BlockPos pos) {
        return level != null && level.getBlockState(pos).is(SolarRegistry.SOLAR_TOWER.get());
    }

    /** The y of the highest tower block in this column (counting up from this block). */
    public int topOfTower() {
        int y = worldPosition.getY();
        while (isTower(new BlockPos(worldPosition.getX(), y, worldPosition.getZ()))) {
            y++;
        }
        return y - 1;
    }

    public boolean isTopOfTower() {
        return worldPosition.getY() == topOfTower();
    }

    @Nullable
    private SolarTowerBlockEntity towerAt(int y) {
        return level != null && level.getBlockEntity(new BlockPos(worldPosition.getX(), y, worldPosition.getZ())) instanceof SolarTowerBlockEntity t ? t : null;
    }

    // ---- the work ----

    public void serverTick() {
        if (!RotaryConfig.get(RotaryConfig.SOLAR_TOWER)) {
            torque = 0;
            omega = 0;
            power = 0;
            return;
        }
        ServerLevel server = (ServerLevel) level;
        searchForPlant();
        topLocation = topOfTower();
        boolean top = worldPosition.getY() == topLocation;
        int temp = (int) (5 * Math.max(0, size) * overallBrightness);
        if (top && temp > 400) {
            AABB above = new AABB(worldPosition.getX() - 3, worldPosition.getY() + 1, worldPosition.getZ() - 3,
                    worldPosition.getX() + 4, worldPosition.getY() + 2, worldPosition.getZ() + 4);
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, above)) {
                if (!e.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                    e.igniteForSeconds(3);
                }
            }
        }
        if (!isTower(worldPosition.below())) {
            SolarPlant.Tower primary = plant == null ? null : plant.primaryTower();
            if (primary != null && primary.x() == worldPosition.getX() && primary.z() == worldPosition.getZ()) {
                generatePower(server);
            } else {
                torque = 0;
                omega = 0;
                power = 0;
            }
        } else {
            torque = 0;
            omega = 0;
            power = 0;
        }
        BlockPos abovePos = worldPosition.above();
        if (!level.getBlockState(abovePos).isAir() && !(level.getBlockEntity(abovePos) instanceof SodiumSolarUpgrades)) {
            if (level.getBlockEntity(abovePos) instanceof SolarTowerBlockEntity tower) {
                temperature = tower.temperature;
            }
            syncIfChanged(server);
            return;
        }
        if (level.getBlockEntity(new BlockPos(worldPosition.getX(), topLocation + 1, worldPosition.getZ())) instanceof SodiumSolarUpgrades.SodiumSolarReceiver receiver
                && receiver.isActive()) {
            receiver.tick(Math.max(0, size), overallBrightness);
            temperature = receiver.getTemperature();
        }
        if (plant != null) {
            if (top) {
                if (++recountTimer >= RECOUNT_TICKS || size == -1) {
                    recountTimer = 0;
                    overallBrightness = plant.overallBrightness(server);
                    size = plant.mirrorCount();
                }
            } else {
                SolarTowerBlockEntity topTower = towerAt(topLocation);
                size = topTower == null ? 0 : topTower.size;
                overallBrightness = topTower == null ? 0 : topTower.overallBrightness;
            }
        } else {
            size = -1;
            overallBrightness = 0;
        }
        syncIfChanged(server);
    }

    private void syncIfChanged(ServerLevel server) {
        if (torque != syncedTorque && server.getGameTime() % SYNC_TICKS == 0) {
            syncedTorque = torque;
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** Pulls the fluid in the blocks above it down into this block's tank, ready to be used. */
    private void gatherFluid() {
        int y = worldPosition.getY() + 1;
        SolarTowerBlockEntity tower;
        while ((tower = towerAt(y)) != null && isTower(tower.worldPosition)) {
            FluidStack theirs = tower.tank.getFluid();
            if (!theirs.isEmpty() && (tank.isEmpty() || FluidStack.isSameFluid(tank.getFluid(), theirs))) {
                int moved = tank.fill(theirs.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (moved > 0) {
                    tower.tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                }
            }
            y++;
        }
    }

    private boolean tankIsWater() {
        return tank.getFluid().getFluid() == Fluids.WATER;
    }

    private boolean tankIsSodium() {
        return tank.getFluid().is(SODIUM);
    }

    private void generatePower(ServerLevel server) {
        gatherFluid();
        int amount = consumedFluid();
        boolean water = tankIsWater();
        omega = water ? GEN_OMEGA : GEN_OMEGA_SODIUM;
        torque = generatedTorque();
        if (arraySizeOfTop() <= 0 || torque == 0 || tank.getFluidAmount() < amount || (!water && temperature < 800)) {
            omega = 0;
            torque = 0;
        }
        power = (long) omega * torque;
        if (tankIsSodium()) {
            amount = (int) Math.max(1, amount * power / ((double) GEN_OMEGA_SODIUM * MAX_TORQUE_SODIUM));
        }
        double scale = RotaryConfig.get(RotaryConfig.SOLAR_FLUID_USE);
        if (scale != 1) {
            amount = (int) Math.round(amount * scale);
        }
        currentConsumption = amount;
        if (power > 0 && !tank.isEmpty() && amount > 0) {
            if (!water && server.getBlockEntity(worldPosition.below()) instanceof SodiumSolarUpgrades.SodiumSolarOutput output && output.isActive()) {
                amount = output.receiveSodium(amount);
            }
            if (amount > 0) {
                tank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private int arraySizeOfTop() {
        SolarTowerBlockEntity topTower = towerAt(topOfTower());
        return topTower == null ? 0 : Math.max(0, topTower.size);
    }

    private float brightnessOfTop() {
        SolarTowerBlockEntity topTower = towerAt(topOfTower());
        return topTower == null ? 0 : topTower.overallBrightness;
    }

    private int generatedTorque() {
        if (tank.isEmpty() || plant == null) {
            return 0;
        }
        boolean water = tankIsWater();
        int cap = water ? MAX_TORQUE : MAX_TORQUE_SODIUM;
        float factor = water ? 1 : 1.75f;
        float exponent = water ? 1 : 1.5f;
        return Math.min(cap, (int) (factor * brightnessOfTop() * plant.towerMultiplier() * Math.pow(arraySizeOfTop() + 1, exponent)));
    }

    /** Fluid used each tick for the power being made now (the original's formula; sodium goes much further). */
    public int consumedFluid() {
        boolean sodium = tankIsSodium();
        int p = power > 0 ? 63 - Long.numberOfLeadingZeros(power) : 0;
        if (sodium) {
            p = Math.max(1, p);
        }
        int base = 10 + (sodium ? 64 : 16) * p;
        int rounding = base >= 1000 ? 1000 : base >= 100 ? 100 : 10;
        int result = (base + rounding - 1) / rounding * rounding;
        if (sodium) {
            result = (int) (result * (1 / 128D));
        }
        return result;
    }

    @Override
    public int getTorqueOut(Direction side) {
        return side == Direction.DOWN && omega > 0 ? torque : 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        return side == Direction.DOWN && torque > 0 ? omega : 0;
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("temperature", temperature);
        // what it is doing now, for anything that looks at its data (not read back)
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        tag.putInt("mirrors", size);
        tag.putFloat("brightness", overallBrightness);
        tag.putBoolean("hasPlant", plant != null);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        temperature = tag.getInt("temperature");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        tag.putInt("temperature", temperature);
        tag.putInt("flow", currentConsumption);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        torque = tag.getInt("torque");
        omega = tag.getInt("omega");
        temperature = tag.getInt("temperature");
        currentConsumption = tag.getInt("flow");
        tank.readFromNBT(registries, tag.getCompound("tank"));
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
