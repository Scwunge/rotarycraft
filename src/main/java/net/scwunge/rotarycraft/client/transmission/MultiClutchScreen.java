package net.scwunge.rotarycraft.client.transmission;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.MultiClutchMenu;
import net.scwunge.rotarycraft.transmission.MultiClutchBlockEntity;

/**
 * The Multi-Clutch's screen, as the original's: sixteen rows, one for each redstone strength (the one at the block now shown with glowstone
 * instead of redstone), each with a coloured bar naming the side power leaves by; click a bar to move on to the next side.
 */
public class MultiClutchScreen extends AbstractContainerScreen<MultiClutchMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/multi_clutch.png");
    /** The original's colour for each side (down, up, north, south, west, east). */
    private static final int[] COLORS = {0x00FFFF, 0x0000FF, 0xFFFF00, 0x000000, 0xFF7800, 0xFF00FF};
    private static final int[] TEXT = {0x000000, 0xFFFFFF, 0x000000, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF};

    public MultiClutchScreen(MultiClutchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 148;
        inventoryLabelY = 10000;
        titleLabelY = 5;
    }

    private static int barX(int i) {
        return 18 + 70 * (i / 8) + 14;
    }

    private static int barY(int i) {
        return 20 + 16 * (i % 8) - 2;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX - leftPos;
        int my = (int) mouseY - topPos;
        for (int i = 0; i < MultiClutchBlockEntity.STATES; i++) {
            if (mx >= barX(i) && mx < barX(i) + 37 && my >= barY(i) && my < barY(i) + 11) {
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
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        for (int i = 0; i < MultiClutchBlockEntity.STATES; i++) {
            int col = i / 8;
            int row = i % 8;
            g.renderFakeItem(new ItemStack(i == menu.redstone() ? Items.GLOWSTONE_DUST : Items.REDSTONE), 3 + 70 * col, 15 + 16 * row);
            g.drawString(font, String.valueOf(i), 18 + 70 * col, 20 + 16 * row, 0x404040, false);
            int side = Math.floorMod(menu.side(i), 6);
            int color = 0xFF000000 | COLORS[side];
            g.fill(barX(i), barY(i), barX(i) + 37, barY(i) + 11, side == 3 ? 0xFFFFFFFF : 0xFF000000);
            g.fill(barX(i) + 1, barY(i) + 1, barX(i) + 36, barY(i) + 10, color);
            String name = Direction.values()[side].getName();
            g.drawString(font, Character.toUpperCase(name.charAt(0)) + name.substring(1), barX(i) + 2, 20 + 16 * row, 0xFF000000 | TEXT[side], false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
