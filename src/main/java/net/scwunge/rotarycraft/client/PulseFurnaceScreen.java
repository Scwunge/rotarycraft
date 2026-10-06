package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.PulseFurnaceBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.PulseFurnaceMenu;

import java.util.List;

/** The original Pulse Furnace GUI: water, fuel and accelerant gauges, a thermometer, flames and the progress arrow. */
public class PulseFurnaceScreen extends AbstractContainerScreen<PulseFurnaceMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/pulse_furnace.png");

    public PulseFurnaceScreen(PulseFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int cook = Math.min(10, menu.cookTime() * 10 / PulseFurnaceBlockEntity.OPERATION_TIME);
        int fuel = menu.fuel() * 52 / PulseFurnaceBlockEntity.FUEL_CAPACITY;
        int water = menu.water() * 52 / PulseFurnaceBlockEntity.WATER_CAPACITY;
        int temp = Math.max(9, Math.max(0, menu.temperature()) * 54 / PulseFurnaceBlockEntity.MAX_TEMPERATURE);
        int fire = Math.min(38, menu.smeltTick() * 38 / menu.duration());
        int accel = menu.accelerant() * 52 / PulseFurnaceBlockEntity.ACCEL_CAPACITY;
        g.blit(TEXTURE, leftPos + 131, topPos + 36, 215, 55, 4, cook);
        g.blit(TEXTURE, leftPos + 91, topPos + 68 - fuel, 248, 53 - fuel, 5, fuel);
        g.blit(TEXTURE, leftPos + 59, topPos + 68 - water, 199, 53 - water, 5, water);
        g.blit(TEXTURE, leftPos + 20, topPos + 70 - temp, 176, 55 - temp, 11, temp);
        g.blit(TEXTURE, leftPos + 115, topPos + 61 - fire, 177, 95 - fire, 9, fire);
        g.blit(TEXTURE, leftPos + 142, topPos + 61 - fire, 204, 95 - fire, 9, fire);
        g.blit(TEXTURE, leftPos + 160, topPos + 68 - accel, 227, 53 - accel, 5, accel);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (my >= 15 && my < 69 && mx >= 90 && mx < 97) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.pulse_furnace.fuel", menu.fuel(), PulseFurnaceBlockEntity.FUEL_CAPACITY), mouseX, mouseY);
        } else if (my >= 15 && my < 69 && mx >= 58 && mx < 65) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.pulse_furnace.water", menu.water(), PulseFurnaceBlockEntity.WATER_CAPACITY), mouseX, mouseY);
        } else if (my >= 15 && my < 70 && mx >= 20 && mx < 31) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.temperature_short", menu.temperature()), mouseX, mouseY);
        } else if (my >= 15 && my < 69 && mx >= 159 && mx < 166) {
            g.renderTooltip(font, Component.translatable("gui.rotarycraft.pulse_furnace.accelerant", menu.accelerant(), PulseFurnaceBlockEntity.ACCEL_CAPACITY), mouseX, mouseY);
        } else if (mx >= 112 && mx < 153 && my >= 20 && my < 62) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.rotarycraft.pulse_furnace.needs"),
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())),
                    mouseX, mouseY);
        }
    }
}
