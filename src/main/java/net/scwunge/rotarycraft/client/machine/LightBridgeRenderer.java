package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.scwunge.rotarycraft.blockentity.LightBridgeBlockEntity;

/** The Light Bridge projector (ModelBridge). */
public class LightBridgeRenderer extends ModelMachineRenderer<LightBridgeBlockEntity> {
    public LightBridgeRenderer() {
        super("light_bridge", "light_bridge");
    }

    @Override
    protected void orient(LightBridgeBlockEntity machine, PoseStack pose) {
        Direction facing = machine == null ? Direction.EAST : machine.facing();
        pose.mulPose(Axis.YP.rotationDegrees(sideYaw(facing)));
    }
}
