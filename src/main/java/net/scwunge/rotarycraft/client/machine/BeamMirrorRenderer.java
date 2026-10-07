package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.scwunge.rotarycraft.blockentity.BeamMirrorBlockEntity;

/** The Beam Mirror (ModelBeamMirror): the mirror and its frame tilt to follow the sun, and the glass stretches as it tilts. */
public class BeamMirrorRenderer extends ModelMachineRenderer<BeamMirrorBlockEntity> {
    private static final String[] TILTING = {"Shape5d", "Shape5", "Shape5b", "Shape5c"};

    public BeamMirrorRenderer() {
        super("beam_mirror", "beam_mirror");
    }

    @Override
    protected void orient(BeamMirrorBlockEntity machine, PoseStack pose) {
        Direction facing = machine == null ? Direction.EAST : machine.facing();
        int yaw = switch (facing) {
            case EAST -> 270;
            case WEST -> 90;
            case SOUTH -> 0;
            default -> 180;
        };
        pose.mulPose(Axis.YP.rotationDegrees(yaw + 90 - 90));
    }

    @Override
    protected void draw(BeamMirrorBlockEntity machine, float partialTick, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        float theta = machine == null || machine.getLevel() == null ? 0 : BeamMirrorBlockEntity.tilt(machine.getLevel(), partialTick);
        model.render(pose, buffer, light, overlay, "Shape1", "Shape4");
        pose.pushPose();
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.XP.rotationDegrees(theta));
        pose.translate(0, -1, 0);
        model.render(pose, buffer, light, overlay, TILTING);
        pose.popPose();
        double scale = 1 - 0.725 * Math.cos(Math.toRadians(theta)) * Math.sin(Math.toRadians(-theta));
        pose.pushPose();
        pose.translate(0, 1.5, 0);
        pose.scale(1, (float) scale, 1);
        pose.translate(0, -1.5, 0);
        model.render(pose, buffer, light, overlay, "Shape5a");
        pose.popPose();
    }
}
