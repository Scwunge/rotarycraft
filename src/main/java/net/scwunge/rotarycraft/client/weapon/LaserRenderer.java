package net.scwunge.rotarycraft.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.weapon.turret.LaserGunBlockEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** The laser gun: the turret, and its beam, a red glow round a white core, out as far as it reaches. */
public class LaserRenderer extends TurretRenderer<LaserGunBlockEntity> {
    public LaserRenderer(Look look) {
        super(look);
    }

    @Override
    public void render(LaserGunBlockEntity gun, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        super.render(gun, partialTick, pose, buffers, light, overlay);
        int length = gun.beamLength();
        if (length <= 0 || gun.getLevel() == null) {
            return;
        }
        Vec3 d = gun.direction();
        Vec3 o = gun.origin().subtract(gun.getBlockPos().getX(), gun.getBlockPos().getY(), gun.getBlockPos().getZ());
        pose.pushPose();
        pose.translate(o.x, o.y, o.z);
        pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 0, 1), new Vector3f((float) d.x, (float) d.y, (float) d.z)));
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        beam(vc, pose.last(), length, 0.07f, 255, 40, 40, 90);
        beam(vc, pose.last(), length, 0.025f, 255, 255, 255, 220);
        pose.popPose();
    }

    /** A beam along +z, as two crossed quads. */
    private static void beam(VertexConsumer vc, PoseStack.Pose p, float length, float w, int r, int g, int b, int a) {
        quad(vc, p, -w, 0, w, 0, length, r, g, b, a);
        quad(vc, p, 0, -w, 0, w, length, r, g, b, a);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float length, int r, int g, int b, int a) {
        vc.addVertex(p, x0, y0, 0).setColor(r, g, b, a);
        vc.addVertex(p, x1, y1, 0).setColor(r, g, b, a);
        vc.addVertex(p, x1, y1, length).setColor(r, g, b, a);
        vc.addVertex(p, x0, y0, length).setColor(r, g, b, a);
    }

    @Override
    public AABB getRenderBoundingBox(LaserGunBlockEntity gun) {
        return new AABB(gun.getBlockPos()).inflate(260);
    }
}
