package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.scwunge.rotarycraft.blockentity.PileDriverBlockEntity;

/**
 * The Pile Driver (ModelPileDriver): two sets of cogs that turn together and a crank that turns at 2.5 times their rate, all about the axle one block
 * up in the model; idle, the casing parts that are hidden while it runs are drawn, and running, the parts of the working head are.
 */
public class PileDriverRenderer extends ModelMachineRenderer<PileDriverBlockEntity> {
    private static final String[] COG_A = {"Shape9", "Shape9a", "Shape9b", "Shape9c", "Shape9d", "Shape9e", "Shape9f", "Shape9g", "Shape10"};
    private static final String[] COG_B = {"Shape9h", "Shape10af", "Shape10z", "Shape10y", "Shape10x", "Shape10w", "Shape10v", "Shape10u", "Shape10t"};
    private static final String[] CASING = {"Shape1o", "Shape1a", "Shape1b", "Shape1c", "Shape1d", "Shape1e", "Shape1f", "Shape1g"};
    private static final String[] CRANK = {"Shape2", "Shape2a"};
    private static final String[] FIXED = {"Shape3", "Shape3a", "Shape4", "Shape4a", "Shape4b", "Shape4c"};
    private static final String[] HEAD = {"Shape5", "Shape5a", "Shape6", "Shape5b", "Shape5c", "Shape7", "Shape7a"};

    public PileDriverRenderer() {
        super("pile_driver", "pile_driver");
    }

    @Override
    protected void orient(PileDriverBlockEntity machine, PoseStack pose) {
        Direction facing = machine == null ? Direction.EAST : machine.facing();
        pose.mulPose(Axis.YP.rotationDegrees(facing.getAxis() == Direction.Axis.X ? 0 : 90));
    }

    /** The angle of the crank: the original adds log2(speed + 1) to the power of 1.05 every tick it works. */
    private static float phi(PileDriverBlockEntity machine, float partialTick) {
        if (machine == null || machine.getLevel() == null || machine.getOmega() <= 0) {
            return 0;
        }
        double step = Math.pow(Math.log(machine.getOmega() + 1D) / Math.log(2), 1.05);
        return (float) ((machine.getLevel().getGameTime() + partialTick) * step % 3600);
    }

    private void spin(PoseStack pose, float degrees) {
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.XP.rotationDegrees(degrees));
        pose.translate(0, -1, 0);
    }

    @Override
    protected void draw(PileDriverBlockEntity machine, float partialTick, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        boolean on = machine != null && machine.getOmega() > 0;
        float phi = phi(machine, partialTick);
        if (!on) {
            model.render(pose, buffer, light, overlay, "Shape1");
        }
        pose.pushPose();
        spin(pose, phi / 2.5F);
        model.render(pose, buffer, light, overlay, COG_A);
        model.render(pose, buffer, light, overlay, COG_B);
        pose.popPose();
        if (!on) {
            model.render(pose, buffer, light, overlay, CASING);
        }
        pose.pushPose();
        spin(pose, phi);
        model.render(pose, buffer, light, overlay, CRANK);
        pose.popPose();
        model.render(pose, buffer, light, overlay, FIXED);
        if (on) {
            model.render(pose, buffer, light, overlay, HEAD);
        }
    }
}
