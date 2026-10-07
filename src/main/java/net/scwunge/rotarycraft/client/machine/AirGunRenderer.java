package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.scwunge.rotarycraft.blockentity.AirGunBlockEntity;

import java.util.Set;

/** The Air Gun (ModelAirGun): its fan turns about the muzzle's axis at the pace the original's does. */
public class AirGunRenderer extends ModelMachineRenderer<AirGunBlockEntity> {
    private static final String[] FAN = {"Shape6", "Shape6a", "Shape7g", "Shape7h", "Shape7i", "Shape7j", "Shape7k", "Shape7l", "Shape7m", "Shape7n", "Shape7o",
            "Shape7", "Shape7a", "Shape7b", "Shape7c", "Shape7d", "Shape7e", "Shape7f"};

    public AirGunRenderer() {
        super("air_gun", "air_gun");
    }

    @Override
    protected void orient(AirGunBlockEntity machine, PoseStack pose) {
        Direction facing = machine == null ? Direction.EAST : machine.facing();
        int yaw = switch (facing) {
            case EAST -> 270;
            case WEST -> 90;
            case SOUTH -> 0;
            default -> 180;
        };
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    @Override
    protected void draw(AirGunBlockEntity machine, float partialTick, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        Set<String> fan = Set.of(FAN);
        for (String name : model.names()) {
            if (!fan.contains(name)) {
                model.render(pose, buffer, light, overlay, name);
            }
        }
        float phi = 0;
        if (machine != null && machine.getLevel() != null && machine.getOmega() > 0) {
            phi = (float) ((machine.getLevel().getGameTime() + partialTick) * Math.pow(Math.log(machine.getOmega() + 1D) / Math.log(2), 1.05) % 3600);
        }
        pose.pushPose();
        pose.translate(0, 0.9375, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-phi));
        pose.translate(0, -0.9375, 0);
        model.render(pose, buffer, light, overlay, FAN);
        pose.popPose();
    }
}
