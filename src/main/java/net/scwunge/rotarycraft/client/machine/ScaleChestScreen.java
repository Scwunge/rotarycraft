package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.scwunge.rotarycraft.logistics.ScaleChestMenu;

/** The Scale-able Chest's screen: the usual six-row chest, a page picker, and the slots the chest has not got power for greyed out. */
public class ScaleChestScreen extends AbstractContainerScreen<ScaleChestMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public ScaleChestScreen(ScaleChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 222;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> press(0)).bounds(leftPos + 112, topPos + 3, 12, 12).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> press(1)).bounds(leftPos + 152, topPos + 3, 12, 12).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, 6 * 18 + 17);
        graphics.blit(TEXTURE, leftPos, topPos + 6 * 18 + 17, 0, 126, imageWidth, 96);
        for (int i = 0; i < ScaleChestMenu.CHEST_SLOTS; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive()) {
                graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xCC303030);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        String text = (menu.page() + 1) + "/" + menu.pages();
        graphics.drawString(font, text, 138 - font.width(text) / 2, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
