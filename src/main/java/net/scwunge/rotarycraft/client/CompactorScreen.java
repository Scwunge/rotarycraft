package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.CompactorBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.CompactorMenu;

import java.util.List;

/** The original Compactor GUI: progress arrows, a thermometer and a pressure gauge. */
public class CompactorScreen extends AbstractContainerScreen<CompactorMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/compactor.png");

    public CompactorScreen(CompactorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    /** The input column sits where the labels usually go: centre the title and leave out the inventory label. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2 + 16, titleLabelY, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int progress = Math.min(30, menu.progress() * 30 / menu.operationTime());
        if (progress > 0) {
            g.blit(TEXTURE, leftPos + 46, topPos + 14, 193, 32, progress, 58);
        }
        int p = Math.max(0, Math.min(54, (int) ((long) menu.pressure() * 54 / CompactorBlockEntity.MAX_PRESSURE)));
        if (p > 0) {
            g.blit(TEXTURE, leftPos + 147, topPos + 70 - p, 176, 82 - p, 4, p);
        }
        int t = Math.max(0, Math.min(54, menu.temperature() * 54 / CompactorBlockEntity.MAX_TEMPERATURE));
        if (t > 0) {
            g.blit(TEXTURE, leftPos + 118, topPos + 70 - t, 182, 86 - t, 9, t);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (my < 14 || my > 72) {
            return;
        }
        if (mx >= 145 && mx < 153) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.pressure", menu.pressure()), mouseX, mouseY);
        } else if (mx >= 116 && mx < 129) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.temperature", menu.temperature()), mouseX, mouseY);
        } else if (mx >= 46 && mx < 76) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.rotarycraft.compactor.needs"),
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())),
                    mouseX, mouseY);
        }
    }
}
