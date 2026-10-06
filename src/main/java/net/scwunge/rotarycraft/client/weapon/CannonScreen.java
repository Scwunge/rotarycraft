package net.scwunge.rotarycraft.client.weapon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.CannonMenu;
import net.scwunge.rotarycraft.weapon.WeaponNetwork;
import net.scwunge.rotarycraft.weapon.turret.TntCannonBlockEntity;

/**
 * The TNT Cannon's screen, on the original's background: its eleven TNT slots, and either the launch angle, compass bearing, speed
 * and fuse (with the two angle diagrams) or, in target mode, the block to hit. A button switches mode (the original used the
 * screwdriver); every change is sent to the cannon as it is made.
 */
public class CannonScreen extends AbstractContainerScreen<CannonMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/cannon.png");
    private final TntCannonBlockEntity cannon;
    private boolean targetMode;
    private EditBox phi, theta, velocity, fuse, tx, ty, tz;
    private Button mode;

    public CannonScreen(CannonMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.cannon = menu.cannon();
        imageWidth = 212;
        imageHeight = 236;
        titleLabelY = -100;
        inventoryLabelY = -100;
    }

    @Override
    protected void init() {
        super.init();
        targetMode = cannon.targetMode();
        phi = field(leftPos + 154, topPos + 92, cannon.phi());
        theta = field(leftPos + 31, topPos + 92, cannon.theta());
        velocity = field(leftPos + 154, topPos + 112, cannon.velocity());
        fuse = field(leftPos + 65, topPos + 112, cannon.fuse());
        BlockPos t = cannon.target();
        tx = targetField(topPos + 14, t.getX());
        ty = targetField(topPos + 34, t.getY());
        tz = targetField(topPos + 54, t.getZ());
        mode = addRenderableWidget(Button.builder(modeLabel(), b -> {
            targetMode = !targetMode;
            b.setMessage(modeLabel());
            update();
            send();
        }).bounds(leftPos + 78, topPos + 30, 56, 20).build());
        update();
    }

    private Component modeLabel() {
        return Component.translatable(targetMode ? "gui.rotarycraft.cannon.mode_target" : "gui.rotarycraft.cannon.mode_manual");
    }

    private EditBox field(int x, int y, int value) {
        EditBox box = new EditBox(font, x, y, 28, 16, Component.empty());
        box.setMaxLength(4);
        box.setValue(Integer.toString(value));
        box.setFilter(s -> s.matches("-?\\d*"));
        box.setResponder(s -> send());
        return addRenderableWidget(box);
    }

    private EditBox targetField(int y, int value) {
        EditBox box = new EditBox(font, leftPos + 38, y, 52, 16, Component.empty());
        box.setMaxLength(9);
        box.setValue(Integer.toString(value));
        box.setFilter(s -> s.matches("-?\\d*"));
        box.setResponder(s -> send());
        return addRenderableWidget(box);
    }

    /** Shows the fields of the current mode. */
    private void update() {
        phi.visible = theta.visible = velocity.visible = fuse.visible = !targetMode;
        tx.visible = ty.visible = tz.visible = targetMode;
    }

    private static int number(EditBox box) {
        try {
            return box.getValue().isEmpty() || box.getValue().equals("-") ? 0 : Integer.parseInt(box.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void send() {
        if (tz == null) {
            return;
        }
        PacketDistributor.sendToServer(new WeaponNetwork.CannonSettings(cannon.getBlockPos(), targetMode, number(phi), number(theta), number(velocity),
                number(fuse), new BlockPos(number(tx), number(ty), number(tz))));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (!targetMode) {
            line(g, leftPos + 16, topPos + 73, Math.toRadians(Math.min(90, number(theta))), 0x00AA00);
            line(g, leftPos + 167, topPos + 43, Math.toRadians(number(phi) - 90), 0x00AA00);
        }
    }

    /** A line of dots from (x, y) at {@code angle} from the horizontal (up and right), 28 pixels long. */
    private static void line(GuiGraphics g, int x, int y, double angle, int color) {
        for (int i = 0; i <= 28; i++) {
            int px = x + (int) Math.round(i * Math.cos(angle));
            int py = y - (int) Math.round(i * Math.sin(angle));
            g.fill(px, py, px + 1, py + 1, 0xFF000000 | color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        if (targetMode) {
            g.drawString(font, "X", 22, 18, 0xFFFFFF, false);
            g.drawString(font, "Y", 22, 38, 0xFFFFFF, false);
            g.drawString(font, "Z", 22, 58, 0xFFFFFF, false);
        } else {
            g.drawString(font, Component.translatable("gui.rotarycraft.cannon.launch_angle"), 12, 80, 0x404040, false);
            g.drawString(font, Component.translatable("gui.rotarycraft.cannon.compass_angle"), 118, 80, 0x404040, false);
            g.drawString(font, Component.translatable("gui.rotarycraft.cannon.velocity"), 112, 116, 0x404040, false);
            g.drawString(font, Component.translatable("gui.rotarycraft.cannon.fuse"), 12, 116, 0x404040, false);
        }
        g.drawString(font, Component.translatable("gui.rotarycraft.cannon.limits", cannon.maxVelocity(), cannon.maxTheta()), 8, 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
