package net.scwunge.rotarycraft.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

/** Checks of the machines' models: that each one the renderers draw is there, whole, with its draw program pointing at parts it has. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ModelGameTests {
    /** Model names with their textures, as the renderers ask for them. */
    private static final String[][] MODELS = {
            {"dc_engine", "engine_dc"}, {"ac_engine", "engine_ac"}, {"gas_engine", "engine_gas"}, {"performance_engine", "engine_performance"},
            {"steam_engine", "engine_steam"}, {"hydro_engine", "engine_hydro"}, {"wind_engine", "engine_wind"}, {"microturbine", "engine_micro"},
            {"jet_engine", "engine_jet"}, {"clutch", "clutch"}, {"vclutch", "clutch"}, {"flywheel", "flywheel_iron"}, {"gearbox_2", "gearbox_wood"},
            {"gearbox_4", "gearbox_steel"}, {"gearbox_8", "gearbox_diamond"}, {"gearbox_16", "gearbox_bedrock"}, {"pump", "pump"},
            {"friction_heater", "friction_heater"}, {"crystallizer", "crystallizer"}, {"refrigerator", "refrigerator"}, {"grinder", "grinder"},
            {"magnetizer", "magnetizer"}, {"fractionator", "fractionator"}, {"pulse_furnace", "pulse_furnace"}, {"rock_melter", "rock_melter"},
            {"extractor", "extractor"}, {"compactor", "compactor"}, {"centrifuge", "centrifuge"}, {"fan", "fan"}, {"sprinkler", "sprinkler"},
            {"lawn_sprinkler", "lawn_sprinkler"}, {"reservoir", "ground_hydrator"}, {"fertilizer", "fertilizer"}, {"defoliator", "defoliator"},
            {"woodcutter", "woodcutter"}, {"vacuum", "vacuum"}, {"auto_breeder", "auto_breeder"}, {"bait_box", "bait_box"}, {"mob_harvester", "mob_harvester"},
            {"spawner_controller", "spawner_controller"}, {"bevel_gear", "bevel_gear"}, {"splitter", "splitter"}, {"splitter2", "splitter_bedrock"}};
    /** The ones that stand still (no moving parts in the original). */
    private static final Set<String> STILL = Set.of("fractionator", "pulse_furnace", "extractor", "refrigerator", "sprinkler", "reservoir", "vacuum", "auto_breeder",
            "bait_box", "mob_harvester", "spawner_controller", "bevel_gear", "splitter", "splitter2");

    private static JsonObject read(String model) throws IOException {
        try (var in = ModelGameTests.class.getResourceAsStream("/assets/rotarycraft/reika_models/" + model + ".json")) {
            if (in == null) {
                throw new IOException("no model " + model);
            }
            return JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
        }
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "models")
    public static void everyMachineModelIsWholeAndHasItsTexture(GameTestHelper helper) {
        for (String[] pair : MODELS) {
            JsonObject model;
            try {
                model = read(pair[0]);
            } catch (IOException e) {
                helper.fail(e.getMessage());
                return;
            }
            JsonObject parts = model.getAsJsonObject("parts");
            helper.assertTrue(parts.size() > 3, pair[0] + " has only " + parts.size() + " parts");
            for (var part : parts.entrySet()) {
                helper.assertTrue(part.getValue().getAsJsonObject().getAsJsonArray("boxes").size() > 0, pair[0] + "/" + part.getKey() + " has no boxes");
            }
            helper.assertTrue(ModelGameTests.class.getResource("/assets/rotarycraft/textures/machine/" + pair[1] + ".png") != null, "no texture " + pair[1]);
            if (!STILL.contains(pair[0])) {
                helper.assertTrue(model.has("anim"), pair[0] + " has no draw program");
            }
            if (model.has("anim")) {
                int drawn = 0;
                int open = 0;
                for (JsonElement e : model.getAsJsonArray("anim")) {
                    JsonArray op = e.getAsJsonArray();
                    String code = op.get(0).getAsString();
                    if (code.equals("p")) {
                        drawn++;
                        helper.assertTrue(parts.has(op.get(1).getAsString()), pair[0] + " draws a part it lacks: " + op.get(1).getAsString());
                    } else if (code.equals("if")) {
                        open++;
                    } else if (code.equals("end")) {
                        open--;
                    }
                }
                helper.assertTrue(open == 0, pair[0] + " has unbalanced conditions");
                helper.assertTrue(drawn > 3, pair[0] + " draws only " + drawn + " parts");
            }
        }
        helper.succeed();
    }

    /** The moving machines' programs turn parts about an axis through a pivot, by phi: the engine's rotor, the gearbox's gears. */
    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "models")
    public static void movingModelsTurnByPhi(GameTestHelper helper) throws IOException {
        for (String model : new String[] {"dc_engine", "gearbox_2", "wind_engine", "grinder", "fan", "woodcutter", "centrifuge"}) {
            boolean turns = false;
            for (JsonElement e : read(model).getAsJsonArray("anim")) {
                JsonArray op = e.getAsJsonArray();
                if (op.get(0).getAsString().equals("r") && (op.get(2).getAsDouble() != 0 || op.get(3).getAsDouble() != 0)) {
                    turns = true;
                }
            }
            helper.assertTrue(turns, model + " never turns");
        }
        helper.succeed();
    }
}
