package net.scwunge.rotarycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.blockentity.ReservoirBlockEntity;
import org.joml.Matrix4f;

/** Draws the reservoir's fluid surface at its fill height. */
public class ReservoirRenderer implements BlockEntityRenderer<ReservoirBlockEntity> {
    public ReservoirRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(ReservoirBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = be.tank().getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        float a = ((tint >> 24) & 0xFF) / 255F;
        float r = ((tint >> 16) & 0xFF) / 255F;
        float g = ((tint >> 8) & 0xFF) / 255F;
        float b = (tint & 0xFF) / 255F;
        float alpha = a == 0 ? 1 : a;
        float y = (1 + 14F * be.tank().getFluidAmount() / ReservoirBlockEntity.CAPACITY) / 16F;
        float min = 1 / 16F;
        float max = 15 / 16F;
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        float u0 = sprite.getU(1 / 16F);
        float u1 = sprite.getU(15 / 16F);
        float v0 = sprite.getV(1 / 16F);
        float v1 = sprite.getV(15 / 16F);
        int fullLight = fluid.getFluidType().getLightLevel(fluid) > 0 ? 0xF000F0 : light; // lava glows
        vc.addVertex(m, min, y, min).setColor(r, g, b, alpha).setUv(u0, v0).setOverlay(overlay).setLight(fullLight).setNormal(0, 1, 0);
        vc.addVertex(m, min, y, max).setColor(r, g, b, alpha).setUv(u0, v1).setOverlay(overlay).setLight(fullLight).setNormal(0, 1, 0);
        vc.addVertex(m, max, y, max).setColor(r, g, b, alpha).setUv(u1, v1).setOverlay(overlay).setLight(fullLight).setNormal(0, 1, 0);
        vc.addVertex(m, max, y, min).setColor(r, g, b, alpha).setUv(u1, v0).setOverlay(overlay).setLight(fullLight).setNormal(0, 1, 0);
    }
}
