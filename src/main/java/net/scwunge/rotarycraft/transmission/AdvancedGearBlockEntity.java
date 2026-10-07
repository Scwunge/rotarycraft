package net.scwunge.rotarycraft.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;

/**
 * The advanced gears, as the original made them from one block: power comes in at the back and out of the front, changed by the kind of gear.
 * <ul>
 * <li>Worm gear: 64 times the torque for 1/64 the speed, less what the worm loses (more the faster it turns).</li>
 * </ul>
 */
public class AdvancedGearBlockEntity extends PowerBlockEntity {
    /** The most torque or speed any gear passes on (the original's configured limit). */
    public static final int LIMIT = (Integer.MAX_VALUE - 1) / 2;
    public static final int WORM_RATIO = 64;

    public AdvancedGearBlockEntity(BlockPos pos, BlockState state) {
        super(TransmissionRegistry.ADVANCED_GEAR_BE.get(), pos, state);
    }

    public AdvancedGearBlock.Kind kind() {
        return getBlockState().getBlock() instanceof AdvancedGearBlock gear ? gear.kind() : AdvancedGearBlock.Kind.WORM;
    }

    /** What fraction of its speed a worm gear keeps: it falls with the speed, as the original's formula. */
    public static double wormLoss(int speed) {
        return speed <= 0 ? 1 : (128 - 4 * (Math.log(speed) / Math.log(2))) / 100;
    }

    /** The speed the worm gear gives out for a speed going in. */
    public static int wormSpeed(int in) {
        return (int) (in / WORM_RATIO * wormLoss(in));
    }

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
            default -> setPower(0, 0);
        }
    }
}
