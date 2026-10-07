package net.scwunge.rotarycraft.client.transmission;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.machine.MachineRenderer;
import net.scwunge.rotarycraft.transmission.BeltHubBlock;
import net.scwunge.rotarycraft.transmission.BeltHubBlockEntity;

/**
 * A belt, chain or split belt pulley drawn from the original's model, and the belt itself: each pulley wraps its end (a back and two slanting
 * pieces), and the receiving end draws the two straight runs to the other pulley, as the original's renderer did, in the colour of its kind.
 */
public class BeltRenderer extends MachineRenderer<BeltHubBlockEntity> {
    private static final ResourceLocation BELT = RotaryCraft.id("textures/machine/belt_band_belt.png");
    private static final ResourceLocation CHAIN = RotaryCraft.id("textures/machine/belt_band_chain.png");
    private static final ResourceLocation SPLIT = RotaryCraft.id("textures/machine/belt_band_split_belt.png");

    /** The original's turning of the model for the side its shaft is on (west, east, north, south). */
    static final float[] YAWS = {270, 90, 180, 0};

    /** The look of a pulley. */
    public static Look<BeltHubBlockEntity> look() {
        return Look.<BeltHubBlockEntity>spinning("belt_hub", "belt_hub", YAWS, TransmissionClient::spin, 1).oriented((pose, facing) -> {
            switch (facing) {
                case DOWN -> {
                    pose.mulPose(Axis.XP.rotationDegrees(270));
                    pose.translate(0, -1, 1);
                }
                case UP -> {
                    pose.mulPose(Axis.XP.rotationDegrees(90));
                    pose.translate(0, -1, 1);
                    pose.translate(0, 0, -2);
                }
                default -> MachineRenderer.turn(pose, YAWS, 0, facing);
            }
        });
    }

    public BeltRenderer() {
        super(look());
    }

    @Override
    public void render(BeltHubBlockEntity hub, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        super.render(hub, partialTick, pose, buffers, light, overlay);
        BlockPos other = hub.otherEnd();
        if (other == null) {
            return;
        }
        BlockPos delta = other.subtract(hub.getBlockPos());
        Direction dir = Direction.getNearest(delta.getX(), delta.getY(), delta.getZ());
        int distance = delta.distManhattan(BlockPos.ZERO);
        VertexConsumer buffer = buffers.getBuffer(RenderType.entitySolid(switch (hub.kind()) {
            case BELT -> BELT;
            case CHAIN -> CHAIN;
            case SPLIT -> SPLIT;
        }));
        Vec3 d = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
        Vec3 a = new Vec3(hub.facing().getStepX(), hub.facing().getStepY(), hub.facing().getStepZ());
        Vec3 c = d.cross(a);
        if (c.lengthSqr() < 0.5) {
            return;
        }
        Vec3 m = new Vec3(0.5, 0.5, 0.5);
        // the end wrap: a back piece and two slanting ones
        quad(pose, buffer, light, overlay, m, d, c, a, -0.375, 0.125, -0.125, -0.375, 0.125, 0.125, -0.375, -0.125, 0.125, -0.375, -0.125, -0.125);
        quad(pose, buffer, light, overlay, m, d, c, a, -0.375, 0.125, -0.125, -0.375, 0.125, 0.125, -0.125, 0.375, 0.125, -0.125, 0.375, -0.125);
        quad(pose, buffer, light, overlay, m, d, c, a, -0.375, -0.125, -0.125, -0.375, -0.125, 0.125, -0.125, -0.375, 0.125, -0.125, -0.375, -0.125);
        if (hub.isReceivingEnd()) {
            double end = distance + 0.125;
            quad(pose, buffer, light, overlay, m, d, c, a, -0.125, 0.375, -0.125, end, 0.375, -0.125, end, 0.375, 0.125, -0.125, 0.375, 0.125);
            quad(pose, buffer, light, overlay, m, d, c, a, -0.125, -0.375, -0.125, end, -0.375, -0.125, end, -0.375, 0.125, -0.125, -0.375, 0.125);
        }
    }

    /** A flat piece given by four corners, each as (along the belt, across it, along the shaft) from the pulley's centre; drawn from both sides. */
    private static void quad(PoseStack pose, VertexConsumer buffer, int light, int overlay, Vec3 m, Vec3 d, Vec3 c, Vec3 a, double... p) {
        Vec3[] v = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            v[i] = m.add(d.scale(p[i * 3])).add(c.scale(p[i * 3 + 1])).add(a.scale(p[i * 3 + 2]));
        }
        Vec3 n = v[1].subtract(v[0]).cross(v[3].subtract(v[0])).normalize();
        for (int i = 0; i < 4; i++) {
            vertex(pose, buffer, v[i], n, light, overlay, i);
        }
        for (int i = 3; i >= 0; i--) {
            vertex(pose, buffer, v[i], n.scale(-1), light, overlay, i);
        }
    }

    private static void vertex(PoseStack pose, VertexConsumer buffer, Vec3 v, Vec3 n, int light, int overlay, int corner) {
        buffer.addVertex(pose.last(), (float) v.x, (float) v.y, (float) v.z).setColor(255, 255, 255, 255).setUv(corner == 1 || corner == 2 ? 1 : 0, corner >= 2 ? 1 : 0)
                .setOverlay(overlay).setLight(light).setNormal(pose.last(), (float) n.x, (float) n.y, (float) n.z);
    }

    @Override
    public AABB getRenderBoundingBox(BeltHubBlockEntity hub) {
        AABB box = new AABB(hub.getBlockPos()).inflate(1);
        BlockPos other = hub.otherEnd();
        return other == null ? box : box.minmax(new AABB(other).inflate(1));
    }
}
