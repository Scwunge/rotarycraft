package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ComposterBlockEntity;
import net.scwunge.rotarycraft.menu.ComposterMenu;

/** The original Composter GUI: a progress arrow and a thermometer on the left (it works between 40 and 70 C). */
public class ComposterScreen extends AbstractContainerScreen<ComposterMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/composter.png");

    public ComposterScreen(ComposterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int arrow = menu.timer() * 48 / ComposterBlockEntity.CYCLE;
        g.blit(TEXTURE, leftPos + 79, topPos + 34, 176, 14, arrow + 1, 16);
        int temp = Math.max(0, Math.min(54, menu.temperature() * 54 / ComposterBlockEntity.MAX_TEMPERATURE));
        g.blit(TEXTURE, leftPos + 24, topPos + 70 - temp, 177, 86 - temp, 9, temp);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 23 && mx < 34 && my >= 15 && my < 71) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.composter.temperature", menu.temperature()), mouseX, mouseY);
        }
    }
}
