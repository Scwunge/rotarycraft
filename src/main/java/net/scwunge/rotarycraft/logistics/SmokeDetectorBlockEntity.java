package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.item.CoilItem;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.machine.SpringMachineBlockEntity;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import net.scwunge.rotarycraft.registry.MachineSoundRegistry;

/**
 * Smoke Detector (TileEntitySmokeDetector): runs on a wound coil, looking for fire within eight blocks. When it sees any it sounds its alarm every four ticks
 * and gives a redstone signal; with a coil nearly spent it chirps every thirty seconds instead. Each unit of charge lasts 1200 ticks times the coil's stiffness.
 */
public class SmokeDetectorBlockEntity extends SpringMachineBlockEntity implements MachineInteractions {
    public static final String NAME = "smoke_detector";
    public static final int RANGE = 8;
    public static final int LOW_CHARGE = 8;
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).slot(80, 35).build();

    private boolean alarm;
    private boolean lowBattery;
    private int soundTicks;
    private int scanTicks;

    public SmokeDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.SMOKE_DETECTOR.type().get(), pos, state, 1, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    protected int baseDischargeTime() {
        return 1200;
    }

    public boolean isAlarming() {
        return alarm;
    }

    public boolean isLowBattery() {
        return lowBattery;
    }

    @Override
    public int redstoneOutput() {
        return alarm ? 15 : 0;
    }

    /** Whether there is fire within range. */
    public boolean seesFire(ServerLevel server) {
        for (BlockPos pos : BlockPos.betweenClosed(worldPosition.offset(-RANGE, -RANGE, -RANGE), worldPosition.offset(RANGE, RANGE, RANGE))) {
            if (server.isLoaded(pos) && server.getBlockState(pos).is(BlockTags.FIRE)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void springTick(boolean hasCoil) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!hasCoil || !MachineConfig.enabled("smokeDetector")) {
            setAlarm(server, false);
            lowBattery = false;
            return;
        }
        unwind();
        if (++scanTicks >= 5 || alarm) {
            scanTicks = 0;
            setAlarm(server, seesFire(server));
        }
        lowBattery = CoilItem.charge(items.getStackInSlot(0)) <= LOW_CHARGE;
        int delay = alarm ? 4 : lowBattery ? 600 : -1;
        if (delay > 0 && ++soundTicks >= delay) {
            soundTicks = 0;
            server.playSound(null, worldPosition, MachineSoundRegistry.get("smokealarm").get(), SoundSource.BLOCKS, 0.1F, 1F);
        } else if (delay < 0) {
            soundTicks = 0;
        }
    }

    private void setAlarm(ServerLevel server, boolean now) {
        if (now != alarm) {
            alarm = now;
            server.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                server.updateNeighborsAt(worldPosition.relative(dir), getBlockState().getBlock());
            }
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("alarm", alarm);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        alarm = tag.getBoolean("alarm");
    }
}
