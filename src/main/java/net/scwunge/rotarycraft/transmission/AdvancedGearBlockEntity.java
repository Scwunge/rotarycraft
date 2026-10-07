package net.scwunge.rotarycraft.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.menu.CvtMenu;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * The advanced gears, as the original made them from one block: power comes in at the back and out of the front, changed by the kind of gear.
 * <ul>
 * <li>Worm gear: 64 times the torque for 1/64 the speed, less what the worm loses (more the faster it turns).</li>
 * <li>CVT: any ratio from 1 to 32 (as many as it has belts for, in powers of two) either way, trading speed for torque or torque for speed;
 * set by hand, by the redstone signal (a ratio for each of on and off), or automatically to keep a target torque. It needs lubricant and
 * a belt in its last slot.</li>
 * </ul>
 */
public class AdvancedGearBlockEntity extends PowerBlockEntity implements MenuProvider {
    /** The most torque or speed any gear passes on (the original's configured limit). */
    public static final int LIMIT = (Integer.MAX_VALUE - 1) / 2;
    public static final int WORM_RATIO = 64;
    public static final int BELT_SLOTS = 32;
    public static final int LUBRICANT_CAPACITY = 20000;

    /** How the CVT's ratio is chosen. */
    public enum CvtMode {
        MANUAL,
        REDSTONE,
        AUTO;

        public CvtMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** The ratios a CVT can be set to for each redstone state: 1x to 32x speed, then 1x to 32x torque. */
    public static final int[] STATES = {1, 2, 4, 8, 16, 32, -1, -2, -4, -8, -16, -32};

    private final ItemStackHandler belts = new ItemStackHandler(BELT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(RotaryParts.part("belt").get());
        }
    };
    private final FluidTank lubricant = new FluidTank(LUBRICANT_CAPACITY, s -> s.is(RotaryFluids.LUBRICANT.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private int ratio = 1;
    private CvtMode mode = CvtMode.MANUAL;
    /** The redstone states' picks, off then on, as indices into {@link #STATES}. */
    private final int[] stateIndex = new int[2];
    private int targetTorque = 1;
    private int torqueIn;

    public AdvancedGearBlockEntity(BlockPos pos, BlockState state) {
        super(TransmissionRegistry.ADVANCED_GEAR_BE.get(), pos, state);
    }

    public AdvancedGearBlock.Kind kind() {
        return getBlockState().getBlock() instanceof AdvancedGearBlock gear ? gear.kind() : AdvancedGearBlock.Kind.WORM;
    }

    public ItemStackHandler belts() {
        return belts;
    }

    public FluidTank lubricant() {
        return lubricant;
    }

    // ---- worm ----

    /** What fraction of its speed a worm gear keeps: it falls with the speed, as the original's formula. */
    public static double wormLoss(int speed) {
        return speed <= 0 ? 1 : (128 - 4 * (Math.log(speed) / Math.log(2))) / 100;
    }

    /** The speed the worm gear gives out for a speed going in. */
    public static int wormSpeed(int in) {
        return (int) (in / WORM_RATIO * wormLoss(in));
    }

    // ---- CVT ----

    public CvtMode mode() {
        return mode;
    }

    public void stepMode() {
        mode = mode.next();
        setChanged();
        flushPowerSync();
    }

    public int ratio() {
        return ratio;
    }

    public int targetTorque() {
        return targetTorque;
    }

    public int torqueIn() {
        return torqueIn;
    }

    /** The most the CVT can be set to: a power of two, with one fewer belt than that in a row from the first slot. */
    public int maxRatio() {
        int count = 0;
        for (int i = 0; i < BELT_SLOTS - 1; i++) {
            if (!belts.getStackInSlot(i).is(RotaryParts.part("belt").get())) {
                break;
            }
            count++;
        }
        int f = count + 1;
        return Integer.highestOneBit(f);
    }

    public boolean hasRequiredBelt() {
        return belts.getStackInSlot(BELT_SLOTS - 1).is(RotaryParts.part("belt").get());
    }

    /** Sets the ratio (negative for torque), kept within what the belts allow, and never zero. */
    public void setRatio(int value) {
        if (value == 0) {
            ratio = 1;
        } else {
            ratio = (value < 0 ? -1 : 1) * Math.min(Math.abs(value), maxRatio());
        }
        setChanged();
        flushPowerSync();
    }

    public void setTargetTorque(int value) {
        targetTorque = Math.max(1, value);
        setChanged();
        flushPowerSync();
    }

    /** Switches between speed and torque. */
    public void flipRatio() {
        setRatio(-ratio);
    }

    /** The CVT's pick for redstone state {@code on}, as the ratio it gives (negative for torque). */
    public int stateRatio(boolean on) {
        return STATES[stateIndex[on ? 1 : 0]];
    }

    /** Moves the pick for a redstone state on to the next ratio the belts allow. */
    public void stepState(boolean on) {
        int i = on ? 1 : 0;
        do {
            stateIndex[i] = (stateIndex[i] + 1) % STATES.length;
        } while (Math.abs(STATES[stateIndex[i]]) > maxRatio());
        setChanged();
        flushPowerSync();
    }

    /** The ratio in force now: the manual or automatic one, or the redstone state's. */
    public int effectiveRatio() {
        if (mode == CvtMode.REDSTONE) {
            int picked = stateRatio(level != null && level.hasNeighborSignal(worldPosition));
            return (int) Math.signum(picked) * Math.min(Math.abs(picked), maxRatio());
        }
        return ratio;
    }

    /** Picks the ratio that keeps the torque near the target: slower and stronger below it, faster and weaker above. */
    private int autoRatio(int torqueIn) {
        if (torqueIn >= targetTorque && torqueIn < targetTorque * 2L) {
            return 1;
        }
        int max = maxRatio();
        if (torqueIn > targetTorque) {
            int val = 1;
            int has = torqueIn;
            while (has >= targetTorque && val <= max) {
                val++;
                has = torqueIn / val;
            }
            return val - 1;
        }
        int val = 1;
        long has = torqueIn;
        while (has < targetTorque && val < max) {
            val++;
            has = (long) torqueIn * val;
        }
        return -val;
    }

    // ---- shared ----

    /** Sparks and a clink when a gear is asked for more than the limit. */
    private void strain() {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CRIT, worldPosition.getX() + server.random.nextFloat(), worldPosition.getY() + server.random.nextFloat(),
                    worldPosition.getZ() + server.random.nextFloat(), 1, 0.25, 0.25, 0.25, 0.1);
            server.playSound(null, worldPosition, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.1F, 1F);
        }
    }

    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = readInput();
        switch (kind()) {
            case WORM -> {
                int out = wormSpeed(in.omega());
                int force;
                if (in.torque() <= LIMIT / WORM_RATIO) {
                    force = in.torque() * WORM_RATIO;
                } else {
                    force = LIMIT;
                    strain();
                }
                setPower(force, out);
            }
            case CVT -> {
                torqueIn = in.torque();
                if (ratio == 0) {
                    ratio = 1;
                }
                if (Math.abs(ratio) > maxRatio()) {
                    ratio = (ratio < 0 ? -1 : 1) * maxRatio();
                }
                if (mode == CvtMode.AUTO) {
                    ratio = autoRatio(in.torque());
                }
                int r = effectiveRatio();
                if (lubricant.isEmpty() || !hasRequiredBelt() || r == 0) {
                    setPower(0, 0);
                    break;
                }
                int force;
                int speed;
                if (r > 0) {
                    if (in.omega() <= LIMIT / r) {
                        speed = in.omega() * r;
                    } else {
                        speed = LIMIT;
                        strain();
                    }
                    force = in.torque() / r;
                } else {
                    if (in.torque() <= LIMIT / -r) {
                        force = in.torque() * -r;
                    } else {
                        force = LIMIT;
                        strain();
                    }
                    speed = in.omega() / -r;
                }
                setPower(force, speed);
            }
            default -> setPower(0, 0);
        }
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < BELT_SLOTS; i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), belts.getStackInSlot(i));
        }
    }

    // ---- screen ----

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return kind() == AdvancedGearBlock.Kind.CVT ? new CvtMenu(id, inventory, this) : null;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- client ----

    @Override
    protected int statusKey() {
        return ratio * 31 + mode.ordinal() * 1009 + stateIndex[0] * 7919 + stateIndex[1] * 104729 + targetTorque * 15485863 + torqueIn * 31 + lubricant.getFluidAmount() / 100;
    }

    @Override
    protected void writeStatus(CompoundTag tag) {
        tag.putInt("ratio", ratio);
        tag.putInt("mode", mode.ordinal());
        tag.putInt("off", stateIndex[0]);
        tag.putInt("on", stateIndex[1]);
        tag.putInt("target", targetTorque);
        tag.putInt("torqueIn", torqueIn);
        tag.putInt("lube", lubricant.getFluidAmount());
    }

    @Override
    protected void readStatus(CompoundTag tag) {
        ratio = tag.getInt("ratio");
        mode = CvtMode.values()[Math.floorMod(tag.getInt("mode"), CvtMode.values().length)];
        stateIndex[0] = Math.floorMod(tag.getInt("off"), STATES.length);
        stateIndex[1] = Math.floorMod(tag.getInt("on"), STATES.length);
        targetTorque = tag.getInt("target");
        torqueIn = tag.getInt("torqueIn");
        int lube = tag.getInt("lube");
        lubricant.setFluid(lube > 0 ? new net.neoforged.neoforge.fluids.FluidStack(RotaryFluids.LUBRICANT.get(), lube) : net.neoforged.neoforge.fluids.FluidStack.EMPTY);
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ratio", ratio);
        tag.putInt("mode", mode.ordinal());
        tag.putInt("off", stateIndex[0]);
        tag.putInt("on", stateIndex[1]);
        tag.putInt("target", targetTorque);
        tag.put("belts", belts.serializeNBT(registries));
        tag.put("lubricant", lubricant.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ratio = tag.contains("ratio") ? tag.getInt("ratio") : 1;
        mode = CvtMode.values()[Math.floorMod(tag.getInt("mode"), CvtMode.values().length)];
        stateIndex[0] = Math.floorMod(tag.getInt("off"), STATES.length);
        stateIndex[1] = Math.floorMod(tag.getInt("on"), STATES.length);
        targetTorque = tag.contains("target") ? Math.max(1, tag.getInt("target")) : 1;
        belts.deserializeNBT(registries, tag.getCompound("belts"));
        lubricant.readFromNBT(registries, tag.getCompound("lubricant"));
    }
}
