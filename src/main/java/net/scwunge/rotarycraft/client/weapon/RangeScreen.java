package net.scwunge.rotarycraft.client.weapon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.weapon.RangeMenu;
import net.scwunge.rotarycraft.weapon.WeaponNetwork;

/** The original's range panel: the radius to set in a box, and in brackets the radius the power gives, red when it is less. */
public class RangeScreen extends AbstractContainerScreen<RangeMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/range.png");
    private EditBox input;

    public RangeScreen(RangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 46;
        titleLabelY = 5;
        inventoryLabelY = -100;
    }

    @Override
    protected void init() {
        super.init();
        input = new EditBox(font, leftPos + 90, topPos + 21, 26, 16, Component.empty());
        input.setMaxLength(3);
        input.setValue(Integer.toString(menu.host().setRange()));
        input.setFilter(s -> s.matches("\\d*"));
        input.setResponder(s -> PacketDistributor.sendToServer(new WeaponNetwork.SetRange(menu.pos(), s.isEmpty() ? 0 : Integer.parseInt(s))));
        addRenderableWidget(input);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 5, 0x404040, false);
        g.drawString(font, Component.translatable("gui.rotarycraft.range.radius"), 16, 26, 0x404040, false);
        boolean short_ = menu.host().setRange() > menu.host().maxRange();
        g.drawString(font, "(" + menu.host().range() + ")", 122, 26, short_ ? 0xFF0000 : 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
