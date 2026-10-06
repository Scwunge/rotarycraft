package net.scwunge.rotarycraft.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.GrinderBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.GrinderMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;

/** The original Grinder GUI, with the current shaft power and whether it meets the machine's needs. */
public class GrinderScreen extends AbstractContainerScreen<GrinderMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/grinder.png");
    private static final int ARROW_U = 176;
    private static final int ARROW_V = 14;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;

    public GrinderScreen(GrinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int width = menu.progress() * ARROW_W / menu.operationTime();
        if (width > 0) {
            g.blit(TEXTURE, leftPos + 99, topPos + 34, ARROW_U, ARROW_V, Math.min(ARROW_W, width), ARROW_H);
        }
    }

    /** One short line above the inventory; the details are in its tooltip. */
    private static final int POWER_X = 40;
    private static final int POWER_Y = 61;

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        int torque = menu.torque();
        int omega = menu.omega();
        PowerRequirement req = GrinderBlockEntity.REQUIREMENT;
        boolean ok = req.isMetBy(torque, omega);
        String power = MeterItem.formatWatts((long) torque * omega);
        Component line = ok ? Component.literal(power)
                : Component.translatable("gui.rotarycraft.low_power", power, MeterItem.formatWatts(req.minPower()));
        g.drawString(font, line, POWER_X, POWER_Y, ok ? 0x2E7D32 : 0xB71C1C, false);
    }

    private boolean overPowerLine(int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        return x >= POWER_X && x < POWER_X + 120 && y >= POWER_Y - 1 && y < POWER_Y + 9;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        if (overPowerLine(mouseX, mouseY)) {
            PowerRequirement req = GrinderBlockEntity.REQUIREMENT;
            g.renderComponentTooltip(font, java.util.List.of(
                    Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega()),
                    Component.translatable("gui.rotarycraft.needs", req.minTorque(), MeterItem.formatWatts(req.minPower()))), mouseX, mouseY);
        }
    }
}
