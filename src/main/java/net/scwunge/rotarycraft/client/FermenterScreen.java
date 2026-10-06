package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.FermenterBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.FermenterMenu;

/** The original Fermenter GUI: thermometer, water in the middle box, progress arrow. */
public class FermenterScreen extends AbstractContainerScreen<FermenterMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/fermenter.png");
    private static final int MAX_SHOWN_TEMPERATURE = 60;

    public FermenterScreen(FermenterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int arrow = Math.min(24, menu.cookTime() * 24 / menu.operationTime());
        if (arrow > 0) {
            g.blit(TEXTURE, leftPos + 79, topPos + 34, 176, 14, arrow + 1, 16);
        }
        int t = Math.max(0, Math.min(54, menu.temperature() * 54 / MAX_SHOWN_TEMPERATURE));
        if (t > 0) {
            g.blit(TEXTURE, leftPos + 24, topPos + 70 - t, 177, 86 - t, 9, t);
        }
        int w = 16 * menu.water() / FermenterBlockEntity.CAPACITY;
        if (w > 0) {
            g.fill(leftPos + 55, topPos + 35 + 16 - w, leftPos + 71, topPos + 51, 0xFF3F76E4);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 55 && mx < 71 && my >= 35 && my < 51) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.water", menu.water(), FermenterBlockEntity.CAPACITY), mouseX, mouseY);
        } else if (mx >= 22 && mx < 36 && my >= 15 && my < 71) {
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.rotarycraft.temperature", menu.temperature()),
                    Component.translatable("gui.rotarycraft.fermenter.best")), mouseX, mouseY);
        } else if (mx >= 79 && mx < 104 && my >= 34 && my < 50) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()),
                    menu.torque(), menu.omega()), mouseX, mouseY);
        }
    }
}
