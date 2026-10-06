package net.scwunge.rotarycraft.client.world;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.BorerBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.BorerMenu;

/**
 * The Borer's screen, as the original: a 7 by 5 grid of cells (click to cut or leave each; the square marked with a cross is the borer's own row),
 * and buttons to put it back at its face, flip every cell and turn drops on or off. Hover the grid to read what the next slice needs.
 */
public class BorerScreen extends AbstractContainerScreen<BorerMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/borer.png");
    private static final ResourceLocation BUTTONS = RotaryCraft.id("textures/gui/borer_buttons.png");
    private static final int GRID_X = 25;
    private static final int GRID_Y = 16;
    private static final int CELL = 18;

    private Button dropsButton;

    public BorerScreen(BorerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 169;
        inventoryLabelY = 10000;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.rotarycraft.borer.reset"), b -> click(BorerMenu.RESET))
                .bounds(leftPos + 14, topPos + 115, 72, 20).build());
        dropsButton = addRenderableWidget(Button.builder(dropsLabel(), b -> click(BorerMenu.TOGGLE_DROPS)).bounds(leftPos + 90, topPos + 115, 72, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.rotarycraft.borer.toggle_all"), b -> click(BorerMenu.TOGGLE_ALL))
                .bounds(leftPos + 14, topPos + 140, 148, 20).build());
    }

    private Component dropsLabel() {
        return Component.translatable(menu.drops() ? "gui.rotarycraft.borer.drops_on" : "gui.rotarycraft.borer.drops_off");
    }

    private void click(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int col = (int) Math.floor((mouseX - leftPos - GRID_X) / CELL);
        int row = (int) Math.floor((mouseY - topPos - GRID_Y) / CELL);
        if (col >= 0 && col < BorerBlockEntity.COLS && row >= 0 && row < BorerBlockEntity.ROWS) {
            click(col * BorerBlockEntity.ROWS + row);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (dropsButton != null) {
            dropsButton.setMessage(dropsLabel());
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int col = 0; col < BorerBlockEntity.COLS; col++) {
            for (int row = 0; row < BorerBlockEntity.ROWS; row++) {
                boolean on = menu.cell(col, row);
                boolean own = col == 3 && row == 4;
                int u = (own ? 36 : 0) + (on ? 0 : 18);
                g.blit(BUTTONS, leftPos + GRID_X + col * CELL, topPos + GRID_Y + row * CELL, u, 0, CELL, CELL);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, 0x404040, false);
        if (menu.worn()) {
            Component worn = Component.translatable("gui.rotarycraft.borer.worn");
            g.drawString(font, worn, imageWidth - 8 - font.width(worn), 5, 0xA02020, false);
        } else if (menu.jammed()) {
            Component jam = Component.translatable("gui.rotarycraft.borer.jammed");
            g.drawString(font, jam, imageWidth - 8 - font.width(jam), 5, 0xA02020, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= GRID_X && mx < GRID_X + BorerBlockEntity.COLS * CELL && my >= GRID_Y && my < GRID_Y + BorerBlockEntity.ROWS * CELL) {
            Component need = menu.requiredPower() < 0
                    ? Component.translatable("gui.rotarycraft.borer.blocked")
                    : Component.translatable("gui.rotarycraft.borer.required", MeterItem.formatWatts(menu.requiredPower()), menu.requiredTorque());
            Component have = Component.translatable("gui.rotarycraft.borer.supplied", MeterItem.formatWatts(menu.power()), menu.torque());
            g.renderTooltip(font, java.util.List.of(need, have, Component.translatable("gui.rotarycraft.borer.slice", menu.step())).stream()
                    .map(Component::getVisualOrderText).toList(), mouseX, mouseY);
        }
    }
}
