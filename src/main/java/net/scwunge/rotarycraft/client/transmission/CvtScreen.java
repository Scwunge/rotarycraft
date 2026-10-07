package net.scwunge.rotarycraft.client.transmission;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.CvtMenu;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.transmission.AdvancedGearBlockEntity;
import net.scwunge.rotarycraft.transmission.AdvancedGearBlockEntity.CvtMode;
import net.scwunge.rotarycraft.transmission.TransmissionNetwork;

/**
 * The CVT's screen, as the original's. In Manual mode a box takes the ratio and a button swaps speed for torque; in Redstone mode two buttons
 * step the ratio for the signal on and off (the one in force is framed); in Auto mode a box takes the torque to aim for. The button at the top
 * right changes the mode, and the lubricant shows in the tall slot beside the belts.
 */
public class CvtScreen extends AbstractContainerScreen<CvtMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/cvt.png");
    private static final ResourceLocation TEXTURE_REDSTONE = RotaryCraft.id("textures/gui/cvt_redstone.png");

    private EditBox input;
    private CvtMode shown;

    public CvtScreen(CvtMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 240;
        imageHeight = 237;
        inventoryLabelY = 10000;
        titleLabelY = 10000;
    }

    private AdvancedGearBlockEntity gear() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof AdvancedGearBlockEntity g ? g : null;
    }

    private void click(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        AdvancedGearBlockEntity gear = gear();
        if (gear != null && gear.mode() != shown) {
            clearWidgets();
            rebuild();
        }
    }

    private static String stateText(int ratio) {
        return Math.abs(ratio) + "x " + (ratio > 0 ? "Speed" : "Torque");
    }

    private void rebuild() {
        AdvancedGearBlockEntity gear = gear();
        input = null;
        if (gear == null) {
            return;
        }
        shown = gear.mode();
        addRenderableWidget(Button.builder(Component.empty(), b -> click(CvtMenu.MODE)).bounds(leftPos + 212, topPos + 6, 20, 20).build());
        switch (shown) {
            case MANUAL -> {
                addRenderableWidget(Button.builder(Component.literal(gear.ratio() > 0 ? "Speed" : "Torque"), b -> click(CvtMenu.FLIP)).bounds(leftPos + 122, topPos + 51, 80, 20).build());
                input = new EditBox(font, leftPos + 152, topPos + 27, 26, 16, Component.empty());
                input.setMaxLength(3);
                input.setValue(String.valueOf(Math.abs(gear.ratio())));
            }
            case AUTO -> {
                input = new EditBox(font, leftPos + 152, topPos + 36, 76, 16, Component.empty());
                input.setMaxLength(9);
                input.setValue(String.valueOf(gear.targetTorque()));
            }
            case REDSTONE -> {
                addRenderableWidget(Button.builder(Component.literal(stateText(gear.stateRatio(true))), b -> click(CvtMenu.STATE_ON)).bounds(leftPos + 153, topPos + 31, 71, 20).build());
                addRenderableWidget(Button.builder(Component.literal(stateText(gear.stateRatio(false))), b -> click(CvtMenu.STATE_OFF)).bounds(leftPos + 153, topPos + 54, 71, 20).build());
            }
        }
        if (input != null) {
            input.setFilter(s -> s.isEmpty() || s.matches("\\d{1,9}"));
            input.setResponder(this::send);
            addRenderableWidget(input);
        }
    }

    private void send(String text) {
        AdvancedGearBlockEntity gear = gear();
        if (gear == null || text.isEmpty()) {
            return;
        }
        int value = Integer.parseInt(text);
        if (value == 0) {
            return;
        }
        boolean target = shown == CvtMode.AUTO;
        PacketDistributor.sendToServer(new TransmissionNetwork.CvtValue(menu.pos(), target ? value : gear.ratio() < 0 ? -value : value, target));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        AdvancedGearBlockEntity gear = gear();
        g.blit(gear != null && gear.mode() == CvtMode.REDSTONE ? TEXTURE_REDSTONE : TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (gear != null && gear.lubricant().getFluidAmount() > 0) {
            FluidStack fluid = new FluidStack(RotaryFluids.LUBRICANT.get(), 1000);
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
            int height = 48 * gear.lubricant().getFluidAmount() / AdvancedGearBlockEntity.LUBRICANT_CAPACITY;
            int tint = ext.getTintColor(fluid);
            g.setColor((tint >> 16 & 0xFF) / 255F, (tint >> 8 & 0xFF) / 255F, (tint & 0xFF) / 255F, 1);
            g.blit(leftPos + 186, topPos + 89 + 48 - height, 0, 16, height, sprite);
            g.setColor(1, 1, 1, 1);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        AdvancedGearBlockEntity gear = gear();
        if (gear == null) {
            return;
        }
        int color = 0x404040;
        switch (gear.mode()) {
            case MANUAL -> {
                g.drawString(font, "Belt Ratio:", 88, 31, color, false);
                g.drawCenteredString(font, "M", 222, 12, 0xFFFFFF);
                int max = menu.maxRatio();
                boolean over = Math.abs(gear.ratio()) > max;
                g.drawCenteredString(font, "(" + (over ? max : Math.abs(gear.ratio())) + ")", 208, 31, over ? 0xFF0000 : color);
            }
            case AUTO -> {
                g.drawString(font, "Target Torque:", 72, 40, color, false);
                g.drawString(font, "Current Input: " + gear.torqueIn() + " Nm", 90, 60, color, false);
                int r = gear.ratio();
                g.drawString(font, "Current Ratio: " + Math.abs(r) + "x (" + (r < 0 ? "Torque" : "Speed") + ")", 90, 72, color, false);
                g.renderFakeItem(RotaryParts.part("circuit_board").get().getDefaultInstance(), 214, 8);
            }
            case REDSTONE -> {
                g.renderFakeItem(new ItemStack(Items.REDSTONE_TORCH), 129, 31);
                g.renderFakeItem(new ItemStack(Items.REDSTONE), 214, 7);
                g.drawString(font, "Belt Ratio:", 74, 48, color, false);
                boolean on = gear.getLevel() != null && gear.getLevel().hasNeighborSignal(gear.getBlockPos());
                int y = on ? 34 : 57;
                g.renderOutline(130, y, 14, 14, 0xFFFF0000);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        AdvancedGearBlockEntity gear = gear();
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        if (gear != null && mx >= 185 && mx < 202 && my >= 88 && my < 149) {
            g.renderTooltip(font, Component.literal("Lubricant: " + gear.lubricant().getFluidAmount() + " / " + AdvancedGearBlockEntity.LUBRICANT_CAPACITY + " mB"), mouseX, mouseY);
        }
        if (mx >= 212 && mx < 232 && my >= 6 && my < 26) {
            g.renderTooltip(font, Component.literal("Control Mode"), mouseX, mouseY);
        }
    }
}
