package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.DryerBlockEntity;
import net.scwunge.rotarycraft.menu.DryerMenu;

/** The original Dryer GUI: the tank on the left and a bar that fills along the 400-tick drying period. */
public class DryerScreen extends AbstractContainerScreen<DryerMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/dryer.png");

    public DryerScreen(DryerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int bar = Math.min(91, menu.progress() * 91 / DryerBlockEntity.PERIOD);
        if (bar > 0) {
            g.blit(TEXTURE, leftPos + 29, topPos + 41, 1, 169, bar, 4);
        }
        FluidGauge.draw(g, menu.fluid(), menu.fluidAmount(), DryerBlockEntity.CAPACITY, leftPos + 8, topPos + 79, 16, 72);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 7 && mx < 24 && my >= 6 && my < 79) {
            g.renderTooltip(font, FluidGauge.tooltip(menu.fluid(), menu.fluidAmount(), DryerBlockEntity.CAPACITY), mouseX, mouseY);
        }
    }
}
