package net.scwunge.rotarycraft.process;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.MachineEnergy;
import net.scwunge.rotarycraft.registry.ProcessRegistry;

/**
 * The Dynamo, as the original's: shaft power in at its back, Forge Energy out of its front: torque up to 1024 N*m and speed up to 8192
 * rad/s count (so 8 MW at most), at the converter efficiency, and a watts-per-FE rate set in the main config. It does not pass the power on.
 */
public class DynamoBlockEntity extends PowerBlockEntity implements net.scwunge.rotarycraft.upgrade.Upgradable {
    public static final int MAX_TORQUE = 1024;
    public static final int MAX_TORQUE_UPGRADED = 2048;
    public static final int MAX_OMEGA = 8192;
    private boolean flux;

    public boolean isUpgraded() {
        return flux;
    }

    @Override
    public boolean canUpgradeWith(net.minecraft.world.item.ItemStack stack) {
        return !flux && stack.getItem() instanceof net.scwunge.rotarycraft.item.EngineUpgradeItem up && up.kind() == net.scwunge.rotarycraft.item.EngineUpgradeItem.Kind.FLUX;
    }

    @Override
    public void upgradeWith(net.minecraft.world.item.ItemStack stack) {
        flux = true;
        setChanged();
    }
    private final MachineEnergy energy = new MachineEnergy(1_000_000, 0, 1_000_000, this::setChanged);

    public DynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ProcessRegistry.DYNAMO_BE.get(), pos, state);
    }

    public IEnergyStorage energy() {
        return energy;
    }

    @Override
    protected boolean outputsPower() {
        return false;
    }

    /** FE made each tick at the power the dynamo is turned with. */
    public int generated() {
        long power = (long) Math.min(torque, flux ? MAX_TORQUE_UPGRADED : MAX_TORQUE) * Math.min(omega, MAX_OMEGA);
        return (int) Math.min(Integer.MAX_VALUE, (long) (power / RotaryConfig.get(RotaryConfig.WATTS_PER_FE) * RotaryConfig.get(FarmConfig.CONVERTER_EFFICIENCY)));
    }

    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = readInput();
        setPower(in.torque(), in.omega());
        if (FarmConfig.enabled("dynamo")) {
            energy.generate(generated());
        }
        IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(facing()), facing().getOpposite());
        if (target != null && target.canReceive() && energy.getEnergyStored() > 0) {
            int sent = target.receiveEnergy(energy.extractEnergy(energy.getEnergyStored(), true), false);
            energy.extractEnergy(sent, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("flux", flux);
        tag.put("energy", energy.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flux = tag.getBoolean("flux");
        if (tag.get("energy") instanceof IntTag stored) {
            energy.deserializeNBT(registries, stored);
        }
    }
}
