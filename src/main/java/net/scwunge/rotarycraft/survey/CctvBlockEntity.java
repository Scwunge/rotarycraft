package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.registry.SurveyRegistry;

/**
 * CCTV, as the original: a camera on a wound coil that is aimed (with a screwdriver, five degrees at a time: pan, or with sneak
 * tilt) and that a Screen with the same three dyes calls up. The viewer's view then moves into the camera until they sneak, are hurt
 * or the coil runs out. Unlike the original it moves only the view: the player stays where they are.
 */
public class CctvBlockEntity extends RemoteMachineBlockEntity {
    public static final int TILT_LIMIT = 60;

    private float phi;
    private float theta;

    public CctvBlockEntity(BlockPos pos, BlockState state) {
        super(SurveyRegistry.CCTV_BE.get(), pos, state);
    }

    public float phi() {
        return phi;
    }

    public float theta() {
        return theta;
    }

    /** The screwdriver: turns the camera five degrees, round or (with sneak) up and down, wrapping at the tilt limit. */
    public void aim(boolean tilt) {
        if (tilt) {
            theta -= 5;
            if (theta < -TILT_LIMIT) {
                theta = TILT_LIMIT;
            }
        } else {
            phi = (phi + 5) % 360;
        }
        setChanged();
        syncNow();
    }

    @Override
    public void activate(Player player) {
        if (isOn() && player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new SurveyNetwork.ViewCamera(getBlockPos()));
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag) {
        tag.putFloat("phi", phi);
        tag.putFloat("theta", theta);
    }

    @Override
    protected void readClientData(CompoundTag tag) {
        phi = tag.getFloat("phi");
        theta = tag.getFloat("theta");
    }
}
