package net.scwunge.rotarycraft.client.survey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import net.scwunge.rotarycraft.survey.DisplayBlockEntity;
import org.joml.Matrix4f;

import java.util.List;

/**
 * The Display, drawn from the original's model, and its board: a tinted pane five blocks wide and three high standing on it, with a
 * thin border and faint grid lines in the same colour, and the message in lines of text, scrolling when there are more than twelve.
 */
public class DisplayRenderer implements BlockEntityRenderer<DisplayBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("display");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/display.png");
    private static final int LINES = 12;
    private static final int LINE_HEIGHT = 12;
    private static final int LINE_WIDTH = 243;
    private static final float TEXT_SCALE = 0.02f;

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float yaw) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(DisplayBlockEntity display, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float yaw = -display.facing().toYRot();
        draw(pose, buffers, light, overlay, yaw);
        if (!display.isShowing()) {
            return;
        }
        pose.pushPose();
        // the board's plane stands through the middle of the block, across the way the machine faces
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5, 0, -0.5);
        board(pose, buffers.getBuffer(RenderType.debugQuads()), display);
        text(pose, buffers, display);
        pose.popPose();
    }

    /** The pane and its border, from x -2 to 3 and y 1 to 4, in the plane z = 0.495. */
    private static void board(PoseStack pose, VertexConsumer vc, DisplayBlockEntity display) {
        Matrix4f m = pose.last().pose();
        int fill = display.fill(), border = display.border();
        float z = 0.495f;
        quad(vc, m, -2, 1, 3, 4, z, fill, 96);
        float d = 0.03125f;
        float z2 = z + 0.0005f;
        quad(vc, m, -2, 4 - d, 3, 4, z2, border, 255);
        quad(vc, m, -2, 1, 3, 1 + d, z2, border, 255);
        quad(vc, m, 3 - d, 1, 3, 4, z2, border, 255);
        quad(vc, m, -2, 1, -2 + d, 4, z2, border, 255);
        for (float k = 1 + 0.0625f; k < 4; k += 0.0625f) {
            quad(vc, m, -2, k, 3, k + 0.004f, z2, border, 32);
        }
        for (float k = -2 + 0.25f; k < 3; k += 0.25f) {
            quad(vc, m, k, 1, k + 0.004f, 4, z2, border, 32);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, int rgb, int alpha) {
        int color = alpha << 24 | rgb & 0xFFFFFF;
        vc.addVertex(m, x0, y0, z).setColor(color);
        vc.addVertex(m, x1, y0, z).setColor(color);
        vc.addVertex(m, x1, y1, z).setColor(color);
        vc.addVertex(m, x0, y1, z).setColor(color);
    }

    private static void text(PoseStack pose, MultiBufferSource buffers, DisplayBlockEntity display) {
        String message = display.message();
        if (message.isEmpty() || display.getLevel() == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lines = font.split(Component.literal(message), LINE_WIDTH);
        float scroll = lines.size() > LINES ? (display.getLevel().getGameTime() * 4 % (180L * lines.size())) / 180f : 0;
        int first = scroll - (int) scroll > 0.5f ? (int) scroll + 1 : (int) scroll;
        int last = Math.min(lines.size() - 1, LINES + first - 1);
        for (int side = 0; side < 2; side++) {
            pose.pushPose();
            if (side == 1) {
                // the original draws it on the back of the pane too
                pose.translate(0.5, 0, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.translate(-0.5, 0, -0.5);
            }
            pose.translate(0, 0, side == 0 ? 0.5 + 0.006 : 0.5 + 0.006);
            pose.translate(-2 + 0.05, 3.9, 0);
            pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
            for (int i = first; i <= last; i++) {
                font.drawInBatch(lines.get(i), 0, (int) ((i - scroll) * LINE_HEIGHT), 0xFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0,
                        LightTexture.FULL_BRIGHT);
            }
            pose.popPose();
        }
    }

    @Override
    public AABB getRenderBoundingBox(DisplayBlockEntity display) {
        return new AABB(display.getBlockPos()).inflate(3).expandTowards(0, 3, 0);
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
