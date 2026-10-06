package net.scwunge.rotarycraft.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.turret.LandmineBlockEntity;

/** The Landmine, drawn from the original's model. */
public class LandmineRenderer implements BlockEntityRenderer<LandmineBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("landmine");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/landmine.png");

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(LandmineBlockEntity mine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, light, overlay);
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay);
        }
    }
}
