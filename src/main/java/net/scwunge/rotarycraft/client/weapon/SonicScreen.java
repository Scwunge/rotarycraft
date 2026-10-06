package net.scwunge.rotarycraft.client.weapon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.SonicMenu;
import net.scwunge.rotarycraft.weapon.WeaponNetwork;
import net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity;

/** The Sonic Weapon's screen, on the original's panel: one box for the volume in decibels, red when the power cannot make it. */
public class SonicScreen extends AbstractContainerScreen<SonicMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/sonic.png");
    private final SonicWeaponBlockEntity sonic;
    private EditBox volume;

    public SonicScreen(SonicMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.sonic = menu.sonic();
        imageWidth = 176;
        imageHeight = 56;
        titleLabelY = 6;
        inventoryLabelY = -100;
    }

    @Override
    protected void init() {
        super.init();
        volume = new EditBox(font, leftPos + 82, topPos + 31, 38, 16, Component.empty());
        volume.setMaxLength(3);
        volume.setValue(Integer.toString(sonic.decibels()));
        volume.setFilter(s -> s.matches("\\d*"));
        volume.setResponder(s -> PacketDistributor.sendToServer(new WeaponNetwork.SonicVolume(sonic.getBlockPos(), s.isEmpty() ? 0 : Integer.parseInt(s))));
        addRenderableWidget(volume);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, 0x404040, false);
        g.drawString(font, Component.translatable("gui.rotarycraft.sonic.volume"), 40, 36, 0x404040, false);
        g.drawString(font, "dB", 126, 36, 0x404040, false);
        int shown = volume.getValue().isEmpty() ? 0 : Integer.parseInt(volume.getValue());
        boolean tooLoud = Math.pow(10, shown / 10D) > sonic.maxVolume();
        g.drawString(font, Component.translatable("gui.rotarycraft.sonic.max", (int) Math.floor(10 * Math.log10(Math.max(1, sonic.maxVolume())))), 8, 20,
                tooLoud ? 0xFF0000 : 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
