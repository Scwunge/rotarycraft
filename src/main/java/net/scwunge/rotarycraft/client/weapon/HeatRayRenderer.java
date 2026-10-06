package net.scwunge.rotarycraft.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.turret.HeatRayBlockEntity;

/** The Heat Ray, drawn from the original's model and turned to face the way it fires. */
public class HeatRayRenderer implements BlockEntityRenderer<HeatRayBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("heat_ray");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/heat_ray.png");

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, Direction facing) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(90));
            default -> pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 90));
        }
        pose.translate(-0.5, -0.5, -0.5);
        ReikaModel.enterModelSpace(pose);
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(HeatRayBlockEntity ray, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, light, overlay, ray.facing());
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, Direction.NORTH);
        }
    }
}
