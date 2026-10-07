package net.scwunge.rotarycraft.client.farm;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.FarmMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * The screen of the farming machines that keep items, on the original's "basic storage" background (rows of nine slots like a chest), with what
 * the machine reports written under it.
 */
public class FarmScreen extends AbstractContainerScreen<FarmMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/basic_storage.png");

    public FarmScreen(FarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 114 + menu.rows() * 18;
        inventoryLabelY = imageHeight - 94;
    }

    /** What the machine reports, by the kind of machine. */
    private List<Component> lines() {
        List<Component> lines = new ArrayList<>();
        String kind = menu.kind();
        long power = (long) menu.torque() * menu.omega();
        lines.add(Component.translatable("gui.rotarycraft.farm.power", menu.torque(), menu.omega(), power));
        switch (kind) {
            case "fertilizer" -> {
                lines.add(Component.translatable("gui.rotarycraft.farm.water", menu.value(0), menu.value(1)));
                lines.add(Component.translatable("gui.rotarycraft.farm.range", menu.value(2)));
                lines.add(Component.translatable("gui.rotarycraft.farm.tries", menu.value(3)));
            }
            default -> {
            }
        }
        return lines;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int rows = menu.rows() * 18 + 17;
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, rows);
        g.blit(TEXTURE, leftPos, topPos + rows, 0, 126, imageWidth, 96);
        int y = topPos + imageHeight + 4;
        for (Component line : lines()) {
            g.drawString(font, line, leftPos + 4, y, 0xFFFFFF, true);
            y += 10;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
