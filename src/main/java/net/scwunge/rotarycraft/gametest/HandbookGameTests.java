package net.scwunge.rotarycraft.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.GadgetRegistry;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Tests of the Handbook's pages: every item and every word they use exists, and every machine in the machine table is on a page. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class HandbookGameTests {
    private static JsonObject read(String path) throws Exception {
        try (var in = HandbookGameTests.class.getResourceAsStream(path)) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 20)
    public static void everyHandbookPageHasItsItemsAndWords(GameTestHelper helper) throws Exception {
        JsonObject structure = read("/assets/rotarycraft/handbook/structure.json");
        JsonObject lang = read("/assets/rotarycraft/lang/en_us.json");
        Set<String> entries = new HashSet<>();
        int pages = 0;
        for (JsonElement c : structure.getAsJsonArray("chapters")) {
            JsonObject chapter = c.getAsJsonObject();
            helper.assertTrue(lang.has("handbook.rotarycraft.chapter." + chapter.get("id").getAsString()), "no title for chapter " + chapter.get("id"));
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(chapter.get("icon").getAsString())), "no icon " + chapter.get("icon"));
            for (JsonElement p : chapter.getAsJsonArray("pages")) {
                JsonObject page = p.getAsJsonObject();
                pages++;
                String id = page.get("id").getAsString();
                helper.assertTrue(lang.has("handbook.rotarycraft.page." + id + ".title"), "no title for page " + id);
                for (int i = 0; i < page.get("paragraphs").getAsInt(); i++) {
                    helper.assertTrue(lang.has("handbook.rotarycraft.page." + id + ".text." + i), "no paragraph " + i + " of page " + id);
                }
                for (JsonElement e : page.getAsJsonArray("entries")) {
                    JsonObject entry = e.getAsJsonObject();
                    String slug = entry.get("id").getAsString();
                    entries.add(slug);
                    helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(entry.get("item").getAsString())), "no item " + entry.get("item") + " on page " + id);
                    helper.assertTrue(lang.has("handbook.rotarycraft.entry." + slug + ".name") && lang.has("handbook.rotarycraft.entry." + slug + ".text"), "no words for " + slug);
                    String text = lang.get("handbook.rotarycraft.entry." + slug + ".text").getAsString();
                    helper.assertTrue(text.length() > 20 && !text.contains("{@") && !text.toLowerCase().contains("original"), "bad text for " + slug + ": " + text);
                }
            }
        }
        helper.assertTrue(pages >= 30 && entries.size() >= 120, pages + " pages, " + entries.size() + " entries");
        // the machines the machine table describes
        for (String machine : new String[] {"dc_engine", "gas_engine", "jet_engine", "grinder", "extractor", "blast_furnace", "compactor", "centrifuge", "reservoir", "railgun", "fuel_tank", "ethanol_minecart"}) {
            helper.assertTrue(entries.contains(machine), machine + " is on no page");
        }
        helper.assertTrue(GadgetRegistry.HANDBOOK.isBound(), "no handbook item");
        helper.succeed();
    }
}
