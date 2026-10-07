package net.scwunge.rotarycraft.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.scwunge.rotarycraft.RotaryCraft;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Handbook: chapters along the top, the pages of the chapter on the left, and the page on the right with its machines and items; select one to see how it is
 * crafted (read from the recipes the game has loaded). The pages are in assets/rotarycraft/handbook/structure.json, their words in the language file.
 */
public class HandbookScreen extends Screen {
    private static final ResourceLocation STRUCTURE = RotaryCraft.id("handbook/structure.json");
    private static final int PAGE_LIST = 116;
    private static int lastChapter;
    private static int lastPage;

    record Entry(String id, Item item) {
        Component name() {
            return Component.translatable("handbook.rotarycraft.entry." + id + ".name");
        }

        Component text() {
            return Component.translatable("handbook.rotarycraft.entry." + id + ".text");
        }
    }

    record Page(String id, int paragraphs, List<Entry> entries) {
        Component title() {
            return Component.translatable("handbook.rotarycraft.page." + id + ".title");
        }
    }

    record Chapter(String id, Item icon, List<Page> pages) {
        Component title() {
            return Component.translatable("handbook.rotarycraft.chapter." + id);
        }
    }

    /** One line of the page being read, and what clicking it does. */
    private interface Row {
        int height();

        void draw(GuiGraphics g, int x, int y, int width);
    }

    private static List<Chapter> chapters;

    private int chapter = lastChapter;
    private int page = lastPage;
    private int scroll;
    private Entry selected;
    private final List<Row> rows = new ArrayList<>();
    private final List<int[]> entryHits = new ArrayList<>();
    private final List<Entry> entryOrder = new ArrayList<>();
    private EditBox search;
    private List<Page> matches = List.of();

    // layout
    private int left;
    private int top;
    private int panelW;
    private int panelH;

    public HandbookScreen() {
        super(Component.translatable("handbook.rotarycraft.title"));
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new HandbookScreen());
    }

    static List<Chapter> chapters() {
        if (chapters == null) {
            List<Chapter> list = new ArrayList<>();
            try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(STRUCTURE)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (JsonElement c : root.getAsJsonArray("chapters")) {
                    JsonObject co = c.getAsJsonObject();
                    List<Page> pages = new ArrayList<>();
                    for (JsonElement p : co.getAsJsonArray("pages")) {
                        JsonObject po = p.getAsJsonObject();
                        List<Entry> entries = new ArrayList<>();
                        JsonArray ea = po.getAsJsonArray("entries");
                        for (JsonElement e : ea) {
                            JsonObject eo = e.getAsJsonObject();
                            entries.add(new Entry(eo.get("id").getAsString(), item(eo.get("item").getAsString())));
                        }
                        pages.add(new Page(po.get("id").getAsString(), po.get("paragraphs").getAsInt(), entries));
                    }
                    list.add(new Chapter(co.get("id").getAsString(), item(co.get("icon").getAsString()), pages));
                }
            } catch (IOException | RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().error("Could not read the handbook", e);
            }
            chapters = list;
        }
        return chapters;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    /** Called when resources reload (the language may have changed the words, not the structure; this is for a changed structure). */
    public static void forget() {
        chapters = null;
    }

    @Override
    protected void init() {
        panelW = Math.min(width - 16, 400);
        panelH = Math.min(height - 16, 240);
        left = (width - panelW) / 2;
        top = (height - panelH) / 2;
        String text = search == null ? "" : search.getValue();
        search = new EditBox(font, left + panelW - 104, top + 4, 98, 12, Component.translatable("handbook.rotarycraft.search"));
        search.setHint(Component.translatable("handbook.rotarycraft.search"));
        search.setValue(text);
        search.setResponder(this::searched);
        addRenderableWidget(search);
        searched(text);
        layout();
    }

    private void searched(String text) {
        String q = text.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) {
            matches = List.of();
            return;
        }
        List<Page> found = new ArrayList<>();
        for (Chapter c : chapters()) {
            for (Page p : c.pages()) {
                boolean hit = p.title().getString().toLowerCase(Locale.ROOT).contains(q);
                for (Entry e : p.entries()) {
                    hit |= e.name().getString().toLowerCase(Locale.ROOT).contains(q) || e.text().getString().toLowerCase(Locale.ROOT).contains(q);
                }
                for (int i = 0; i < p.paragraphs() && !hit; i++) {
                    hit = Component.translatable("handbook.rotarycraft.page." + p.id() + ".text." + i).getString().toLowerCase(Locale.ROOT).contains(q);
                }
                if (hit) {
                    found.add(p);
                }
            }
        }
        matches = found;
    }

    private boolean searching() {
        return search != null && search.getValue().trim().length() >= 2;
    }

    private List<Page> listed() {
        if (searching()) {
            return matches;
        }
        List<Chapter> all = chapters();
        return all.isEmpty() ? List.of() : all.get(Math.min(chapter, all.size() - 1)).pages();
    }

    private Page current() {
        List<Page> pages = listed();
        return pages.isEmpty() ? null : pages.get(Math.max(0, Math.min(page, pages.size() - 1)));
    }

    private int contentX() {
        return left + PAGE_LIST + 8;
    }

    private int contentW() {
        return panelW - PAGE_LIST - 16;
    }

    private int contentTop() {
        return top + 42;
    }

    private int contentBottom() {
        return top + panelH - 6;
    }

    /** Builds the rows of the current page for the width there is. */
    private void layout() {
        rows.clear();
        Page p = current();
        if (p == null) {
            return;
        }
        int w = contentW() - 8;
        rows.add(new TextRow(Component.translatable("handbook.rotarycraft.page." + p.id() + ".title").withStyle(net.minecraft.ChatFormatting.YELLOW), w, true));
        for (int i = 0; i < p.paragraphs(); i++) {
            rows.add(new Gap(3));
            for (FormattedCharSequence line : font.split(Component.translatable("handbook.rotarycraft.page." + p.id() + ".text." + i), w)) {
                rows.add(new LineRow(line, 0xDDDDDD));
            }
        }
        for (Entry e : p.entries()) {
            rows.add(new Gap(5));
            rows.add(new HeaderRow(e));
            for (FormattedCharSequence line : font.split(e.text(), w - 20)) {
                rows.add(new IndentRow(line));
            }
            if (e == selected) {
                rows.add(new Gap(3));
                rows.add(new RecipeRow(e));
            }
        }
        int total = 0;
        for (Row r : rows) {
            total += r.height();
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - (contentBottom() - contentTop()))));
    }

    private final class TextRow implements Row {
        private final FormattedCharSequence line;

        TextRow(Component text, int width, boolean big) {
            this.line = text.getVisualOrderText();
        }

        public int height() {
            return 14;
        }

        public void draw(GuiGraphics g, int x, int y, int width) {
            g.drawString(font, line, x, y, 0xFFFFFF);
        }
    }

    private record Gap(int h) implements Row {
        public int height() {
            return h;
        }

        public void draw(GuiGraphics g, int x, int y, int width) {
        }
    }

    private final class LineRow implements Row {
        private final FormattedCharSequence line;
        private final int color;

        LineRow(FormattedCharSequence line, int color) {
            this.line = line;
            this.color = color;
        }

        public int height() {
            return 10;
        }

        public void draw(GuiGraphics g, int x, int y, int width) {
            g.drawString(font, line, x, y, color);
        }
    }

    private final class IndentRow implements Row {
        private final FormattedCharSequence line;

        IndentRow(FormattedCharSequence line) {
            this.line = line;
        }

        public int height() {
            return 10;
        }

        public void draw(GuiGraphics g, int x, int y, int width) {
            g.drawString(font, line, x + 20, y, 0xBBBBBB);
        }
    }

    private final class HeaderRow implements Row {
        private final Entry entry;

        HeaderRow(Entry entry) {
            this.entry = entry;
        }

        public int height() {
            return 18;
        }

        public void draw(GuiGraphics g, int x, int y, int width) {
            if (entry == selected) {
                g.fill(x - 2, y - 1, x + width, y + 17, 0x55FFFFFF);
            }
            g.renderItem(new ItemStack(entry.item()), x, y);
            g.drawString(font, entry.name().copy().withStyle(net.minecraft.ChatFormatting.AQUA), x + 20, y + 5, 0xFFFFFF);
        }
    }

    private final class RecipeRow implements Row {
        private final Entry entry;
        private final RecipeHolder<CraftingRecipe> recipe;

        RecipeRow(Entry entry) {
            this.entry = entry;
            RecipeHolder<CraftingRecipe> found = null;
            var level = Minecraft.getInstance().level;
            if (level != null) {
                for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                    ItemStack result = holder.value().getResultItem(level.registryAccess());
                    if (result.is(entry.item()) && !holder.value().getIngredients().isEmpty()) {
                        found = holder;
                        break;
                    }
                }
            }
            recipe = found;
        }

        public int height() {
            return recipe == null ? 12 : 58;
        }

        public void draw(GuiGraphics g, int x, int y, int width) {
            if (recipe == null) {
                g.drawString(font, Component.translatable("handbook.rotarycraft.no_recipe"), x + 20, y, 0x999999);
                return;
            }
            var level = Minecraft.getInstance().level;
            int gridW = recipe.value() instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            List<Ingredient> ingredients = recipe.value().getIngredients();
            long tick = System.currentTimeMillis() / 1000;
            for (int i = 0; i < 9; i++) {
                int sx = x + 20 + (i % 3) * 18;
                int sy = y + (i / 3) * 18;
                g.fill(sx, sy, sx + 17, sy + 17, 0xFF2B2B33);
                g.renderOutline(sx, sy, 17, 17, 0xFF5A5A70);
            }
            for (int i = 0; i < ingredients.size(); i++) {
                ItemStack[] options = ingredients.get(i).getItems();
                if (options.length == 0) {
                    continue;
                }
                int column = i % gridW;
                int row = i / gridW;
                g.renderItem(options[(int) (tick % options.length)], x + 21 + column * 18, y + 1 + row * 18);
            }
            g.drawString(font, "->", x + 20 + 56, y + 22, 0xAAAAAA);
            ItemStack result = recipe.value().getResultItem(level.registryAccess());
            g.renderItem(result, x + 20 + 74, y + 19);
            if (result.getCount() > 1) {
                g.renderItemDecorations(font, result, x + 20 + 74, y + 19);
            }
        }
    }

    private List<Component> tip;

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        drawBook(g, mouseX, mouseY);
    }

    private void drawBook(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(left, top, left + panelW, top + panelH, 0xEE101018);
        g.renderOutline(left, top, panelW, panelH, 0xFF5A5A70);
        g.drawString(font, title, left + 6, top + 6, 0xFFFFFF);
        // chapter tabs
        List<Chapter> all = chapters();
        tip = null;
        for (int i = 0; i < all.size(); i++) {
            int tx = left + 6 + i * 22;
            int ty = top + 20;
            boolean on = !searching() && i == chapter;
            g.fill(tx - 2, ty - 2, tx + 18, ty + 18, on ? 0xFF3A3A5A : 0xFF1C1C28);
            g.renderOutline(tx - 2, ty - 2, 20, 20, on ? 0xFFFFFF55 : 0xFF3A3A50);
            g.renderItem(new ItemStack(all.get(i).icon()), tx, ty);
            if (mouseX >= tx - 2 && mouseX < tx + 18 && mouseY >= ty - 2 && mouseY < ty + 18) {
                tip = List.of(all.get(i).title());
            }
        }
        // page list
        List<Page> pages = listed();
        if (pages.isEmpty() && searching()) {
            g.drawString(font, Component.translatable("handbook.rotarycraft.no_results"), left + 6, contentTop() + 2, 0x999999);
        }
        for (int i = 0; i < pages.size(); i++) {
            int py = contentTop() + i * 12;
            if (py + 10 > contentBottom()) {
                break;
            }
            boolean on = i == page;
            if (on) {
                g.fill(left + 4, py - 1, left + PAGE_LIST, py + 10, 0x66FFFFFF);
            }
            g.drawString(font, font.plainSubstrByWidth(pages.get(i).title().getString(), PAGE_LIST - 10), left + 6, py + 1, on ? 0xFFFFFF : 0xBBBBBB);
        }
        // the page
        int cx = contentX();
        int cw = contentW();
        g.enableScissor(cx - 2, contentTop(), cx + cw, contentBottom());
        int y = contentTop() - scroll;
        entryHits.clear();
        entryOrder.clear();
        for (Row row : rows) {
            if (row instanceof HeaderRow header) {
                entryHits.add(new int[] {y, y + row.height()});
                entryOrder.add(header.entry);
            }
            if (y + row.height() > contentTop() && y < contentBottom()) {
                row.draw(g, cx, y, cw - 8);
            }
            y += row.height();
        }
        g.disableScissor();
        // a scroll bar
        int total = y + scroll - contentTop();
        int visible = contentBottom() - contentTop();
        if (total > visible) {
            int barH = Math.max(12, visible * visible / total);
            int barY = contentTop() + (visible - barH) * scroll / (total - visible);
            g.fill(cx + cw - 3, barY, cx + cw, barY + barH, 0x99AAAAAA);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = contentX();
        int cw = contentW();
        if (tip != null) {
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        } else {
            for (int i = 0; i < entryHits.size(); i++) {
                int[] hit = entryHits.get(i);
                if (mouseX >= cx && mouseX < cx + cw && mouseY >= Math.max(hit[0], contentTop()) && mouseY < Math.min(hit[1], contentBottom())) {
                    g.renderTooltip(font, new ItemStack(entryOrder.get(i).item()), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        List<Chapter> all = chapters();
        for (int i = 0; i < all.size(); i++) {
            int tx = left + 6 + i * 22;
            int ty = top + 20;
            if (mx >= tx - 2 && mx < tx + 18 && my >= ty - 2 && my < ty + 18) {
                search.setValue("");
                chapter = lastChapter = i;
                page = lastPage = 0;
                scroll = 0;
                selected = null;
                layout();
                return true;
            }
        }
        List<Page> pages = listed();
        for (int i = 0; i < pages.size(); i++) {
            int py = contentTop() + i * 12;
            if (mx >= left + 4 && mx < left + PAGE_LIST && my >= py - 1 && my < py + 10) {
                page = i;
                if (!searching()) {
                    lastPage = i;
                }
                scroll = 0;
                selected = null;
                layout();
                return true;
            }
        }
        for (int i = 0; i < entryHits.size(); i++) {
            int[] hit = entryHits.get(i);
            if (mx >= contentX() && mx < contentX() + contentW() && my >= Math.max(hit[0], contentTop()) && my < Math.min(hit[1], contentBottom())) {
                selected = selected == entryOrder.get(i) ? null : entryOrder.get(i);
                layout();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 20);
        layout();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
