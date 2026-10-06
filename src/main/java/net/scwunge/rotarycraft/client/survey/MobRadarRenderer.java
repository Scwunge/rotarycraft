package net.scwunge.rotarycraft.client.survey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import net.scwunge.rotarycraft.survey.MobRadarBlockEntity;

/** The Mob Radar, drawn from the original's model: the dish turns four degrees a tick while it has power. */
public class MobRadarRenderer implements BlockEntityRenderer<MobRadarBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("radar");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/mob_radar.png");
    private static final String[] DISH = {"Shape2a", "Shape2b", "Shape3", "Shape3a", "Shape3b", "Shape2c", "Shape2d", "Shape2e", "Shape2f", "Shape5", "Shape4a",
            "Shape4b"};

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float phi) {
        var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.render(pose, vc, light, overlay, "Shape1");
        pose.translate(0, 1, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-phi));
        pose.translate(0, -1, 0);
        MODEL.render(pose, vc, light, overlay, DISH);
        pose.popPose();
    }

    @Override
    public void render(MobRadarBlockEntity radar, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        long time = radar.getLevel() == null ? 0 : radar.getLevel().getGameTime();
        draw(pose, buffers, light, overlay, radar.isOn() ? (float) ((time + partialTick) * 4 % 360) : 0);
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay, 0);
        }
    }
}
