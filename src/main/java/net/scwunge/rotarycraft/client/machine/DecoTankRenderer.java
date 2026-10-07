package net.scwunge.rotarycraft.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.decor.DecoTank;
import net.scwunge.rotarycraft.decor.DecoTankBlockEntity;
import org.joml.Matrix4f;

/** The fluid inside a Decorative Tank: a cube of it just inside the glass, in the fluid's own colour unless the tank is set to ignore it, bright if it glows. */
public class DecoTankRenderer implements BlockEntityRenderer<DecoTankBlockEntity> {
    private static final float MIN = 1 / 16F;
    private static final float MAX = 15 / 16F;

    @Override
    public void render(DecoTankBlockEntity tank, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = tank.fluid();
        if (fluid.isEmpty()) {
            return;
        }
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int tint = tank.getBlockState().getValue(DecoTank.Flag.NOCOLOR.property) ? 0xFFFFFFFF : ext.getTintColor(fluid);
        boolean glows = fluid.getFluidType().getLightLevel() >= 15 || tank.getBlockState().getValue(DecoTank.Flag.LIGHTED.property);
        int lit = glows ? 0xF000F0 : light;
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        int r = tint >> 16 & 0xFF;
        int g = tint >> 8 & 0xFF;
        int b = tint & 0xFF;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        // top, bottom, then the four sides
        quad(vc, pose, m, r, g, b, lit, u0, u1, v0, v1, 0, 1, 0, MIN, MAX, MAX, MAX, MAX, MAX, MIN, MIN, MAX, MIN);
        quad(vc, pose, m, r, g, b, lit, u0, u1, v0, v1, 0, -1, 0, MIN, MIN, MIN, MAX, MIN, MIN, MAX, MIN, MAX, MIN, MIN, MAX);
        side(vc, pose, m, r, g, b, lit, u0, u1, v0, v1, 0, 0, -1, MAX, MIN, MIN, MIN, MIN, MIN, MIN, MAX, MIN, MAX, MAX, MIN);
        side(vc, pose, m, r, g, b, lit, u0, u1, v0, v1, 0, 0, 1, MIN, MIN, MAX, MAX, MIN, MAX, MAX, MAX, MAX, MIN, MAX, MAX);
        side(vc, pose, m, r, g, b, lit, u0, u1, v0, v1, -1, 0, 0, MIN, MIN, MIN, MIN, MIN, MAX, MIN, MAX, MAX, MIN, MAX, MIN);
        side(vc, pose, m, r, g, b, lit, u0, u1, v0, v1, 1, 0, 0, MAX, MIN, MAX, MAX, MIN, MIN, MAX, MAX, MIN, MAX, MAX, MAX);
    }

    /** A face from four corners (x, y, z each). */
    private static void quad(VertexConsumer vc, PoseStack pose, Matrix4f m, int r, int g, int b, int light, float u0, float u1, float v0, float v1, float nx, float ny, float nz,
                             float... c) {
        float[] us = {u0, u1, u1, u0};
        float[] vs = {v1, v1, v0, v0};
        // drawn both ways round, so the face shows whichever side the glass is seen from
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                int i = pass == 0 ? k : 3 - k;
                vc.addVertex(m, c[i * 3], c[i * 3 + 1], c[i * 3 + 2]).setColor(r, g, b, 230).setUv(us[i], vs[i]).setLight(light)
                        .setNormal(pose.last(), pass == 0 ? nx : -nx, pass == 0 ? ny : -ny, pass == 0 ? nz : -nz).setOverlay(OverlayTexture.NO_OVERLAY);
            }
        }
    }

    private static void side(VertexConsumer vc, PoseStack pose, Matrix4f m, int r, int g, int b, int light, float u0, float u1, float v0, float v1, float nx, float ny, float nz,
                             float... c) {
        quad(vc, pose, m, r, g, b, light, u0, u1, v0, v1, nx, ny, nz, c);
    }
}
