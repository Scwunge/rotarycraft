package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.CoolingFinBlock;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Cooling Fin: sits against a machine (on the side it faces) and, as in the original, takes one degree off it for every degree
 * it can warm up itself, at its setting's rate (every 20, 40 or 80 ticks). It rises one degree a step towards the surroundings'
 * temperature (cooler beside water or ice, 2600 C beside lava) and falls two, so it only works while it is cooler than the
 * machine. Only machines that allow it can be cooled (steam engines, gearboxes, the Compactor, the Fermenter, the Rock Melter).
 */
public class CoolingFinBlockEntity extends BlockEntity implements Heatable {
    public enum Setting {
        FULL(20), HALF(40), QUARTER(80);

        public final int tickRate;

        Setting(int tickRate) {
            this.tickRate = tickRate;
        }

        public Setting next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private int temperature = Integer.MIN_VALUE;
    private Setting setting = Setting.FULL;
    private int ticks;

    public CoolingFinBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.COOLING_FIN.get(), pos, state);
    }

    public Setting setting() {
        return setting;
    }

    public Setting cycleSetting() {
        setting = setting.next();
        setChanged();
        return setting;
    }

    @Override
    public int getTemperature() {
        return temperature == Integer.MIN_VALUE ? Ambient.temperature(level, worldPosition) : temperature;
    }

    @Override
    public int getMaxTemperature() {
        return 3000;
    }

    @Override
    public void addTemperature(int amount) {
        temperature = getTemperature() + amount;
        setChanged();
    }

    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    private Direction side() {
        return getBlockState().getValue(CoolingFinBlock.FACING);
    }

    public void serverTick() {
        if (++ticks < setting.tickRate) {
            return;
        }
        ticks = 0;
        updateTemperature();
        BlockEntity target = level.getBlockEntity(worldPosition.relative(side()));
        if (target instanceof Heatable h && h.canBeCooledWithFins() && h.getTemperature() > getTemperature()) {
            temperature = getTemperature() + 1;
            h.addTemperature(-1);
        }
        setChanged();
    }

    private void updateTemperature() {
        int tAmb = Ambient.temperature(level, worldPosition);
        boolean water = false;
        boolean lava = false;
        boolean ice = false;
        for (Direction d : Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            water |= s.getFluidState().is(FluidTags.WATER);
            lava |= s.getFluidState().is(FluidTags.LAVA);
            ice |= s.is(Blocks.ICE) || s.is(Blocks.PACKED_ICE) || s.is(Blocks.BLUE_ICE);
        }
        if (water) {
            tAmb -= 5;
        }
        if (lava) {
            tAmb = 2600;
        }
        if (ice) {
            tAmb -= 15;
        }
        int t = getTemperature();
        temperature = tAmb > t ? t + 1 : Math.max(tAmb, t - 2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("temperature", getTemperature());
        tag.putString("setting", setting.name());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        try {
            setting = Setting.valueOf(tag.getString("setting"));
        } catch (IllegalArgumentException e) {
            setting = Setting.FULL;
        }
    }
}
