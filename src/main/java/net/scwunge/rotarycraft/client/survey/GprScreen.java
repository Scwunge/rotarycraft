package net.scwunge.rotarycraft.client.survey;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.survey.GprBlockEntity;
import net.scwunge.rotarycraft.survey.GprMenu;
import net.scwunge.rotarycraft.survey.SurveyNetwork;

import java.util.List;

/**
 * The GPR's screen, on the original's panel: the slice of ground it has scanned, a block to every two pixels, the machine's own
 * layer at the top. [ and ] move the plane one block along the way it looks, and \ puts it back under the machine.
 */
public class GprScreen extends AbstractContainerScreen<GprMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/gpr.png");
    private static final int UNIT = 2;

    public GprScreen(GprMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 215;
        titleLabelY = -100;
        inventoryLabelY = -100;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        int amount;
        if (key == InputConstants.KEY_LBRACKET) {
            amount = 1;
        } else if (key == InputConstants.KEY_RBRACKET) {
            amount = -1;
        } else if (key == InputConstants.KEY_BACKSLASH) {
            amount = 0;
        } else {
            return super.keyPressed(key, scan, modifiers);
        }
        PacketDistributor.sendToServer(new SurveyNetwork.GprShift(menu.pos(), amount));
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        List<Integer> palette = menu.palette();
        byte[] columns = menu.columns();
        int r = menu.range();
        for (int c = 0; c <= 2 * r && (c + 1) * GprBlockEntity.MAX_HEIGHT <= columns.length; c++) {
            int x0 = leftPos + 7 + UNIT * (c - r + GprBlockEntity.MAX_RANGE);
            int runStart = 0;
            int runColor = colour(palette, columns[c * GprBlockEntity.MAX_HEIGHT]);
            for (int row = 1; row <= GprBlockEntity.MAX_HEIGHT; row++) {
                int color = row < GprBlockEntity.MAX_HEIGHT ? colour(palette, columns[c * GprBlockEntity.MAX_HEIGHT + row]) : -1;
                if (color != runColor) {
                    int y0 = topPos + 17 + UNIT * runStart;
                    g.fill(x0, y0, x0 + UNIT, topPos + 17 + UNIT * row, 0xFF000000 | runColor);
                    runStart = row;
                    runColor = color;
                }
            }
        }
    }

    private static int colour(List<Integer> palette, byte index) {
        int i = index & 255;
        return i < palette.size() ? palette.get(i) : 0;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        var centre = menu.centre();
        g.pose().pushPose();
        g.pose().scale(0.5f, 0.5f, 1);
        g.drawString(font, centre.getX() + ", " + centre.getY() + ", " + centre.getZ(), 14, 20, 0xFFFFFF, true);
        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
