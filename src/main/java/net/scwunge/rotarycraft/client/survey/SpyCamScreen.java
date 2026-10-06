package net.scwunge.rotarycraft.client.survey;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.survey.SpyCamBlockEntity;
import net.scwunge.rotarycraft.survey.SpyCamMenu;

import java.util.List;

/**
 * The Spy Cam's screen, on the original's panel: the ground within 24 blocks as a map of coloured blocks, darker the further down
 * their top is, with the creatures on it as their faces. As the original draws it, east is down the screen and north to the right.
 */
public class SpyCamScreen extends AbstractContainerScreen<SpyCamMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/spy_cam.png");
    private static final ResourceLocation ICONS = RotaryCraft.id("textures/gui/mob_icons.png");
    private static final int UNIT = 4;

    public SpyCamScreen(SpyCamMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 222;
        imageHeight = 222;
        titleLabelY = -100;
        inventoryLabelY = -100;
    }

    /** The map's right edge, as the original: 24 cells either way of a middle at 17 + 192. */
    private static final int MAX = SpyCamBlockEntity.RANGE * 2 * UNIT;

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int[] colors = menu.colors();
        int side = SpyCamBlockEntity.SIDE;
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                int x = leftPos + 17 + MAX - UNIT * j;
                int y = topPos + 20 + UNIT * i;
                g.fill(x - UNIT, y, x, y + UNIT, 0xFF000000 | colors[i * side + j]);
            }
        }
        List<Integer> mobs = menu.mobs();
        for (int n = 0; n + 2 < mobs.size(); n += 3) {
            int i = mobs.get(n), j = mobs.get(n + 1), id = mobs.get(n + 2);
            int u = id < 0 ? 2 * UNIT : 2 * UNIT * (id % 16);
            int v = id < 0 ? 0 : 2 * UNIT * (id / 16);
            g.blit(ICONS, leftPos + 13 + MAX - UNIT * i - UNIT / 2, topPos + 20 + UNIT * j - UNIT / 2, u, v, 2 * UNIT, 2 * UNIT, 256, 256);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
