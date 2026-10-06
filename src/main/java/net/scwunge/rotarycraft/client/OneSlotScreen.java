package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.item.ShaftCoreItem;
import net.scwunge.rotarycraft.menu.OneSlotMenu;

/** The original one-slot machine GUI, with the core's charge, the AC signal and the shaft power written underneath the slot. */
public class OneSlotScreen extends AbstractContainerScreen<OneSlotMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/one_slot.png");

    public OneSlotScreen(OneSlotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int m = ShaftCoreItem.magnetization(menu.item());
        g.drawString(font, Component.translatable("gui.rotarycraft.magnetization", m), 8, 58, 0x404040, false);
        Component ac = Component.translatable(menu.alternating() ? "gui.rotarycraft.ac_on" : "gui.rotarycraft.ac_off");
        g.drawString(font, ac, imageWidth - 8 - font.width(ac), 58, menu.alternating() ? 0x207020 : 0x802020, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int my = mouseY - topPos;
        if (my >= 56 && my < 68 && hoveredSlot == null) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()),
                    menu.torque(), menu.omega()), mouseX, mouseY);
        }
    }
}
