package net.scwunge.rotarycraft.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
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
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.menu.CoilMenu;
import net.scwunge.rotarycraft.menu.CvtMenu;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.RotaryComponents;
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
 * <li>Energy coil: stores the shaft power that reaches it as energy, as much as it is asked for at a power and torque that rise with what it
 * holds, and gives it out again as the torque and speed set in its screen while it has a redstone signal (at a torque that also rises with
 * the energy held). It blows up if it is overcharged. The bedrock coil holds far more and gives out more. The energy goes with the item.</li>
 * <li>256x gear: 256 times the torque for a 256th of the speed (torque mode), or the other way (speed mode, set with the screwdriver
 * while sneaking), at no loss but a steady use of lubricant, which it cannot run without.</li>
 * </ul>
 */
public class AdvancedGearBlockEntity extends PowerBlockEntity implements MenuProvider, MachineInteractions {
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

    /** What a coil holds, in joules times twenty: a watt for a tick adds one. */
    private long energy;
    private int releaseTorque;
    private int releaseOmega;
    private boolean releasing;
    /** The 256x gear: true trades speed for torque, false torque for speed. */
    private boolean torqueMode = true;

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

    // ---- coil ----

    public static final long CAPACITY = 720_000_000L;
    public static final long CAPACITY_BEDROCK = 240_000_000_000_000L;
    public static final int EMISSION_CAP = 1024;
    public static final int EMISSION_CAP_BEDROCK = 4096;

    public boolean isBedrockCoil() {
        return kind() == AdvancedGearBlock.Kind.BEDROCK_COIL;
    }

    /** What the coil holds, in joules times twenty. */
    public long energy() {
        return energy;
    }

    public void setEnergy(long value) {
        energy = Math.max(0, value);
        setChanged();
    }

    /** The most it can hold, in joules. */
    public long capacity() {
        return isBedrockCoil() ? CAPACITY_BEDROCK : CAPACITY;
    }

    /** The most speed or torque it can be set to give out. */
    public int maxEmission() {
        return isBedrockCoil() ? EMISSION_CAP_BEDROCK : EMISSION_CAP;
    }

    public int releaseTorque() {
        return releaseTorque;
    }

    public int releaseOmega() {
        return releaseOmega;
    }

    public void setReleaseTorque(int value) {
        releaseTorque = Math.max(0, Math.min(torqueCap(), Math.min(maxEmission(), value)));
        setChanged();
        flushPowerSync();
    }

    public void setReleaseOmega(int value) {
        releaseOmega = Math.max(0, Math.min(maxEmission(), value));
        setChanged();
        flushPowerSync();
    }

    public boolean isReleasing() {
        return releasing;
    }

    /** The smallest power of two that is at least {@code x} (one for anything less). */
    static long ceilPow2(long x) {
        return x <= 1 ? 1 : Long.highestOneBit(x - 1) << 1;
    }

    /**
     * The smallest of 1, 2, 3, 4, 6, 8, 12, 16... (a power of two, or three quarters of one) that is at least {@code x}. An assumption: the
     * original's helper (ceilPseudo2Exp) is in DragonAPI, which the reference does not have, so this is read from its name and use; it only
     * shapes the curve of the torque a coil can give.
     */
    public static int ceilPseudoPow2(int x) {
        int p = (int) ceilPow2(x);
        int threeQuarters = p / 4 * 3;
        return p >= 4 && threeQuarters >= x ? threeQuarters : p;
    }

    private static int floorLog2(long x) {
        return x <= 0 ? 0 : 63 - Long.numberOfLeadingZeros(x);
    }

    /** The power (W) the shaft must bring to charge it at all: it rises with what is held, so a fuller coil takes only harder power. */
    public long chargingPower() {
        if (energy < 20) {
            return 1;
        }
        long l = floorLog2(energy / 20);
        return ceilPow2(l * l * l * l);
    }

    /** The torque (N*m) the shaft must bring to charge it. */
    public int chargingTorque() {
        long l = floorLog2(energy / 20);
        int base = energy >= 20 ? (int) (ceilPow2(l * l * l) / 2) : 1;
        if (isBedrockCoil()) {
            long l80 = floorLog2(energy / 80);
            base = Math.max(base, energy >= 20 ? (int) Math.min(Integer.MAX_VALUE, 16 * (ceilPow2(l80 * l80 * l80) / 2)) : 16);
            if (base <= 16) {
                base = 16;
            }
        } else if (base <= 1) {
            base = 1;
        }
        return base;
    }

    /** The most torque it can give out now: it rises with the square root of what it holds. */
    public int torqueCap() {
        return ceilPseudoPow2((int) Math.ceil(Math.sqrt(energy / 20D) / 4));
    }

    private void store(IShaftPowerOutput.Reading in) {
        releasing = level.hasNeighborSignal(worldPosition);
        if (energy / 20 >= capacity()) {
            overcharge();
            return;
        }
        if (!releasing) {
            setPower(0, 0);
            long power = in.power();
            if (in.torque() >= chargingTorque() && power >= chargingPower()) {
                long next = energy + power;
                if (next < 0) {
                    overcharge();
                    return;
                }
                energy = next;
                setChanged();
            }
        } else if (energy > 0 && releaseTorque > 0 && releaseOmega > 0) {
            releaseTorque = Math.min(releaseTorque, torqueCap());
            setPower(releaseTorque, releaseOmega);
            if (level.getGameTime() % 26 == 0) {
                level.playSound(null, worldPosition, net.scwunge.rotarycraft.registry.MachineSoundRegistry.get("coil").get(), SoundSource.BLOCKS, 0.5F, 1F);
            }
            energy = Math.max(0, energy - (long) releaseTorque * releaseOmega);
            setChanged();
        } else {
            setPower(0, 0);
        }
    }

    /** Charged past what it holds: it goes up, with a blast around it. */
    private void overcharge() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean bedrock = isBedrockCoil();
        BlockPos at = worldPosition;
        server.removeBlock(at, false);
        Level.ExplosionInteraction interaction = RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
        int count = bedrock ? 24 : 3;
        int range = bedrock ? 9 : 1;
        for (int i = 0; i < count; i++) {
            server.explode(null, at.getX() + (server.random.nextDouble() * 2 - 1) * range, at.getY() + (server.random.nextDouble() * 2 - 1) * range,
                    at.getZ() + (server.random.nextDouble() * 2 - 1) * range, 8, interaction);
        }
        server.explode(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, bedrock ? 12 : 8, interaction);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (energy > 0) {
            builder.set(RotaryComponents.COIL_ENERGY.get(), energy);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        energy = Math.max(0, input.getOrDefault(RotaryComponents.COIL_ENERGY.get(), 0L));
    }

    // ---- 256x gear ----

    public static final int HIGH_RATIO = 256;

    public boolean isTorqueMode() {
        return torqueMode;
    }

    public void setTorqueMode(boolean mode) {
        torqueMode = mode;
        setChanged();
    }

    /** The lubricant a 256x gear uses on a tick it is running: the log of the larger of its torque and speed, in mB. */
    public int lubricantUse() {
        return (int) (Math.log(Math.max(1, Math.max(omega, torque))) / Math.log(2));
    }

    private void highGear(IShaftPowerOutput.Reading in) {
        if (lubricant.isEmpty()) {
            setPower(0, 0);
            return;
        }
        int force;
        int speed;
        if (torqueMode) {
            if (in.torque() <= LIMIT / HIGH_RATIO) {
                force = in.torque() * HIGH_RATIO;
            } else {
                force = LIMIT;
                strain();
            }
            speed = in.omega() / HIGH_RATIO;
        } else {
            force = in.torque() / HIGH_RATIO;
            if (in.omega() <= LIMIT / HIGH_RATIO) {
                speed = in.omega() * HIGH_RATIO;
            } else {
                speed = LIMIT;
                strain();
            }
        }
        setPower(force, speed);
        if (omega > 0 && (level.getGameTime() & 4) == 4) {
            lubricant.drain(lubricantUse(), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /**
     * The screwdriver turns an advanced gear a quarter turn along the ground (they only work level); sneaking with it switches a 256x gear
     * between trading speed for torque and torque for speed.
     */
    @Override
    public boolean onScrewdriver(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            if (kind() == AdvancedGearBlock.Kind.HIGH) {
                setTorqueMode(!torqueMode);
                player.displayClientMessage(Component.translatable(torqueMode ? "message.rotarycraft.gear.torque_mode" : "message.rotarycraft.gear.speed_mode"), true);
            }
            return true;
        }
        level.setBlock(worldPosition, getBlockState().setValue(MachineBlock.FACING, facing().getClockWise()), 3);
        return true;
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
            case COIL, BEDROCK_COIL -> store(in);
            case HIGH -> highGear(in);
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
        return kind() == AdvancedGearBlock.Kind.CVT ? new CvtMenu(id, inventory, this) : kind().isCoil() ? new CoilMenu(id, inventory, this) : null;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    // ---- client ----

    @Override
    protected int statusKey() {
        return Long.hashCode(energy / 20000) * 7 + releaseTorque * 131 + releaseOmega * 17 + (releasing ? 1 : 0) + ratio * 31 + mode.ordinal() * 1009 + stateIndex[0] * 7919 + stateIndex[1] * 104729 + targetTorque * 15485863 + torqueIn * 31 + lubricant.getFluidAmount() / 100;
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
        tag.putLong("energy", energy);
        tag.putInt("releaseTorque", releaseTorque);
        tag.putInt("releaseOmega", releaseOmega);
        tag.putBoolean("releasing", releasing);
    }

    @Override
    protected void readStatus(CompoundTag tag) {
        ratio = tag.getInt("ratio");
        mode = CvtMode.values()[Math.floorMod(tag.getInt("mode"), CvtMode.values().length)];
        stateIndex[0] = Math.floorMod(tag.getInt("off"), STATES.length);
        stateIndex[1] = Math.floorMod(tag.getInt("on"), STATES.length);
        targetTorque = tag.getInt("target");
        torqueIn = tag.getInt("torqueIn");
        energy = tag.getLong("energy");
        releaseTorque = tag.getInt("releaseTorque");
        releaseOmega = tag.getInt("releaseOmega");
        releasing = tag.getBoolean("releasing");
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
        tag.putLong("energy", energy);
        tag.putInt("releaseTorque", releaseTorque);
        tag.putInt("releaseOmega", releaseOmega);
        tag.putBoolean("torqueMode", torqueMode);
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
        energy = tag.getLong("energy");
        releaseTorque = tag.getInt("releaseTorque");
        releaseOmega = tag.getInt("releaseOmega");
        torqueMode = !tag.contains("torqueMode") || tag.getBoolean("torqueMode");
    }
}
