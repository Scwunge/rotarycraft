package net.scwunge.rotarycraft.client.world;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.WeatherMenu;

/** The Weather Controller's screen, on the original's small-chest background. */
public class WeatherScreen extends AbstractContainerScreen<WeatherMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/basic_storage.png");

    public WeatherScreen(WeatherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 114 + WeatherMenu.ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int rows = WeatherMenu.ROWS * 18 + 17;
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, rows);
        g.blit(TEXTURE, leftPos, topPos + rows, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
