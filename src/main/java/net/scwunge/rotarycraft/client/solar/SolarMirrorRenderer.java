package net.scwunge.rotarycraft.client.solar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import net.scwunge.rotarycraft.solar.SolarMirrorBlockEntity;
import net.scwunge.rotarycraft.solar.SolarPlant;

/**
 * A Solar Mirror, drawn from the original's model: its glass turns to throw the sun (by day, the moon by night) at the top of the tower nearest it,
 * so its normal points half way between the light and the tower. A broken mirror loses its glass.
 */
public class SolarMirrorRenderer implements BlockEntityRenderer<SolarMirrorBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("solar_mirror");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/solar_mirror.png");
    private static final String[] BASE = {"Shape1"};
    private static final String[] TURNING = {"Shape3", "Shape4"};
    private static final String[] GLASS = {"Shape5", "Shape5b", "Shape5c", "Shape5d"};
    private static final String[] UPPER = {"Shape3a", "Shape3b", "Shape3c", "Shape3d", "Shape3as"};
    private static final String[] STAY = {"Shape5a", "Shape5af", "Shape5as"};
    /** The glass's slope when the mirror is drawn with no tilt (the model is built with it at 45 degrees). */
    private static final float DEFAULT_ELEVATION = 45;
    private static final float MAX_STEP = 3;

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float yaw, float tilt, boolean broken) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.render(pose, vc, light, overlay, BASE);
        pose.pushPose();
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0, -1, 0);
        MODEL.render(pose, vc, light, overlay, TURNING);
        if (!broken) {
            pose.pushPose();
            pose.translate(0, 1, 0);
            pose.mulPose(Axis.XP.rotationDegrees(tilt));
            pose.translate(0, -1, 0);
            MODEL.render(pose, vc, light, overlay, GLASS);
            pose.popPose();
        }
        MODEL.render(pose, vc, light, overlay, UPPER);
        double scale = 1 - 0.725 * Math.cos(Math.toRadians(tilt)) * Math.sin(Math.toRadians(-tilt));
        pose.pushPose();
        pose.translate(0, 1.5, 0);
        pose.scale(1, (float) scale, 1);
        pose.translate(0, -1.5, 0);
        MODEL.render(pose, vc, light, overlay, STAY);
        pose.popPose();
        pose.popPose();
        pose.popPose();
    }

    /** Which way the glass should face now: the yaw and tilt that point its normal between the light and the tower top. */
    public static float[] aim(Level level, BlockPos pos, SolarPlant plant, float partialTick) {
        double angle = level.getSunAngle(partialTick);
        Vec3 light = new Vec3(-Math.sin(angle), Math.cos(angle), 0);
        if (light.y < 0) {
            light = light.scale(-1);
        }
        Vec3 normal = light;
        BlockPos target = plant == null ? null : plant.aimingPosition(pos);
        if (target != null) {
            Vec3 toTarget = Vec3.atCenterOf(target).subtract(Vec3.atCenterOf(pos)).normalize();
            Vec3 sum = light.add(toTarget);
            if (sum.lengthSqr() > 1.0E-6) {
                normal = sum.normalize();
            }
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-normal.x, normal.z));
        float elevation = (float) Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, normal.y))));
        return new float[] {yaw, elevation - DEFAULT_ELEVATION};
    }

    private static float approach(float from, float to) {
        float diff = to - from;
        diff = ((diff + 180) % 360 + 360) % 360 - 180;
        return Math.abs(diff) <= MAX_STEP ? from + diff : from + Math.signum(diff) * MAX_STEP;
    }

    @Override
    public void render(SolarMirrorBlockEntity mirror, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = mirror.getLevel();
        if (level != null) {
            mirror.searchForPlant();
            float[] target = aim(level, mirror.getBlockPos(), mirror.plant(), partialTick);
            mirror.renderYaw = mirror.renderAimed ? approach(mirror.renderYaw, target[0]) : target[0];
            mirror.renderTilt = mirror.renderAimed ? approach(mirror.renderTilt, target[1]) : target[1];
            mirror.renderAimed = true;
        }
        draw(pose, buffers, light, overlay, mirror.renderYaw, mirror.renderTilt, mirror.isBroken());
    }

    @Override
    public AABB getRenderBoundingBox(SolarMirrorBlockEntity mirror) {
        return new AABB(mirror.getBlockPos()).inflate(1);
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, 0, 0, false);
        }
    }
}
