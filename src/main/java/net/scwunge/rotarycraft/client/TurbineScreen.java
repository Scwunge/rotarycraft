package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.FuelEngineBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.FuelEngineMenu;

import java.util.List;

/** The original turbine GUI (Microturbine and Jet Engine): a jet fuel gauge. */
public class TurbineScreen extends AbstractContainerScreen<FuelEngineMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/turbine.png");

    public TurbineScreen(FuelEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int h = (int) ((long) menu.fuel() * 54 / FuelEngineBlockEntity.CAPACITY);
        if (h > 0) {
            g.blit(TEXTURE, leftPos + 85, topPos + 71 - h, 207, 55 - h, 5, h);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 84 && mx < 90 && my >= 16 && my < 71) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.rotarycraft.jet_fuel", menu.fuel(), FuelEngineBlockEntity.CAPACITY),
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())),
                    mouseX, mouseY);
        }
    }
}
