package net.scwunge.rotarycraft.client.survey;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.survey.MobRadarBlockEntity;
import net.scwunge.rotarycraft.survey.RadarMenu;

/**
 * The Mob Radar's screen, on the original's panel: every creature in range as its face on a map centred on the radar, north up,
 * with the distances of the three rings marked.
 */
public class RadarScreen extends AbstractContainerScreen<RadarMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/mob_radar.png");
    private static final ResourceLocation ICONS = RotaryCraft.id("textures/gui/mob_icons.png");
    private static final int UNIT = 4;

    public RadarScreen(RadarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 214;
        imageHeight = 223;
        titleLabelY = -100;
        inventoryLabelY = -100;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        MobRadarBlockEntity.Scan scan = menu.scan();
        for (MobRadarBlockEntity.Blip b : scan.blips()) {
            int id = b.icon();
            int u = id < 0 ? 2 * UNIT : 2 * UNIT * (id % 16);
            int v = id < 0 ? 0 : 2 * UNIT * (id / 16);
            g.blit(ICONS, leftPos + 7 + 100 + b.dx() - UNIT / 2, topPos + 1 + 16 + 100 + b.dz() - UNIT / 2, u, v, 2 * UNIT, 2 * UNIT, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int range = menu.scan().range();
        g.drawString(font, range + "m", 109, 17, 0xAAFFAA, false);
        g.drawString(font, (int) (0.63 * range) + "m", 109, 54, 0xAAFFAA, false);
        g.drawString(font, (int) Math.ceil(0.31 * range) + "m", 109, 86, 0xAAFFAA, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
