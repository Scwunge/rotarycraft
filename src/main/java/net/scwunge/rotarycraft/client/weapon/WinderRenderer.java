package net.scwunge.rotarycraft.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.scwunge.rotarycraft.blockentity.WinderBlockEntity;

/** The Winder, drawn from the original's model: its drum turns with the shaft, and the coil is drawn when it holds one. */
public class WinderRenderer implements BlockEntityRenderer<WinderBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("winder");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/winder.png");
    private static final String[] BASE = {"Shape1", "Shape2", "Shape2a", "Shape3", "Shape3a", "Shape4"};
    private static final String[] DRUM = {"Shape5", "Shape5a"};
    private static final String[] COIL = {"Shape6", "Shape6a", "Shape6b", "Shape6c", "Shape6d", "Shape6e", "Shape6f", "Shape6g", "Shape6h", "Shape6i", "Shape6j", "Shape6k"};

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, Direction facing, float phi, boolean coil) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        if (facing.getAxis().isVertical()) {
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(facing == Direction.UP ? -90 : 90));
            pose.translate(-0.5, -0.5, -0.5);
        }
        ReikaModel.enterModelSpace(pose);
        if (facing.getAxis().isHorizontal()) {
            pose.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        }
        MODEL.render(pose, vc, light, overlay, BASE);
        pose.pushPose();
        pose.translate(0, 1.0625, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-phi));
        pose.translate(0, -1.0625, 0);
        MODEL.render(pose, vc, light, overlay, DRUM);
        pose.popPose();
        if (coil) {
            pose.scale(-1, 1, 1);
            MODEL.render(pose, vc, light, overlay, COIL);
        }
        pose.popPose();
    }

    @Override
    public void render(WinderBlockEntity winder, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        long time = winder.getLevel() == null ? 0 : winder.getLevel().getGameTime();
        float phi = winder.getOmega() <= 0 ? 0 : (float) ((time + partialTick) * Math.pow(Math.log(winder.getOmega() + 1) / Math.log(2), 1.05));
        draw(pose, buffers, light, overlay, winder.facing(), phi, winder.hasCoil());
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, Direction.NORTH, 0, false);
        }
    }
}
