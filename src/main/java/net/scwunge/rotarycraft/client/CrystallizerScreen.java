package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.CrystallizerBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.CrystallizerMenu;

import java.util.List;

/** The original Crystallizer GUI: the fluid tank on the left, the icicle progress bar, the machine and freezing temperatures. */
public class CrystallizerScreen extends AbstractContainerScreen<CrystallizerMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/crystallizer.png");

    public CrystallizerScreen(CrystallizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // the tank takes the title's corner: show the temperatures instead
        Component now = Component.translatable("gui.rotarycraft.crystallizer.now", menu.temperature());
        g.drawString(font, now, 50 - font.width(now) / 2, 30, 0x404040, false);
        if (menu.fluidAmount() > 0) {
            Component freeze = Component.translatable("gui.rotarycraft.crystallizer.freezes", menu.freezingPoint());
            g.drawString(font, freeze, imageWidth / 2 - font.width(freeze) / 2, 56, menu.temperature() <= menu.freezingPoint() ? 0x207020 : 0x802020, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int bar = Math.min(44, menu.progress() * 44 / menu.operationTime());
        if (bar > 0) {
            g.blit(TEXTURE, leftPos + 29, topPos + 41, 178, 1, bar, 4);
        }
        FluidGauge.draw(g, menu.fluid(), menu.fluidAmount(), CrystallizerBlockEntity.CAPACITY, leftPos + 8, topPos + 78, 16, 72);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 7 && mx < 24 && my >= 6 && my < 79) {
            g.renderTooltip(font, FluidGauge.tooltip(menu.fluid(), menu.fluidAmount(), CrystallizerBlockEntity.CAPACITY), mouseX, mouseY);
        } else if (mx >= 29 && mx < 73 && my >= 38 && my < 52) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.rotarycraft.crystallizer.needs"),
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())),
                    mouseX, mouseY);
        }
    }
}
