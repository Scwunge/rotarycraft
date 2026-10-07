package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.FloodlightBlockEntity;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;

/**
 * The Flood Light (RenderLamp): the lamp model for the four sides, and for up and down the vertical lamp (the same model turned over for down), which
 * shows its beam housing in beam mode.
 */
public class FloodlightRenderer extends ModelMachineRenderer<FloodlightBlockEntity> {
    private static final String[] BEAM_PARTS = {"Shape17", "Shape17b", "Shape18", "Shape18b", "Shape19"};
    private final ReikaModel vertical = new ReikaModel("floodlight_v");
    private final ResourceLocation verticalTexture = RotaryCraft.id("textures/machine/floodlight_v.png");

    public FloodlightRenderer() {
        super("floodlight", "floodlight");
    }

    private static boolean isVertical(FloodlightBlockEntity machine) {
        return machine != null && machine.facing().getAxis() == Direction.Axis.Y;
    }

    @Override
    protected ResourceLocation textureFor(FloodlightBlockEntity machine) {
        return isVertical(machine) ? verticalTexture : texture;
    }

    @Override
    protected void orient(FloodlightBlockEntity machine, PoseStack pose) {
        Direction facing = machine == null ? Direction.EAST : machine.facing();
        if (facing.getAxis().isHorizontal()) {
            int yaw = switch (facing) {
                case EAST -> 270;
                case WEST -> 90;
                case SOUTH -> 0;
                default -> 180;
            };
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
        } else if (facing == Direction.DOWN) {
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.translate(0, -2, 0);
        }
    }

    @Override
    protected void draw(FloodlightBlockEntity machine, float partialTick, PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        if (!isVertical(machine)) {
            model.renderAll(pose, buffer, light, overlay);
            return;
        }
        for (String name : vertical.names()) {
            if (machine.beamMode() || !java.util.Arrays.asList(BEAM_PARTS).contains(name)) {
                vertical.render(pose, buffer, light, overlay, name);
            }
        }
    }
}
