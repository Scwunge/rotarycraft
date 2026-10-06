package net.scwunge.rotarycraft.client.weapon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.WinderBlockEntity;
import net.scwunge.rotarycraft.item.CoilItem;
import net.scwunge.rotarycraft.menu.OneSlotMenu;
import net.scwunge.rotarycraft.weapon.WeaponNetwork;

/** The Winder's screen, on the original's one-slot background: the coil's charge, and a button to switch between winding and unwinding. */
public class WinderScreen extends AbstractContainerScreen<OneSlotMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/one_slot.png");
    private final net.minecraft.core.BlockPos pos;
    private Button mode;

    public WinderScreen(OneSlotMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.pos = menu.pos();
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    private boolean unwinding() {
        return (menu.flags() & WinderBlockEntity.FLAG_UNWINDING) != 0;
    }

    private Component modeLabel() {
        return Component.translatable(unwinding() ? "gui.rotarycraft.winder.unwinding" : "gui.rotarycraft.winder.winding");
    }

    @Override
    protected void init() {
        super.init();
        mode = addRenderableWidget(Button.builder(modeLabel(), b -> PacketDistributor.sendToServer(new WeaponNetwork.WinderMode(pos, unwinding()))).bounds(leftPos + 108, topPos + 30, 56, 20).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        mode.setMessage(modeLabel());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("item.rotarycraft.coil.charge", CoilItem.charge(menu.item()), CoilItem.MAX_CHARGE), 8, 58, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
