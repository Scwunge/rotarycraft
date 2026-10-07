package net.scwunge.rotarycraft.client.farm;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.FarmMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * The screen of the farming machines that keep items, on the original's "basic storage" background (rows of nine slots like a chest), with what
 * the machine reports written under it.
 */
public class FarmScreen extends AbstractContainerScreen<FarmMenu> {

    public FarmScreen(FarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = menu.ui().width();
        imageHeight = menu.ui().height();
        inventoryLabelY = imageHeight - 94;
    }

    private EditBox delayBox;

    private void press(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    protected void init() {
        super.init();
        switch (menu.kind()) {
            case "blower" -> {
                for (int i = 0; i < 4; i++) {
                    int id = i;
                    addRenderableWidget(new ToggleButton(leftPos + 25 + 36 * i, topPos + 64, i, () -> menu.value(id) != 0, () -> press(id), tooltip(i)));
                }
            }
            case "spawnerController" -> {
                addRenderableWidget(Button.builder(Component.translatable("gui.rotarycraft.farm.spawner.disable"), b -> press(0))
                        .bounds(leftPos + 48, topPos + 19, 80, 20).build());
                delayBox = new EditBox(font, leftPos + 89, topPos + 47, 26, 16, Component.empty());
                delayBox.setMaxLength(3);
                delayBox.setFilter(text -> text.matches("[0-9]*"));
                delayBox.setValue(Integer.toString(menu.value(3)));
                delayBox.setResponder(text -> {
                    if (!text.isEmpty()) {
                        press(1000 + Math.min(999, Integer.parseInt(text)));
                    }
                });
                addRenderableWidget(delayBox);
            }
            default -> {
            }
        }
    }

    private Component tooltip(int index) {
        boolean on = menu.value(index) != 0;
        return switch (index) {
            case 0 -> Component.translatable(on ? "gui.rotarycraft.farm.whitelist" : "gui.rotarycraft.farm.blacklist");
            case 1 -> Component.translatable(on ? "gui.rotarycraft.farm.metadata_on" : "gui.rotarycraft.farm.metadata_off");
            case 2 -> Component.translatable(on ? "gui.rotarycraft.farm.nbt_on" : "gui.rotarycraft.farm.nbt_off");
            default -> Component.translatable(on ? "gui.rotarycraft.farm.exact" : "gui.rotarycraft.farm.tags");
        };
    }

    /** One of the original's 18 by 18 toggles, drawn from its screen's picture. */
    private final class ToggleButton extends Button {
        private final int index;
        private final java.util.function.BooleanSupplier on;
        private final Component tip;

        ToggleButton(int x, int y, int index, java.util.function.BooleanSupplier on, Runnable action, Component tip) {
            super(x, y, 18, 18, Component.empty(), b -> action.run(), DEFAULT_NARRATION);
            this.index = index;
            this.on = on;
            this.tip = tip;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            ResourceLocation texture = RotaryCraft.id("textures/gui/" + menu.ui().texture() + ".png");
            g.blit(texture, getX(), getY(), 176 + (on.getAsBoolean() ? 18 : 0), 54 + 18 * index, 18, 18);
            if (isHovered()) {
                g.renderTooltip(font, tooltip(index), mouseX, mouseY);
            }
        }
    }

    /** What the machine reports, by the kind of machine. */
    private List<Component> lines() {
        List<Component> lines = new ArrayList<>();
        String kind = menu.kind();
        long power = (long) menu.torque() * menu.omega();
        lines.add(Component.translatable("gui.rotarycraft.farm.power", menu.torque(), menu.omega(), power));
        switch (kind) {
            case "fertilizer" -> {
                lines.add(Component.translatable("gui.rotarycraft.farm.water", menu.value(0), menu.value(1)));
                lines.add(Component.translatable("gui.rotarycraft.farm.range", menu.value(2)));
                lines.add(Component.translatable("gui.rotarycraft.farm.tries", menu.value(3)));
            }
            case "defoliator" -> {
                lines.add(Component.translatable("gui.rotarycraft.farm.poison", menu.value(0), menu.value(1)));
                lines.add(Component.translatable("gui.rotarycraft.farm.range", menu.value(2)));
            }
            case "spawnerController" -> {
                lines.clear();
            }
            default -> {
            }
        }
        return lines;
    }

    /** What the machine draws on its own picture (a tank's gauge, for one). */
    private void drawOverlay(GuiGraphics g) {
        ResourceLocation texture = RotaryCraft.id("textures/gui/" + menu.ui().texture() + ".png");
        switch (menu.kind()) {
            case "defoliator" -> {
                int level = menu.value(0) * 52 / Math.max(1, menu.value(1));
                g.blit(texture, leftPos + 134, topPos + 69 - level, 177, 69 - level, 16, level);
            }
            default -> {
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = RotaryCraft.id("textures/gui/" + menu.ui().texture() + ".png");
        if (menu.rows() > 0) {
            int rows = menu.rows() * 18 + 17;
            g.blit(texture, leftPos, topPos, 0, 0, imageWidth, rows);
            g.blit(texture, leftPos, topPos + rows, 0, 126, imageWidth, 96);
        } else {
            g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            drawOverlay(g);
        }
        if (menu.kind().equals("spawnerController")) {
            boolean valid = menu.value(2) != 0;
            int color = menu.value(1) != 0 ? 0xCCCCCC : 0x404040;
            g.drawString(font, Component.translatable(valid ? "gui.rotarycraft.farm.spawner.delay" : "gui.rotarycraft.farm.spawner.no_spawner"), leftPos + 24, topPos + 51, color, false);
        }
        int y = topPos + imageHeight + 4;
        for (Component line : lines()) {
            g.drawString(font, line, leftPos + 4, y, 0xFFFFFF, true);
            y += 10;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
