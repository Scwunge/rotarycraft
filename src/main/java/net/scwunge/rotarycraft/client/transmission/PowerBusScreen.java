package net.scwunge.rotarycraft.client.transmission;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.PowerBusMenu;

/**
 * The Power Bus screen, as the original: a gear unit slot on each side (a button beside each changes torque mode to speed mode, shown by the
 * button picture), tinted in the colour of its side; a side that faces another bus block is crossed out.
 */
public class PowerBusScreen extends AbstractContainerScreen<PowerBusMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/power_bus.png");
    private static final ResourceLocation BUTTONS = RotaryCraft.id("textures/gui/borer_buttons.png");
    /** Where each side (north, south, west, east) has its slot, mode button and tint. */
    private static final int[] SLOT_X = {101, 101, 65, 137};
    private static final int[] SLOT_Y = {32, 104, 68, 68};
    private static final int[] BUTTON_X = {101, 101, 47, 155};
    private static final int[] BUTTON_Y = {14, 122, 68, 68};
    private static final int[] TINT = {0x440000FF, 0x44FF0000, 0x4400FF00, 0x44FFFF00};

    public PowerBusScreen(PowerBusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 220;
        inventoryLabelX = 30;
        inventoryLabelY = 130;
        titleLabelY = 6;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX - leftPos;
        int my = (int) mouseY - topPos;
        for (int i = 0; i < 4; i++) {
            if (menu.hasSlot(i) && mx >= BUTTON_X[i] && mx < BUTTON_X[i] + 18 && my >= BUTTON_Y[i] && my < BUTTON_Y[i] + 18) {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int i = 0; i < 4; i++) {
            if (menu.hasSlot(i)) {
                g.fill(leftPos + SLOT_X[i], topPos + SLOT_Y[i], leftPos + SLOT_X[i] + 18, topPos + SLOT_Y[i] + 18, TINT[i]);
                g.blit(BUTTONS, leftPos + BUTTON_X[i], topPos + BUTTON_Y[i], menu.speedMode(i) ? 54 : 36, 36, 18, 18);
            } else {
                g.blit(TEXTURE, leftPos + SLOT_X[i], topPos + SLOT_Y[i], 8, 8, 18, 18);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        for (int i = 0; i < 4; i++) {
            if (menu.hasSlot(i) && mx >= BUTTON_X[i] && mx < BUTTON_X[i] + 18 && my >= BUTTON_Y[i] && my < BUTTON_Y[i] + 18) {
                g.renderTooltip(font, Component.translatable(menu.speedMode(i) ? "gui.rotarycraft.bus.speed_mode" : "gui.rotarycraft.bus.torque_mode"), mouseX, mouseY);
            }
        }
    }
}
