package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Friction Heater: rubs shaft power into heat for the machine in front of it (the Blast Furnace). Needs 32 N*m and 8 kW.
 * While powered and facing a heatable machine its temperature rises by 3 x log2(speed) x log2(torque) every tick and falls
 * by a fifth of its excess over 30 C, so it settles at 30 + 12 x log2(speed) x log2(torque) C; the machine in front is
 * brought up to that. Steel (600 C) needs log2(speed) x log2(torque) of about 48 or more, for example 32 N*m at 1024 rad/s
 * (about 630 C); a lone steam engine (32 N*m at 512 rad/s) reaches about 570 C. Values and update order from the original.
 */
public class FrictionHeaterBlockEntity extends ConsumerBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(32, 1, 8192);
    public static final int MAX_TEMPERATURE = 2000;

    private int temperature = 20;

    public FrictionHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.FRICTION_HEATER.get(), pos, state);
    }

    public int temperature() {
        return temperature;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    private Heatable target() {
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.relative(facing()));
        return be instanceof Heatable h ? h : null;
    }

    @Override
    protected void machineTick(boolean powered) {
        Heatable target = target();
        if (powered && target != null) {
            temperature += (int) (3 * log2(omega) * log2(torque));
        }
        int tAmb = powered ? 30 : 20;
        if (temperature > tAmb) {
            temperature -= (temperature - tAmb) / 5;
            if (temperature - tAmb <= 4) {
                temperature--;
            }
        } else if (temperature < tAmb) {
            temperature = tAmb;
        }
        temperature = Math.min(MAX_TEMPERATURE, temperature);
        if (powered && target != null) {
            int diff = Math.min(target.getMaxTemperature(), temperature) - target.getTemperature();
            if (diff > 0) {
                target.addTemperature(Math.max(1, (int) (diff * target.heatMultiplier())));
            }
        }
        setChanged();
    }

    private static double log2(int v) {
        return v <= 0 ? 0 : Math.log(v) / Math.log(2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : 20;
    }
}
