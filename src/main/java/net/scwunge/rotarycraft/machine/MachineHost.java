package net.scwunge.rotarycraft.machine;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/** A block entity whose screen is described by a {@link GuiLayout}: its menu reads its slots, tanks and extra numbers through this. */
public interface MachineHost {
    GuiLayout layout();

    ItemStackHandler items();

    List<FluidTank> tanks();

    /** Torque arriving, for the screen's tooltip. */
    int hostTorque();

    int hostOmega();

    /** The machine's own numbers for its progress bars (indexes as in the layout). */
    default int extra(int index) {
        return 0;
    }
}
