package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.FractionatorBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.FractionatorMenu;

import java.util.List;

/**
 * The original Fractionation Unit GUI: the progress lines run through five sections (pixel lengths 18, 37, 38, 8, 9;
 * the third and fifth fill slower, as in the original), plus ethanol, jet fuel and pressure gauges.
 */
public class FractionatorScreen extends AbstractContainerScreen<FractionatorMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/fractionator.png");
    private static final int[] SECTION_PIXELS = {18, 37, 38, 8, 9};
    private static final float[] SECTION_SPEED = {1, 1, 0.4F, 1, 0.1F};

    public FractionatorScreen(FractionatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    /** Pixels shown of each progress section at this point of the batch. */
    static int[] sections(float progress) {
        float total = 0;
        for (int i = 0; i < SECTION_PIXELS.length; i++) {
            total += SECTION_PIXELS[i] / SECTION_SPEED[i];
        }
        int[] out = new int[SECTION_PIXELS.length];
        float at = 0;
        for (int i = 0; i < SECTION_PIXELS.length; i++) {
            float start = at / total;
            at += SECTION_PIXELS[i] / SECTION_SPEED[i];
            float end = at / total;
            float f = progress <= start ? 0 : progress >= end ? 1 : (progress - start) / (end - start);
            out[i] = (int) (f * SECTION_PIXELS[i]);
        }
        return out;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int fuel = (int) ((long) menu.fuel() * 54 / FractionatorBlockEntity.FUEL_CAPACITY);
        if (fuel > 0) {
            g.blit(TEXTURE, leftPos + 151, topPos + 70 - fuel, 179, 55 - fuel, 8, fuel);
        }
        int eth = menu.ethanol() * 54 / FractionatorBlockEntity.ETHANOL_CAPACITY;
        if (eth > 0) {
            g.blit(TEXTURE, leftPos + 124, topPos + 70 - eth, 189, 55 - eth, 8, eth);
        }
        int p = Math.min(54, menu.pressure() * 54 / FractionatorBlockEntity.MAX_PRESSURE);
        if (p > 0) {
            g.blit(TEXTURE, leftPos + 134, topPos + 70 - p, 224, 55 - p, 2, p);
        }
        int[] v = sections(Math.min(0.999F, menu.mixTime() / (float) menu.operationTime()));
        g.blit(TEXTURE, leftPos + 34, topPos + 17, 178, 59, v[0], 47);
        g.blit(TEXTURE, leftPos + 70, topPos + 17, 197, 59, v[1], 47);
        if (v[2] > 0) {
            g.blit(TEXTURE, leftPos + 107, topPos + 53 - v[2], 212, 39 - v[2], 8, v[2]);
        }
        g.blit(TEXTURE, leftPos + 115, topPos + 17, 235, 59, v[3], 34);
        g.blit(TEXTURE, leftPos + 139, topPos + 18, 200, 1, v[4], 52);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (my < 15 || my > 70) {
            return;
        }
        if (mx >= 150 && mx <= 159) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.jet_fuel", menu.fuel(), FractionatorBlockEntity.FUEL_CAPACITY), mouseX, mouseY);
        } else if (mx >= 123 && mx <= 132) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.ethanol", menu.ethanol(), FractionatorBlockEntity.ETHANOL_CAPACITY), mouseX, mouseY);
        } else if (mx >= 133 && mx <= 136) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.rotarycraft.pressure", menu.pressure()),
                    Component.translatable("gui.rotarycraft.fractionator.yield", Math.round(FractionatorBlockEntity.yieldAt(menu.pressure()) * 100)),
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())),
                    mouseX, mouseY);
        }
    }
}
