package net.scwunge.rotarycraft.client.weapon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.weapon.AmmoMenu;

/** A machine's storage, drawn with the vanilla chest background for its number of rows (as the original's basic storage GUI). */
public class AmmoScreen extends AbstractContainerScreen<AmmoMenu> {
    private static final ResourceLocation CHEST = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private final int rows;

    public AmmoScreen(AmmoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        rows = menu.rows();
        imageHeight = 114 + rows * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        g.blit(CHEST, x, y, 0, 0, imageWidth, rows * 18 + 17);
        g.blit(CHEST, x, y + rows * 18 + 17, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
