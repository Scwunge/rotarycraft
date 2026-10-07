package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.scwunge.rotarycraft.blockentity.AerosolizerBlockEntity;
import org.joml.Matrix4f;

/** The Aerosolizer's model, with a patch of the stored potion's colour on top of it for each of its nine stores, rising as the store fills (RenderAerosolizer). */
public class AerosolizerRenderer extends ModelMachineRenderer<AerosolizerBlockEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("block/white_concrete");

    public AerosolizerRenderer() {
        super("aerosolizer", "aerosolizer");
    }

    @Override
    protected void drawExtras(AerosolizerBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (machine == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < AerosolizerBlockEntity.SLOTS; i++) {
            int level = machine.level(i);
            if (level <= 0) {
                continue;
            }
            int color = machine.color(i);
            float h = (float) (0.785 + 0.075 * level / AerosolizerBlockEntity.CAPACITY);
            float x0 = (float) (0.0625 + (i % 3) * (5 / 16D));
            float z0 = (float) (0.0625 + (i / 3) * (5 / 16D));
            float x1 = x0 + 0.25F;
            float z1 = z0 + 0.25F;
            int r = color >> 16 & 0xFF;
            int g = color >> 8 & 0xFF;
            int b = color & 0xFF;
            float u0 = sprite.getU0();
            float u1 = sprite.getU1();
            float v0 = sprite.getV0();
            float v1 = sprite.getV1();
            vc.addVertex(m, x0, h, z1).setColor(r, g, b, 192).setUv(u0, v1).setLight(light).setNormal(pose.last(), 0, 1, 0).setOverlay(OverlayTexture.NO_OVERLAY);
            vc.addVertex(m, x1, h, z1).setColor(r, g, b, 192).setUv(u1, v1).setLight(light).setNormal(pose.last(), 0, 1, 0).setOverlay(OverlayTexture.NO_OVERLAY);
            vc.addVertex(m, x1, h, z0).setColor(r, g, b, 192).setUv(u1, v0).setLight(light).setNormal(pose.last(), 0, 1, 0).setOverlay(OverlayTexture.NO_OVERLAY);
            vc.addVertex(m, x0, h, z0).setColor(r, g, b, 192).setUv(u0, v0).setLight(light).setNormal(pose.last(), 0, 1, 0).setOverlay(OverlayTexture.NO_OVERLAY);
        }
    }
}
