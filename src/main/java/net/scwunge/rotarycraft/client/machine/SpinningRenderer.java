package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;

import java.util.Set;
import java.util.function.Function;

/**
 * A model with a group of parts that turns about one axis (the original's glRotatef round a pivot) at the pace its machines turn at: log2(speed + 1) to
 * the power of 1.05 degrees a tick. The rest of the parts are drawn as they are.
 */
public class SpinningRenderer<T extends PowerBlockEntity> extends ModelMachineRenderer<T> {
    private final Set<String> spinning;
    private final Axis axis;
    private final double pivotY;
    private final Function<T, Float> yaw;

    /** {@code pivotY} is the height, in model units, of the axis the group turns about (0 when the origin is the pivot). */
    public SpinningRenderer(String modelName, String textureName, String[] spinning, Axis axis, double pivotY, Function<T, Float> yaw) {
        super(modelName, textureName);
        this.spinning = Set.of(spinning);
        this.axis = axis;
        this.pivotY = pivotY;
        this.yaw = yaw;
    }

    /** The common case: a machine that does not turn about its facing. */
    public static float noYaw(BlockEntity be) {
        return 0F;
    }

    /** Whether the machine is running, as far as the client can tell. */
    protected boolean running(T machine) {
        return machine != null && machine.getOmega() > 0;
    }

    @Override
    protected void orient(T machine, PoseStack pose) {
        float degrees = machine == null ? 0 : yaw.apply(machine);
        if (degrees != 0) {
            pose.mulPose(Axis.YP.rotationDegrees(degrees));
        }
    }

    public static float axisYaw(PowerBlockEntity machine) {
        return machine.facing().getAxis() == Direction.Axis.X ? 90F : 0F;
    }

    @Override
    protected void draw(T machine, float partialTick, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        for (String name : model.names()) {
            if (!spinning.contains(name)) {
                model.render(pose, buffer, light, overlay, name);
            }
        }
        float phi = 0;
        if (running(machine) && machine.getLevel() != null) {
            phi = (float) ((machine.getLevel().getGameTime() + partialTick) * Math.pow(Math.log(machine.getOmega() + 1D) / Math.log(2), 1.05) % 3600);
        }
        pose.pushPose();
        pose.translate(0, pivotY, 0);
        pose.mulPose(axis.rotationDegrees(phi));
        pose.translate(0, -pivotY, 0);
        for (String name : spinning) {
            model.render(pose, buffer, light, overlay, name);
        }
        pose.popPose();
    }
}
