package net.scwunge.rotarycraft.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.TurretShot;

/** A turret shot drawn as the original draws the rail gun slug and the freeze gun snowball: a star of eight quads facing the camera. */
public class StarShotRenderer extends EntityRenderer<TurretShot> {
    private final ResourceLocation texture;

    public StarShotRenderer(EntityRendererProvider.Context context, String texture) {
        super(context);
        this.texture = RotaryCraft.id("textures/entity/" + texture + ".png");
        shadowRadius = 0.15f;
    }

    @Override
    public void render(TurretShot shot, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(0.3f, 0.3f, 0.3f);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (int i = 0; i < 360; i += 45) {
            pose.mulPose(Axis.YP.rotationDegrees(i));
            PoseStack.Pose p = pose.last();
            vertex(vc, p, -0.5f, -0.25f, 0, 0, light);
            vertex(vc, p, 0.5f, -0.25f, 0, 1, light);
            vertex(vc, p, 0.5f, 0.75f, 1, 1, light);
            vertex(vc, p, -0.5f, 0.75f, 1, 0, light);
        }
        pose.popPose();
        super.render(shot, yaw, partialTick, pose, buffers, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float u, float v, int light) {
        vc.addVertex(p, x, y, 0).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(TurretShot shot) {
        return texture;
    }
}
