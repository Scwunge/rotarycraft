package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.SplitterBlock;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Shaft junction, as in the original.
 * <ul>
 * <li>Merge: power from the back and from the bent side joins and leaves through the front. Matching speeds add their
 * torque; mismatched speeds fight each other and the output is erratic.</li>
 * <li>Split: power from the back leaves through both the front and the bent side at the same speed, with the torque
 * divided by the ratio: 1 splits evenly, n sends 1/n to one output and the rest to the other.</li>
 * </ul>
 */
public class SplitterBlockEntity extends PowerBlockEntity {
    public static final int[] RATIOS = {1, 2, 4, 8, 16, 32};

    private boolean splitting;
    /** Index into {@link #RATIOS}. */
    private int ratioIndex;
    /** In split mode, which output gets the larger share when the ratio isn't 1. */
    private boolean favorBent;
    private int straightTorque;
    private int bentTorque;

    public SplitterBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.SPLITTER.get(), pos, state);
    }

    public Direction bentSide() {
        return getBlockState().getValue(SplitterBlock.BENT);
    }

    public boolean isSplitting() {
        return splitting;
    }

    public int ratio() {
        return RATIOS[ratioIndex];
    }

    public boolean favorsBent() {
        return favorBent;
    }

    public void toggleMode() {
        splitting = !splitting;
        setChanged();
    }

    /** Cycles 1:1, then 2..32 favouring the straight output, then 2..32 favouring the bent one. */
    public void cycleRatio() {
        if (ratioIndex == 0) {
            ratioIndex = 1;
            favorBent = false;
        } else if (ratioIndex < RATIOS.length - 1) {
            ratioIndex++;
        } else if (!favorBent) {
            ratioIndex = 1;
            favorBent = true;
        } else {
            ratioIndex = 0;
            favorBent = false;
        }
        setChanged();
    }

    public int straightTorque() {
        return splitting ? straightTorque : torque;
    }

    public int bentTorque() {
        return splitting ? bentTorque : 0;
    }

    @Override
    public int getTorqueOut(Direction side) {
        if (side == facing()) {
            return straightTorque();
        }
        if (splitting && side == bentSide()) {
            return bentTorque;
        }
        return 0;
    }

    @Override
    public int getOmegaOut(Direction side) {
        if (side == facing() || (splitting && side == bentSide())) {
            return omega;
        }
        return 0;
    }

    @Override
    public void serverTick() {
        if (level == null) {
            return;
        }
        IShaftPowerOutput.Reading back = readInput();
        if (splitting) {
            int r = ratio();
            int toBent;
            int toStraight;
            if (r == 1) {
                toBent = back.torque() / 2;
                toStraight = back.torque() / 2;
            } else {
                int small = back.torque() / r;
                int large = (int) (back.torque() * ((r - 1D) / r));
                toBent = favorBent ? large : small;
                toStraight = favorBent ? small : large;
            }
            straightTorque = toStraight;
            bentTorque = toBent;
            setPower(back.torque(), back.omega());
            return;
        }
        IShaftPowerOutput.Reading side = IShaftPowerOutput.readInput(level, worldPosition, bentSide());
        int w;
        int t;
        if (back.omega() == side.omega()) {
            w = back.omega();
            t = back.torque() + side.torque();
        } else if (back.omega() == 0 || side.omega() == 0) {
            boolean useBack = back.omega() != 0;
            w = useBack ? back.omega() : side.omega();
            t = useBack ? back.torque() : side.torque();
        } else {
            // two inputs at different speeds fight: erratic output, as in the original
            w = level.random.nextInt(Math.max(1 + back.omega(), 1 + side.omega()));
            t = level.random.nextInt(Math.min(1 + back.torque(), 1 + side.torque()));
        }
        straightTorque = t;
        bentTorque = 0;
        setPower(t, w);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("splitting", splitting);
        tag.putInt("ratioIndex", ratioIndex);
        tag.putBoolean("favorBent", favorBent);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        splitting = tag.getBoolean("splitting");
        ratioIndex = Math.max(0, Math.min(RATIOS.length - 1, tag.getInt("ratioIndex")));
        favorBent = tag.getBoolean("favorBent");
    }
}
