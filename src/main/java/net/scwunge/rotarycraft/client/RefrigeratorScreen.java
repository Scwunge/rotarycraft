package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.RefrigeratorBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.RefrigeratorMenu;
import net.scwunge.rotarycraft.registry.RotaryFluids;

import java.util.List;

/** The original Refrigerator GUI: a frost bar that fills as it works, and the liquid nitrogen gauge on the right. */
public class RefrigeratorScreen extends AbstractContainerScreen<RefrigeratorMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/refrigerator.png");

    public RefrigeratorScreen(RefrigeratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 188;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int bar = Math.min(145, menu.progress() * 145 / menu.operationTime());
        if (bar > 0) {
            g.blit(TEXTURE, leftPos + 7, topPos + 17, 0, 189, bar, 67);
        }
        FluidGauge.draw(g, RotaryFluids.LIQUID_NITROGEN.get(), menu.fluidAmount(), RefrigeratorBlockEntity.CAPACITY, leftPos + 152, topPos + 90, 16, 72);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 152 && mx < 168 && my >= 18 && my < 90) {
            g.renderTooltip(font, FluidGauge.tooltip(RotaryFluids.LIQUID_NITROGEN.get(), menu.fluidAmount(), RefrigeratorBlockEntity.CAPACITY), mouseX, mouseY);
        } else if (mx >= 7 && mx < 152 && my >= 17 && my < 84) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.rotarycraft.refrigerator.needs"),
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())),
                    mouseX, mouseY);
        }
    }
}
