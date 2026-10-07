package net.scwunge.rotarycraft.client.crafting;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.crafting.AutoCrafterBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.AutoCrafterMenu;

/**
 * The Auto-Crafter's screen, as the original's: two rows of nine patterns with what each made under it, a lamp under each pattern that
 * flashes when it crafts, and in Request mode a bar over each pattern to click for one batch. The coloured square in the corner is the mode
 * (red Request, blue Continuous): click it to change.
 */
public class AutoCrafterScreen extends AbstractContainerScreen<AutoCrafterMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/auto_crafter.png");

    public AutoCrafterScreen(AutoCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 222;
        titleLabelX = 20;
        titleLabelY = 6;
        inventoryLabelY = 129;
    }

    private static int barX(int i) {
        return 7 + (i % 9) * 18;
    }

    private static int barY(int i) {
        return i < 9 ? 13 : 75;
    }

    private static int lampY(int i) {
        return i < 9 ? 36 : 98;
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
        if (mx >= 5 && mx < 16 && my >= 5 && my < 16) {
            click(AutoCrafterMenu.MODE);
            return true;
        }
        if (menu.mode() == AutoCrafterBlockEntity.Mode.REQUEST) {
            for (int i = 0; i < AutoCrafterBlockEntity.SIZE; i++) {
                if (mx >= barX(i) && mx < barX(i) + 18 && my >= barY(i) && my < barY(i) + 4) {
                    click(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.mode() == AutoCrafterBlockEntity.Mode.REQUEST) {
            for (int i = 0; i < AutoCrafterBlockEntity.SIZE; i++) {
                g.blit(TEXTURE, leftPos + barX(i), topPos + barY(i), 176, 6, 18, 4);
            }
        }
        RenderSystem.enableBlend();
        for (int i = 0; i < AutoCrafterBlockEntity.SIZE; i++) {
            if (menu.flash(i) > 0) {
                g.setColor(1, 1, 1, Math.min(1F, menu.flash(i) / 2F));
                g.blit(TEXTURE, leftPos + barX(i), topPos + lampY(i), 176, 11, 18, 9);
            }
        }
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        int color = 0xFF000000 | menu.mode().color;
        g.fill(leftPos + 5, topPos + 5, leftPos + 16, topPos + 16, color);
        g.fill(leftPos + 5, topPos + 5, leftPos + 6, topPos + 15, 0x80FFFFFF);
        g.fill(leftPos + 5, topPos + 5, leftPos + 15, topPos + 6, 0x80FFFFFF);
        g.fill(leftPos + 6, topPos + 15, leftPos + 16, topPos + 16, 0x80000000);
        g.fill(leftPos + 15, topPos + 6, leftPos + 16, topPos + 16, 0x80000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 5 && mx < 16 && my >= 5 && my < 16) {
            g.renderComponentTooltip(font, java.util.List.of(menu.mode().label(),
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())),
                    mouseX, mouseY);
        }
    }
}
