package net.scwunge.rotarycraft.client.crafting;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.crafting.WorktableBlockEntity;
import net.scwunge.rotarycraft.menu.WorktableMenu;

/**
 * The Worktable's screen, as the original's: what the grid would make is drawn faintly in the middle output slot (click it to take it), and
 * the arrow between the grid and the outputs lights green while a craft is ready.
 */
public class WorktableScreen extends AbstractContainerScreen<WorktableMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/worktable.png");

    public WorktableScreen(WorktableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (minecraft != null && minecraft.level != null) {
            menu.refreshGhost(minecraft.level);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.ready()) {
            g.blit(TEXTURE, leftPos + 79, topPos + 35, 176, 34, 18, 18);
        }
        if (!menu.ghost().isEmpty() && !menu.getSlot(WorktableBlockEntity.MAIN_OUTPUT).hasItem()) {
            int x = leftPos + 116;
            int y = topPos + 35;
            g.renderFakeItem(menu.ghost(), x, y);
            g.fill(RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, 0x30FFFFFF);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (hoveredSlot == null && !menu.ghost().isEmpty() && mx >= 116 && mx < 132 && my >= 35 && my < 51) {
            g.renderTooltip(font, menu.ghost(), mouseX, mouseY);
        }
    }
}
