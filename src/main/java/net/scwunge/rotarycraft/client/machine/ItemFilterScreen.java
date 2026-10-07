package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.logistics.ItemFilterBlockEntity;
import net.scwunge.rotarycraft.logistics.MatchData;
import net.scwunge.rotarycraft.machine.LayoutMenu;

import java.util.List;

/**
 * The Item Filter's screen: the template slot, the buffer slot, the blacklist, and a list of what the template item is (its id, damage, mod, tags, classes and
 * component values) where each line is clicked round Match, Mismatch and Ignore. The pages are stepped with the arrows; long lists scroll with + and -.
 */
public class ItemFilterScreen extends AbstractContainerScreen<LayoutMenu> {
    private static final ResourceLocation TEXTURE = RotaryCraft.id("textures/gui/item_filter.png");
    private static final int LINES = 5;
    private MatchData.Page page = MatchData.Page.BASIC;
    private int scroll;

    public ItemFilterScreen(LayoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 217;
        inventoryLabelX = 8;
        inventoryLabelY = 124;
    }

    private ItemFilterBlockEntity filter() {
        return minecraft != null && minecraft.level != null && minecraft.level.getBlockEntity(menu.pos()) instanceof ItemFilterBlockEntity f ? f : null;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            page = page.previous();
            scroll = 0;
        }).bounds(leftPos + 30, topPos + 100, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            page = page.next();
            scroll = 0;
        }).bounds(leftPos + 50, topPos + 100, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> scroll = Math.min(scroll + 1, Math.max(0, rows().size() - lines()))).bounds(leftPos + 70, topPos + 100, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> scroll = Math.max(0, scroll - 1)).bounds(leftPos + 90, topPos + 100, 20, 20).build());
    }

    private List<MatchData.Row> rows() {
        ItemFilterBlockEntity filter = filter();
        return filter == null || filter.matchData() == null ? List.of() : filter.matchData().rows(page);
    }

    /** How many lines of the list show: the components page keeps the first for its set-all squares. */
    private int lines() {
        return page == MatchData.Page.COMPONENTS ? LINES - 1 : LINES;
    }

    private int firstLine() {
        return page == MatchData.Page.COMPONENTS ? 1 : 0;
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<MatchData.Row> rows = rows();
        for (int i = 0; i < lines() && scroll + i < rows.size(); i++) {
            if (inSquare(mouseX, mouseY, 30, 18 + (i + firstLine()) * 16)) {
                press(page.ordinal() * 10000 + scroll + i);
                return true;
            }
        }
        if (page == MatchData.Page.COMPONENTS && !rows.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                if (inSquare(mouseX, mouseY, 30 + i * 10, 18)) {
                    press(200000 + page.ordinal() * 10 + i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inSquare(double mouseX, double mouseY, int x, int y) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + 9 && mouseY >= topPos + y && mouseY < topPos + y + 9;
    }

    private static int colour(MatchData.MatchType type) {
        return 0xFF000000 | type.color;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        List<MatchData.Row> rows = rows();
        for (int i = 0; i < lines() && scroll + i < rows.size(); i++) {
            int x = leftPos + 30;
            int y = topPos + 18 + (i + firstLine()) * 16;
            graphics.fill(x - 1, y - 1, x + 10, y + 10, 0xFF202020);
            graphics.fill(x, y, x + 9, y + 9, colour(rows.get(scroll + i).setting()));
        }
        if (page == MatchData.Page.COMPONENTS && !rows.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                int x = leftPos + 30 + i * 10;
                graphics.fill(x - 1, topPos + 17, x + 10, topPos + 28, 0xFF202020);
                graphics.fill(x, topPos + 18, x + 9, topPos + 27, colour(MatchData.MatchType.values()[i]));
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, page.label, 42, 6, 0x404040, false);
        graphics.drawString(font, "Blacklist", 176, 124, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 124, 0x404040, false);
        List<MatchData.Row> rows = rows();
        if (rows.isEmpty()) {
            graphics.drawString(font, filter() != null && filter().matchData() == null ? "[Put an item in the top slot]" : "[No Values]", 42, 19, 0x000000, false);
            return;
        }
        for (int i = 0; i < lines() && scroll + i < rows.size(); i++) {
            MatchData.Row row = rows.get(scroll + i);
            String label = font.plainSubstrByWidth(row.name() + " (" + row.value() + "): ", 138);
            int y = 19 + (i + firstLine()) * 16;
            graphics.drawString(font, label, 42, y, 0x000000, false);
            graphics.drawString(font, row.setting().label, 42 + font.width(label), y, row.setting().color, false);
        }
        if (page == MatchData.Page.COMPONENTS) {
            graphics.drawString(font, "Set All", 64, 19, 0x000000, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
