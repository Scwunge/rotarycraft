package net.scwunge.rotarycraft.client.transmission;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.DistributionClutchMenu;
import net.scwunge.rotarycraft.transmission.DistributionClutchBlockEntity;
import net.scwunge.rotarycraft.transmission.TransmissionNetwork;

/**
 * The Distribution Clutch's screen, as the original's: a row for each side with a button to turn it on and a box for the torque (N*m) it asks
 * for; what is left of the input goes out of the front. The button at the top changes between GUI control and Redstone control.
 */
public class DistributionClutchScreen extends AbstractContainerScreen<DistributionClutchMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/distribution_clutch.png");
    private static final ResourceLocation BUTTONS = RotaryCraft.id("textures/gui/borer_buttons.png");

    private final EditBox[] boxes = new EditBox[4];

    public DistributionClutchScreen(DistributionClutchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 160;
        imageHeight = 110;
        inventoryLabelY = 10000;
        titleLabelY = 10000;
    }

    private DistributionClutchBlockEntity clutch() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof DistributionClutchBlockEntity c ? c : null;
    }

    private void click(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    protected void init() {
        super.init();
        DistributionClutchBlockEntity clutch = clutch();
        for (int i = 0; i < 4; i++) {
            EditBox box = new EditBox(font, leftPos + 31, topPos + 24 + 20 * i, 80, 16, Component.empty());
            box.setMaxLength(9);
            box.setFilter(s -> s.isEmpty() || s.matches("\\d{1,9}"));
            box.setValue(clutch == null ? "0" : String.valueOf(clutch.torqueRequest(DistributionClutchBlockEntity.SIDES[i])));
            box.setResponder(s -> send());
            boxes[i] = addRenderableWidget(box);
        }
        addRenderableWidget(Button.builder(Component.empty(), b -> click(DistributionClutchMenu.MODE)).bounds(leftPos + 6, topPos + 3, 148, 14).build());
    }

    private void send() {
        if (boxes[3] == null) {
            return;
        }
        int[] v = new int[4];
        for (int i = 0; i < 4; i++) {
            String text = boxes[i].getValue();
            v[i] = text.isEmpty() ? 0 : Integer.parseInt(text);
        }
        PacketDistributor.sendToServer(new TransmissionNetwork.DistributionRequests(menu.pos(), v[0], v[1], v[2], v[3]));
    }

    private static boolean usable(DistributionClutchBlockEntity clutch, Direction side) {
        return side != clutch.facing() && side != clutch.inputSide();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX - leftPos;
        int my = (int) mouseY - topPos;
        DistributionClutchBlockEntity clutch = clutch();
        for (int i = 0; i < 4 && clutch != null; i++) {
            if (mx >= 16 && mx < 25 && my >= 28 + 20 * i && my < 37 + 20 * i && usable(clutch, DistributionClutchBlockEntity.SIDES[i])) {
                click(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        DistributionClutchBlockEntity clutch = clutch();
        for (int i = 0; i < 4 && clutch != null; i++) {
            Direction side = DistributionClutchBlockEntity.SIDES[i];
            int u = 9;
            int v = 54;
            if (!usable(clutch, side)) {
                u = 0;
            } else if (clutch.isSideEnabled(side)) {
                u = 0;
                v = 63;
            }
            g.blit(BUTTONS, leftPos + 16, topPos + 28 + 20 * i, u, v, 9, 9);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        DistributionClutchBlockEntity clutch = clutch();
        for (int i = 0; i < 4; i++) {
            Direction side = DistributionClutchBlockEntity.SIDES[i];
            String name = side.getName();
            g.drawString(font, Character.toUpperCase(name.charAt(0)) + name.substring(1), 114, 28 + 20 * i, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        DistributionClutchBlockEntity clutch = clutch();
        if (clutch != null) {
            g.drawCenteredString(font, clutch.control().label(), leftPos + 80, topPos + 6, 0xFFFFFF);
            int mx = mouseX - leftPos;
            int my = mouseY - topPos;
            for (int i = 0; i < 4; i++) {
                Direction side = DistributionClutchBlockEntity.SIDES[i];
                if (mx >= 16 && mx < 25 && my >= 28 + 20 * i && my < 37 + 20 * i && !usable(clutch, side)) {
                    g.renderTooltip(font, Component.translatable(side == clutch.inputSide() ? "gui.rotarycraft.distribution_clutch.input" : "gui.rotarycraft.distribution_clutch.front"), mouseX, mouseY);
                }
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }
}
