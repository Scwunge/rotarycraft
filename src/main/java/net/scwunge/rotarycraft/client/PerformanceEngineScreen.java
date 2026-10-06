package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.FuelEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.PerformanceEngineBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.FuelEngineMenu;

import java.util.List;

/** The original Performance Engine GUI: water gauge, fuel and additive gauges, and a thermometer. */
public class PerformanceEngineScreen extends AbstractContainerScreen<FuelEngineMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/performance_engine.png");

    public PerformanceEngineScreen(FuelEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    private void gauge(GuiGraphics g, int x, int u, int vBottom, int width, int height) {
        if (height > 0) {
            g.blit(TEXTURE, leftPos + x, topPos + 71 - height, u, vBottom - height, width, height);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        gauge(g, 41, 193, 55, 5, menu.water() * 54 / PerformanceEngineBlockEntity.WATER_CAPACITY);
        gauge(g, 128, 177, 99, 9, Math.max(0, Math.min(54, menu.temperature() * 54 / PerformanceEngineBlockEntity.MAX_TEMPERATURE)));
        gauge(g, 82, 200, 55, 6, (int) ((long) menu.fuel() * 54 / FuelEngineBlockEntity.CAPACITY));
        gauge(g, 89, 207, 55, 6, Math.min(54, menu.additives() * 54 / PerformanceEngineBlockEntity.MAX_ADDITIVES));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (my < 16 || my >= 72) {
            return;
        }
        Component power = Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega());
        if (mx >= 40 && mx < 47) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.water", menu.water(), PerformanceEngineBlockEntity.WATER_CAPACITY), mouseX, mouseY);
        } else if (mx >= 81 && mx < 88) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.rotarycraft.fuel", menu.fuel(), FuelEngineBlockEntity.CAPACITY), power), mouseX, mouseY);
        } else if (mx >= 88 && mx < 96) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.additives", menu.additives(), PerformanceEngineBlockEntity.MAX_ADDITIVES), mouseX, mouseY);
        } else if (mx >= 126 && mx < 139) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.rotarycraft.temperature", menu.temperature()),
                    Component.translatable("gui.rotarycraft.performance_engine.limit", PerformanceEngineBlockEntity.MAX_TEMPERATURE)), mouseX, mouseY);
        }
    }
}
