package net.scwunge.rotarycraft.client.world;

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
import net.scwunge.rotarycraft.blockentity.SonicBorerBlockEntity;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;

/** The Sonic Borer, drawn from the original's model, turned to face the way it fires (the original's rotations for each of its six facings). */
public class SonicBorerRenderer implements BlockEntityRenderer<SonicBorerBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("sonic_borer");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/sonic_borer.png");

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, Direction facing) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        switch (facing) {
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(270));
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> {
            }
            case UP -> {
                pose.mulPose(Axis.XP.rotationDegrees(270));
                pose.translate(0, -1, 1);
            }
            case DOWN -> {
                pose.mulPose(Axis.XP.rotationDegrees(90));
                pose.translate(0, -1, 1);
                pose.translate(0, 0, -2);
            }
        }
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(SonicBorerBlockEntity borer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, light, overlay, borer.facing());
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, Direction.SOUTH);
        }
    }
}
