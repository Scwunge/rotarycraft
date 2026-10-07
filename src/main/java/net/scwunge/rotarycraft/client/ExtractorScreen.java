package net.scwunge.rotarycraft.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.ExtractorMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;

import java.util.ArrayList;
import java.util.List;

/** The original Extractor GUI with its four stage progress bars; power and water are in the tooltip of the status line. */
public class ExtractorScreen extends AbstractContainerScreen<ExtractorMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/extractor.png");
    private static final int STATUS_X = 8;
    private static final int STATUS_Y = 73;

    public ExtractorScreen(ExtractorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 10000; // the status line takes the label's place
    }

    private int scaled(int stage, int size) {
        return Math.min(size, menu.progress(stage) * size / menu.operationTime(stage));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        // bars from the original GUI: stages 1-3 fill downward, stage 4 fills upward
        g.blit(TEXTURE, x + 29, y + 34, 176, 48, 10, scaled(0, 32));
        g.blit(TEXTURE, x + 63, y + 35, 186, 48, 14, scaled(1, 28));
        g.blit(TEXTURE, x + 99, y + 35, 200, 48, 14, scaled(2, 28));
        if (ExtractorBlockEntity.wears()) {
            // the drill slot, and how much is left of the drill in the machine
            g.fill(x + 7, y + 33, x + 25, y + 51, 0xFF8B8B8B);
            g.fill(x + 8, y + 34, x + 24, y + 50, 0xFF373737);
            g.fill(x + 8, y + 52, x + 24, y + 54, 0xFF373737);
            g.fill(x + 8, y + 52, x + 8 + 16 * Math.max(0, menu.drill()) / ExtractorBlockEntity.DRILL_LIFE, y + 54, 0xFFC69C3A);
        }
        int up = scaled(3, 32);
        g.blit(TEXTURE, x + 133, y + 49 - up, 176, 79 - up, 17, up);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY - 4, 0x404040, false);
        int running = 0;
        for (int s = 0; s < ExtractorBlockEntity.STAGES; s++) {
            if (ExtractorBlockEntity.STAGE_REQUIREMENTS[s].isMetBy(menu.torque(), menu.omega())) {
                running++;
            }
        }
        String power = MeterItem.formatWatts((long) menu.torque() * menu.omega());
        Component line = Component.translatable("gui.rotarycraft.extractor.status", power, running, menu.water() / 1000.0);
        g.drawString(font, line, STATUS_X, STATUS_Y, running > 0 ? 0x2E7D32 : 0xB71C1C, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (mx >= STATUS_X && mx < imageWidth - 8 && my >= STATUS_Y - 1 && my < STATUS_Y + 9) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.rotarycraft.power", MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega()));
            for (int s = 0; s < ExtractorBlockEntity.STAGES; s++) {
                PowerRequirement r = ExtractorBlockEntity.STAGE_REQUIREMENTS[s];
                boolean ok = r.isMetBy(menu.torque(), menu.omega());
                lines.add(Component.translatable("gui.rotarycraft.extractor.stage" + (s + 1), r.minTorque(), r.minOmega(), MeterItem.formatWatts(r.minPower()))
                        .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            lines.add(Component.translatable("gui.rotarycraft.water", menu.water(), ExtractorBlockEntity.WATER_CAPACITY));
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}
