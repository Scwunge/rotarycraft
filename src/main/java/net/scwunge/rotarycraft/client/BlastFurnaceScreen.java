package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.menu.BlastFurnaceMenu;

/** The original Blast Furnace GUI: thermometer on the left, progress arrow before the outputs. */
public class BlastFurnaceScreen extends AbstractContainerScreen<BlastFurnaceMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/blast_furnace.png");

    public BlastFurnaceScreen(BlastFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int arrow = Math.min(24, menu.smeltTime() * 24 / menu.operationTime());
        if (arrow > 0) {
            g.blit(TEXTURE, leftPos + 119, topPos + 34, 176, 14, arrow + 1, 16);
        }
        int temp = Math.max(0, Math.min(54, menu.temperature() * 54 / BlastFurnaceBlockEntity.MAX_TEMPERATURE));
        if (temp > 0) {
            g.blit(TEXTURE, leftPos + 11, topPos + 70 - temp, 176, 86 - temp, 10, temp);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= 10 && mx < 22 && my >= 15 && my < 71) {
            int t = menu.temperature();
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.rotarycraft.temperature", t),
                    Component.translatable(t >= BlastFurnaceBlockEntity.SMELT_TEMPERATURE ? "gui.rotarycraft.blast_furnace.hot" : "gui.rotarycraft.blast_furnace.cold",
                            BlastFurnaceBlockEntity.SMELT_TEMPERATURE)), mouseX, mouseY);
        }
    }
}
