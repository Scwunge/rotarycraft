package net.scwunge.rotarycraft.client.crafting;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.crafting.CraftPattern;
import net.scwunge.rotarycraft.menu.CraftPatternMenu;

/**
 * The Craft Pattern's screen, as the original's: the 3 by 3 grid to lay a recipe out in and what it makes, the button at the top left for the
 * kind of recipe (shown by the item that stands for it), and the arrows for the input limit (shift for 64 at a time, control for 16).
 */
public class CraftPatternScreen extends AbstractContainerScreen<CraftPatternMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/craft_pattern.png");
    private static final ResourceLocation BUTTONS = RotaryCraft.id("textures/gui/borer_buttons.png");

    public CraftPatternScreen(CraftPatternMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
        inventoryLabelX = imageWidth - 8 - 100;
        titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.empty(), b -> click(CraftPatternMenu.MODE)).bounds(leftPos + 6, topPos + 6, 20, 20).build());
    }

    private void click(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX - leftPos;
        int my = (int) mouseY - topPos;
        if (mx >= 4 && mx < 28 && (my >= 42 && my < 50 || my >= 61 && my < 69)) {
            boolean up = my < 55;
            int size = Screen.hasShiftDown() ? 2 : Screen.hasControlDown() ? 1 : 0;
            click(up ? CraftPatternMenu.LIMIT_UP_1 + 2 * size : CraftPatternMenu.LIMIT_DOWN_1 + 2 * size);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        g.blit(BUTTONS, leftPos + 4, topPos + 42, 18, 110, 24, 8);
        g.blit(BUTTONS, leftPos + 4, topPos + 61, 42, 110, 24, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawCenteredString(font, menu.mode().label(), imageWidth / 2 + 10, 6, 0x404040);
        int limit = menu.limit();
        g.drawCenteredString(font, limit == CraftPattern.NO_LIMIT ? "∞" : String.valueOf(limit), 16, 52, 0x404040);
        g.drawString(font, Component.translatable("gui.rotarycraft.pattern_limit"), 6, 72, 0x404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.renderFakeItem(menu.mode().icon().getDefaultInstance(), leftPos + 8, topPos + 8);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 6 && mx < 26 && my >= 6 && my < 26) {
            g.renderTooltip(font, menu.mode().label(), mouseX, mouseY);
        }
    }
}
