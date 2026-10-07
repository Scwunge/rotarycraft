package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.scwunge.rotarycraft.blockentity.LineBuilderBlockEntity;

/** The Line Builder ram (ModelRam): the head slides out along the line as each push comes round. */
public class LineBuilderRenderer extends ModelMachineRenderer<LineBuilderBlockEntity> {
    private static final String[] BODY = {"Shape1", "Shape4", "Shape4a", "Shape4b", "Shape4c", "Shape4d", "Shape4e", "Shape4f", "Shape4g", "Shape2"};
    private static final String[] HEAD = {"a", "b", "c", "d"};

    public LineBuilderRenderer() {
        super("line_builder", "line_builder");
    }

    @Override
    protected void orient(LineBuilderBlockEntity machine, PoseStack pose) {
        Direction facing = machine == null ? Direction.EAST : machine.facing();
        if (facing.getAxis().isHorizontal()) {
            pose.mulPose(Axis.YP.rotationDegrees(sideYaw(facing)));
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(facing == Direction.UP ? 90 : 270));
            pose.translate(0, -1, facing == Direction.UP ? -1 : 1);
        }
    }

    @Override
    protected void draw(LineBuilderBlockEntity machine, float partialTick, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        model.render(pose, buffer, light, overlay, BODY);
        float phi = 0;
        if (machine != null && machine.getLevel() != null && machine.getOmega() > 0) {
            int time = Math.max(1, machine.operationTime());
            phi = 1 - ((machine.getLevel().getGameTime() + partialTick) % time) / time - 0.01F;
        }
        pose.translate(0, 0, phi * 5 / 16D);
        model.render(pose, buffer, light, overlay, HEAD);
    }
}
