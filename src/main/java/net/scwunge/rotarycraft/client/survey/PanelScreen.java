package net.scwunge.rotarycraft.client.survey;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.scwunge.rotarycraft.RotaryCraft;

/** A plain 176 x 166 machine panel from one of the original's screens, with the machine's name and the inventory. */
public class PanelScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final ResourceLocation texture;
    private final Component caption;
    private final int captionY;

    public PanelScreen(T menu, Inventory inventory, Component title, String texture, Component caption, int captionY) {
        this(menu, inventory, title, texture, caption, captionY, 166);
    }

    public PanelScreen(T menu, Inventory inventory, Component title, String texture, Component caption, int captionY, int height) {
        super(menu, inventory, title);
        this.texture = RotaryCraft.id("textures/gui/" + texture + ".png");
        this.caption = caption;
        this.captionY = captionY;
        imageWidth = 176;
        imageHeight = height;
        inventoryLabelY = height - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        if (caption != null) {
            g.drawString(font, caption, (imageWidth - font.width(caption)) / 2, captionY, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
