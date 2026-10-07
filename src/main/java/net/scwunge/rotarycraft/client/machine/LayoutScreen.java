package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.machine.MachineNetwork;
import java.util.ArrayList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.FluidGauge;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.LayoutMenu;

import java.util.List;

/** The screen of every machine described by a {@link GuiLayout}: the original's picture, with real fluid in its gauges and its bars filled. */
public class LayoutScreen extends AbstractContainerScreen<LayoutMenu> {
    private final GuiLayout layout;
    private final ResourceLocation texture;

    public LayoutScreen(LayoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.layout = menu.layout();
        this.texture = RotaryCraft.id("textures/gui/" + layout.name() + ".png");
        this.imageWidth = layout.width();
        this.imageHeight = layout.height();
        this.inventoryLabelX = layout.inventoryX();
        this.inventoryLabelY = layout.inventoryY() - 10;
    }

    private final List<EditBox> boxes = new ArrayList<>();

    @Override
    protected void init() {
        super.init();
        boxes.clear();
        for (GuiLayout.Field field : layout.fields()) {
            EditBox box = new EditBox(font, leftPos + field.x(), topPos + field.y(), field.width(), 16, Component.empty());
            box.setMaxLength(9);
            box.setFilter(s -> s.matches("-?\\d*"));
            box.setValue(Integer.toString(menu.extra(field.extra())));
            box.setResponder(s -> {
                if (box.isFocused()) {
                    PacketDistributor.sendToServer(new MachineNetwork.FieldValue(menu.pos(), field.index(), number(s)));
                }
            });
            boxes.add(addRenderableWidget(box));
        }
        for (GuiLayout.Button button : layout.buttons()) {
            addRenderableWidget(Button.builder(buttonLabel(button), b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button.id()))
                    .bounds(leftPos + button.x(), topPos + button.y(), button.width(), button.height()).build());
        }
    }

    private Component buttonLabel(GuiLayout.Button button) {
        return Component.translatable("gui.rotarycraft." + layout.name() + ".button" + button.id() + "." + menu.extra(button.state()));
    }

    private static int number(String s) {
        try {
            return s.isEmpty() || s.equals("-") ? 0 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int i = 0;
        for (GuiLayout.Field field : layout.fields()) {
            EditBox box = boxes.get(i++);
            if (!box.isFocused() && number(box.getValue()) != menu.extra(field.extra())) {
                box.setValue(Integer.toString(menu.extra(field.extra())));
            }
        }
        for (var widget : children()) {
            if (widget instanceof Button b) {
                for (GuiLayout.Button def : layout.buttons()) {
                    if (b.getX() == leftPos + def.x() && b.getY() == topPos + def.y()) {
                        b.setMessage(buttonLabel(def));
                    }
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        for (GuiLayout.Field field : layout.fields()) {
            if (field.label() != null) {
                g.drawString(font, Component.translatable(field.label()), field.labelX(), field.labelY(), 0x404040, false);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        if (layout.storageRows() > 0) {
            int top = layout.storageRows() * 18 + 17;
            g.blit(texture, leftPos, topPos, 0, 0, imageWidth, top);
            g.blit(texture, leftPos, topPos + top, 0, 126, imageWidth, 96);
        } else {
            g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }
        for (GuiLayout.Gauge gauge : layout.gauges()) {
            FluidGauge.draw(g, menu.fluid(gauge.tank()), menu.fluidAmount(gauge.tank()), gauge.capacity(), leftPos + gauge.x(), topPos + gauge.bottom(),
                    gauge.width(), gauge.height());
        }
        for (GuiLayout.Bar bar : layout.bars()) {
            int max = menu.extra(bar.max());
            int value = Math.min(menu.extra(bar.value()), max);
            if (max <= 0 || value <= 0) {
                continue;
            }
            if (bar.horizontal()) {
                int w = (int) ((long) bar.w() * value / max);
                g.blit(texture, leftPos + bar.x(), topPos + bar.y(), bar.u(), bar.v(), w, bar.h());
            } else {
                int h = (int) ((long) bar.h() * value / max);
                g.blit(texture, leftPos + bar.x(), topPos + bar.y() + bar.h() - h, bar.u(), bar.v() + bar.h() - h, bar.w(), h);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        for (GuiLayout.Gauge gauge : layout.gauges()) {
            if (mx >= gauge.x() - 1 && mx <= gauge.x() + gauge.width() && my >= gauge.bottom() - gauge.height() - 1 && my <= gauge.bottom()) {
                g.renderTooltip(font, FluidGauge.tooltip(menu.fluid(gauge.tank()), menu.fluidAmount(gauge.tank()), gauge.capacity()), mouseX, mouseY);
                return;
            }
        }
        if (mx >= 0 && mx < imageWidth && my >= 0 && my < 14) {
            g.renderComponentTooltip(font, List.of(Component.translatable("gui.rotarycraft.power",
                    MeterItem.formatWatts((long) menu.torque() * menu.omega()), menu.torque(), menu.omega())), mouseX, mouseY);
        }
    }
}
