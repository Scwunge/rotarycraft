package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.RockMelterMenu;

/** The original Rock Melter GUI: 3x3 inputs and a 64 px tank gauge; temperature and power are shown in the middle. */
public class RockMelterScreen extends AbstractContainerScreen<RockMelterMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/rock_melter.png");
    private static final int TANK_X = 134;
    private static final int TANK_BOTTOM = 74;
    private static final int TANK_HEIGHT = 64;

    public RockMelterScreen(RockMelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        FluidGauge.draw(g, menu.fluid(), menu.fluidAmount(), RockMelterBlockEntity.CAPACITY, leftPos + TANK_X, topPos + TANK_BOTTOM, 16, TANK_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.rotarycraft.temperature_short", menu.temperature()), 86, 34, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= TANK_X - 1 && mx < TANK_X + 17 && my >= TANK_BOTTOM - TANK_HEIGHT - 1 && my < TANK_BOTTOM + 1) {
            g.renderTooltip(font, FluidGauge.tooltip(menu.fluid(), menu.fluidAmount(), RockMelterBlockEntity.CAPACITY), mouseX, mouseY);
        } else if (mx >= 84 && mx < 126 && my >= 30 && my < 44) {
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.rotarycraft.temperature", menu.temperature()),
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())),
                    mouseX, mouseY);
        }
    }
}
