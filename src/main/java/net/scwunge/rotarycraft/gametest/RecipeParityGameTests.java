package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parity with the original's crafting: gametest/original_recipes.txt (written by tools/recipe_parity.py from the original's recipe file) names every result the
 * original crafted. Each one is either MAPped to the ids it is called here, all of which must have a recipe, or still PENDING (not ported), in which case it
 * must have no recipe yet, so the list is moved on to MAP when the port arrives; BY_TAG ones use another mod's item by design.
 */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RecipeParityGameTests {
    private static String norm(String s) {
        return s.toLowerCase(Locale.ROOT).replace("_", "");
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "recipe_parity")
    public static void everyOriginalRecipeIsHereOrOnTheList(GameTestHelper helper) {
        Set<String> made = new HashSet<>();
        for (RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes()) {
            var result = holder.value().getResultItem(helper.getLevel().registryAccess());
            var key = BuiltInRegistries.ITEM.getKey(result.getItem());
            if (!result.isEmpty() && key.getNamespace().equals(RotaryCraft.MOD_ID)) {
                made.add(norm(key.getPath()));
            }
        }
        List<String> problems = new ArrayList<>();
        int lines = 0;
        try (var in = RecipeParityGameTests.class.getResourceAsStream("/gametest/original_recipes.txt")) {
            if (in == null) {
                helper.fail("original_recipes.txt is missing");
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                String[] part = line.split("[|]");
                if (part.length < 3) {
                    continue;
                }
                lines++;
                switch (part[0]) {
                    case "MAP" -> {
                        for (String id : part[2].split(",")) {
                            if (!made.contains(norm(id))) {
                                problems.add(part[1] + ": no recipe makes " + id);
                            }
                        }
                    }
                    case "PENDING" -> {
                        if (made.contains(norm(part[1]))) {
                            problems.add(part[1] + " has a recipe now: move it to MAP in tools/recipe_parity.py");
                        }
                    }
                    default -> {
                    }
                }
            }
        } catch (IOException e) {
            helper.fail(e.toString());
            return;
        }
        helper.assertTrue(lines > 150, "only " + lines + " lines in the list");
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }
}
