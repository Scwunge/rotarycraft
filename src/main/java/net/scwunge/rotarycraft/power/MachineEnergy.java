package net.scwunge.rotarycraft.power;

import net.neoforged.neoforge.energy.EnergyStorage;

/** FE buffer whose external receive/extract limits are separate from what the owning machine moves internally. */
public class MachineEnergy extends EnergyStorage {
    private final Runnable onChange;

    public MachineEnergy(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    /** Adds energy made by the machine itself; returns how much fitted. */
    public int generate(int amount) {
        int added = Math.min(capacity - energy, Math.max(0, amount));
        if (added > 0) {
            energy += added;
            onChange.run();
        }
        return added;
    }

    /** Takes energy the machine uses itself; returns true when the full amount was there. */
    public boolean consume(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        onChange.run();
        return true;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(1, capacity);
        if (energy > this.capacity) {
            energy = this.capacity;
        }
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int r = super.receiveEnergy(toReceive, simulate);
        if (r > 0 && !simulate) {
            onChange.run();
        }
        return r;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int e = super.extractEnergy(toExtract, simulate);
        if (e > 0 && !simulate) {
            onChange.run();
        }
        return e;
    }
}
