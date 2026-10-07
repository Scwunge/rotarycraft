package net.scwunge.rotarycraft.client.transmission;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.CoilMenu;
import net.scwunge.rotarycraft.transmission.AdvancedGearBlockEntity;
import net.scwunge.rotarycraft.transmission.TransmissionNetwork;

/**
 * The energy coil's screen, as the original's: the speed and torque it gives out when it has a redstone signal (kept within what it can give),
 * what it holds and the most it can, and the triangle that fills as it charges.
 */
public class CoilScreen extends AbstractContainerScreen<CoilMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/energy_coil.png");
    private static final String[] PREFIX = {"", "k", "M", "G", "T", "P", "E"};

    private EditBox speed;
    private EditBox torque;

    public CoilScreen(CoilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 105;
        inventoryLabelY = 10000;
        titleLabelY = 6;
    }

    private AdvancedGearBlockEntity coil() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof AdvancedGearBlockEntity c ? c : null;
    }

    /** A number with its SI prefix and the joule, to three places. */
    static String joules(double value) {
        int i = 0;
        double v = value;
        while (v >= 1000 && i < PREFIX.length - 1) {
            v /= 1000;
            i++;
        }
        return String.format("%.3f %sJ", v, PREFIX[i]);
    }

    @Override
    protected void init() {
        super.init();
        AdvancedGearBlockEntity coil = coil();
        speed = box(18, coil == null ? 0 : coil.releaseOmega(), false);
        torque = box(48, coil == null ? 0 : coil.releaseTorque(), true);
    }

    private EditBox box(int y, int value, boolean isTorque) {
        EditBox box = new EditBox(font, leftPos + 81, topPos + y, 56, 16, Component.empty());
        box.setMaxLength(8);
        box.setValue(String.valueOf(value));
        box.setFilter(s -> s.isEmpty() || s.matches("\\d{1,8}"));
        box.setResponder(s -> {
            if (!s.isEmpty() && box.isFocused()) {
                PacketDistributor.sendToServer(new TransmissionNetwork.CoilValue(menu.pos(), Integer.parseInt(s), isTorque));
            }
        });
        return addRenderableWidget(box);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        AdvancedGearBlockEntity coil = coil();
        if (coil != null) {
            if (!speed.isFocused()) {
                speed.setValue(String.valueOf(coil.releaseOmega()));
            }
            if (!torque.isFocused()) {
                torque.setValue(String.valueOf(coil.releaseTorque()));
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        AdvancedGearBlockEntity coil = coil();
        if (coil != null) {
            double held = coil.energy() / 20D;
            int h = (int) (held * 40 / coil.capacity());
            if (held > 0 && h == 0) {
                h = 1;
            }
            g.blit(TEXTURE, leftPos + 128, topPos + 57, 178, 2, h, 40);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int color = 0x404040;
        g.drawString(font, Component.translatable("gui.rotarycraft.coil.speed"), 6, 22, color, false);
        g.drawString(font, Component.translatable("gui.rotarycraft.coil.torque"), 6, 52, color, false);
        g.drawString(font, "rad/s", 141, 22, color, false);
        g.drawString(font, "Nm", 141, 52, color, false);
        AdvancedGearBlockEntity coil = coil();
        if (coil != null) {
            g.drawString(font, Component.translatable("gui.rotarycraft.coil.max", coil.maxEmission()), 6, 37, color, false);
            g.drawString(font, Component.translatable("gui.rotarycraft.coil.stored", joules(coil.energy() / 20D)), 6, 72, color, false);
            g.drawString(font, Component.translatable("gui.rotarycraft.coil.capacity", joules(coil.capacity())), 6, 86, color, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
