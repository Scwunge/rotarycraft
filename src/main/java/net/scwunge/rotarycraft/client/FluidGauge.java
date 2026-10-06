package net.scwunge.rotarycraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Draws a machine tank's fluid in a GUI gauge and names it for the tooltip. */
public final class FluidGauge {
    private FluidGauge() {
    }

    /** Fills the gauge bottom-up: {@code amount / capacity} of {@code fullHeight}, tiling the fluid's still texture. */
    public static void draw(GuiGraphics g, Fluid fluid, int amount, int capacity, int x, int bottom, int width, int fullHeight) {
        int height = Math.min(fullHeight, (int) ((long) amount * fullHeight / capacity));
        if (height <= 0 || fluid == Fluids.EMPTY) {
            return;
        }
        FluidStack stack = new FluidStack(fluid, 1000);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(stack));
        int tint = ext.getTintColor(stack);
        float r = (tint >> 16 & 0xFF) / 255F;
        float gr = (tint >> 8 & 0xFF) / 255F;
        float b = (tint & 0xFF) / 255F;
        int top = bottom - height;
        for (int y = bottom; y > top; y -= 16) {
            int piece = Math.min(16, y - top);
            g.blit(x, y - piece, 0, width, piece, sprite, r, gr, b, 1F);
        }
    }

    public static Component tooltip(Fluid fluid, int amount, int capacity) {
        Component name = amount > 0 && fluid != Fluids.EMPTY
                ? new FluidStack(fluid, 1).getHoverName() : Component.translatable("gui.rotarycraft.empty");
        return Component.translatable("gui.rotarycraft.tank", name, amount, capacity);
    }
}
