package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ParticleEmitterBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMenu;

/** The Particle Emitter's screen: a button with a picture for each particle (the original's layout; its picture is the vanilla particle sheet, 8 pixel sprites drawn at double size), the chosen one picked out. */
public class ParticleScreen extends LayoutScreen {
    private static final ResourceLocation ICONS = RotaryCraft.id("textures/gui/particle_icons.png");
    private static final ResourceLocation BUTTONS = RotaryCraft.id("textures/gui/borer_buttons.png");

    public ParticleScreen(LayoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** The button's place for the particle in list position {@code i}: six to a row either side of the coil slot, then eight to a row. */
    private static int[] place(int i) {
        int dx = i % 6;
        int dy = i / 6;
        int x = 8 + dx * 20;
        if (dx >= 3 && dy < 2) {
            x += 41;
        }
        if (dy >= 2) {
            dx = (i - 12) % 8;
            x = 8 + dx * 20;
            dy = 2 + (i - 12) / 8;
        }
        return new int[] {x, 19 + dy * 20};
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        int chosen = menu.extra(0);
        for (int i = 0; i < ParticleEmitterBlockEntity.Kind.LIST.length; i++) {
            int[] at = place(i);
            boolean hovered = mouseX - leftPos >= at[0] && mouseX - leftPos < at[0] + 18 && mouseY - topPos >= at[1] && mouseY - topPos < at[1] + 18;
            g.blit(BUTTONS, leftPos + at[0], topPos + at[1], 0, i == chosen ? 54 : hovered ? 18 : 36, 18, 18);
            int atlas = ParticleEmitterBlockEntity.Kind.LIST[i].atlasIndex;
            g.blit(ICONS, leftPos + at[0] + 1, topPos + at[1] + 1, 16, 16, 8 * (atlas % 16), 8 * (atlas / 16), 8, 8, 128, 128);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < ParticleEmitterBlockEntity.Kind.LIST.length; i++) {
            int[] at = place(i);
            if (mouseX - leftPos >= at[0] && mouseX - leftPos < at[0] + 18 && mouseY - topPos >= at[1] && mouseY - topPos < at[1] + 18) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        for (int i = 0; i < ParticleEmitterBlockEntity.Kind.LIST.length; i++) {
            int[] at = place(i);
            if (mouseX - leftPos >= at[0] && mouseX - leftPos < at[0] + 18 && mouseY - topPos >= at[1] && mouseY - topPos < at[1] + 18) {
                String name = ParticleEmitterBlockEntity.Kind.LIST[i].name();
                g.renderTooltip(font, Component.literal(name.charAt(0) + name.substring(1).toLowerCase(java.util.Locale.ROOT)), mouseX, mouseY);
            }
        }
    }
}
