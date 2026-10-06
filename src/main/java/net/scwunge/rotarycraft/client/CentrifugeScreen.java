package net.scwunge.rotarycraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.CentrifugeBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.CentrifugeMenu;

/** The original Centrifuge GUI: spinning progress ring, 3x3 outputs, a 70 px tank gauge on the right. */
public class CentrifugeScreen extends AbstractContainerScreen<CentrifugeMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/centrifuge.png");
    private static final int TANK_X = 152;
    private static final int TANK_BOTTOM = 78;
    private static final int TANK_HEIGHT = 70;

    public CentrifugeScreen(CentrifugeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int ring = Math.min(37, menu.progress() * 37 / menu.operationTime());
        if (ring > 0) {
            g.blit(TEXTURE, leftPos + 45, topPos + 27, 178, 1, ring, 37);
        }
        int h = Math.min(TANK_HEIGHT, menu.fluidAmount() * TANK_HEIGHT / CentrifugeBlockEntity.CAPACITY);
        if (h > 0 && menu.fluid() != Fluids.EMPTY) {
            drawFluid(g, new FluidStack(menu.fluid(), 1000), leftPos + TANK_X, topPos + TANK_BOTTOM - h, h);
        }
    }

    /** Tiles the fluid's still texture up the gauge, tinted the way the fluid is in the world. */
    private static void drawFluid(GuiGraphics g, FluidStack fluid, int x, int top, int height) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int tint = ext.getTintColor(fluid);
        float r = (tint >> 16 & 0xFF) / 255F;
        float gr = (tint >> 8 & 0xFF) / 255F;
        float b = (tint & 0xFF) / 255F;
        for (int y = top + height; y > top; y -= 16) {
            int piece = Math.min(16, y - top);
            g.blit(x, y - piece, 0, 16, piece, sprite, r, gr, b, 1F);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= TANK_X && mx < TANK_X + 16 && my >= TANK_BOTTOM - TANK_HEIGHT && my < TANK_BOTTOM) {
            Component name = menu.fluidAmount() > 0 && menu.fluid() != Fluids.EMPTY
                    ? new FluidStack(menu.fluid(), 1).getHoverName() : Component.translatable("gui.rotarycraft.empty");
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.tank", name, menu.fluidAmount(), CentrifugeBlockEntity.CAPACITY), mouseX, mouseY);
        } else if (mx >= 45 && mx < 82 && my >= 27 && my < 64) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()),
                    menu.torque(), menu.omega()), mouseX, mouseY);
        }
    }
}
