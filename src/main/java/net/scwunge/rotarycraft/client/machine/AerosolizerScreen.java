package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.blockentity.AerosolizerBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMenu;

/** The Aerosolizer's screen: behind each of its nine slots a bar in the stored potion's colour, as full as the store, and the amount written over it. */
public class AerosolizerScreen extends LayoutScreen {
    public AerosolizerScreen(LayoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int slot = 3 * i + j;
                int level = menu.extra(slot);
                int x = leftPos + 62 + 18 * j;
                int y = topPos + 17 + 18 * i;
                if (level > 0) {
                    int height = Math.min(16, level / 4);
                    g.fill(x, y + 16 - height, x + 16, y + 16, 0xC0000000 | menu.extra(AerosolizerBlockEntity.SLOTS + slot) & 0xFFFFFF);
                    g.drawCenteredString(font, Integer.toString(level), x + 8, y + 5, 0x000000);
                }
            }
        }
    }
}
