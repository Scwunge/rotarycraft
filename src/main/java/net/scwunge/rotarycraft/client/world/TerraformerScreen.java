package net.scwunge.rotarycraft.client.world;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.menu.TerraformerMenu;
import net.scwunge.rotarycraft.power.BiomeTransforms;

import java.util.ArrayList;
import java.util.List;

/**
 * The Terraformer's screen, as the original's: a column of the biomes the land can become (click one to aim for it, click it again to stop),
 * with what each change costs in water and plants beside it, and the 54 slots. The radius of the area it works, the water in its tank and
 * the power it is getting are written over the gap in the slots; hover the biome's picture for the full cost.
 */
public class TerraformerScreen extends AbstractContainerScreen<TerraformerMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/terraformer.png");
    private static final ResourceLocation BIOMES = RotaryCraft.id("textures/gui/biomes.png");
    private static final ResourceLocation BUTTONS = RotaryCraft.id("textures/gui/borer_buttons.png");
    private static final int PAGE = 5;

    private int offset;
    private List<ResourceKey<Biome>> targets = List.of();

    public TerraformerScreen(TerraformerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 240;
        imageHeight = 222;
        inventoryLabelY = 10000;
        titleLabelX = 74;
        titleLabelY = 6;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (minecraft != null && minecraft.level != null) {
            targets = TerraformerMenu.targets(minecraft.level, menu.centralId());
            offset = Math.max(0, Math.min(offset, targets.size() - PAGE));
        }
    }

    private void click(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private int onPage() {
        return Math.min(targets.size(), PAGE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX - leftPos;
        int my = (int) mouseY - topPos;
        if (mx >= 11 && mx < 35 && my >= 6 && my < 18 && offset > 0) {
            offset--;
            return true;
        }
        if (mx >= 11 && mx < 35 && my >= imageHeight - 14 && my < imageHeight - 2 && offset < targets.size() - PAGE) {
            offset++;
            return true;
        }
        for (int i = 0; i < onPage(); i++) {
            if (mx >= 8 && mx < 40 && my >= 17 + 39 * i && my < 49 + 39 * i) {
                click(i + offset);
                return true;
            }
        }
        if (my >= 127 && my < 137) {
            if (mx >= 190 && mx < 202) {
                click(TerraformerMenu.RADIUS_DOWN);
                return true;
            }
            if (mx >= 222 && mx < 234) {
                click(TerraformerMenu.RADIUS_UP);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        g.blit(BUTTONS, leftPos + 11, topPos + 6, 18, 110, 24, 12);
        g.blit(BUTTONS, leftPos + 11, topPos + imageHeight - 14, 42, 110, 24, 12);
        var biomes = minecraft.level.registryAccess().registryOrThrow(Registries.BIOME);
        var central = biomes.getHolder(menu.centralId()).flatMap(h -> h.unwrapKey()).orElse(null);
        for (int i = 0; i < onPage(); i++) {
            ResourceKey<Biome> to = targets.get(i + offset);
            int icon = BiomeTransforms.iconId(to);
            int x = leftPos + 8;
            int y = topPos + 17 + 39 * i;
            if (icon >= 0) {
                g.blit(BIOMES, x, y, 32 * (icon % 8), 32 * (icon / 8), 32, 32, 256, 256);
            }
            boolean chosen = biomes.getHolder(menu.targetId()).flatMap(h -> h.unwrapKey()).map(to::equals).orElse(false);
            if (chosen) {
                g.renderOutline(x - 1, y - 1, 34, 34, 0xFFFFFF55);
            }
            BiomeTransforms.Step step = central == null ? null : BiomeTransforms.step(central, to);
            if (step != null && step.waterMb() > 0) {
                g.fill(leftPos + 48, topPos + 17 + 39 * i, leftPos + 64, topPos + 33 + 39 * i, 0xFF3F76E4);
                g.drawCenteredString(font, Integer.toString(step.waterMb()), leftPos + 56, topPos + 21 + 39 * i, 0xFFFFFF);
            } else {
                g.drawString(font, "-", leftPos + 54, topPos + 21 + 39 * i, 0x606060, false);
            }
            if (step != null && !step.items().isEmpty()) {
                var req = step.items().get((int) ((System.nanoTime() / 500_000_000L) % step.items().size()));
                g.renderItem(new ItemStack(req.item()), leftPos + 48, topPos + 36 + 39 * i);
            } else {
                g.drawString(font, "-", leftPos + 54, topPos + 40 + 39 * i, 0x606060, false);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, "-", 193, 128, 0x404040, false);
        g.drawString(font, "+", 225, 128, 0x404040, false);
        g.drawCenteredString(font, Component.translatable("gui.rotarycraft.terraformer.radius", menu.radius()), 212, 128, 0x404040);
        g.drawString(font, Component.translatable("gui.rotarycraft.terraformer.water", menu.water()), 74, 128, 0x404040, false);
        if (!menu.signal()) {
            g.drawString(font, Component.translatable("gui.rotarycraft.terraformer.needs_signal"), 74, 6 + 9, 0xA02020, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos;
        int my = mouseY - topPos;
        for (int i = 0; i < onPage(); i++) {
            if (mx >= 8 && mx < 40 && my >= 17 + 39 * i && my < 49 + 39 * i) {
                ResourceKey<Biome> to = targets.get(i + offset);
                var biomes = minecraft.level.registryAccess().registryOrThrow(Registries.BIOME);
                var central = biomes.getHolder(menu.centralId()).flatMap(h -> h.unwrapKey()).orElse(null);
                BiomeTransforms.Step step = central == null ? null : BiomeTransforms.step(central, to);
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable(to.location().toLanguageKey("biome")));
                if (step != null) {
                    lines.add(Component.translatable("gui.rotarycraft.terraformer.power", MeterItem.formatWatts(step.power())));
                    if (step.waterMb() > 0) {
                        lines.add(Component.translatable("gui.rotarycraft.terraformer.water_cost", step.waterMb()));
                    }
                    for (var req : step.items()) {
                        lines.add(Component.translatable("gui.rotarycraft.terraformer.item", req.item().getDescription(), Math.round(req.chance() * 100)));
                    }
                }
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }
}
