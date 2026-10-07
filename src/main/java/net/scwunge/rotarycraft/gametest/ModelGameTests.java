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
            {"spawner_controller", "spawner_controller"}, {"bevel_gear", "bevel_gear"}, {"splitter", "splitter"}, {"splitter2", "splitter_bedrock"},
            {"electric_motor", "electric_motor"}, {"generator", "generator"},
            {"boiler", "boiler"}, {"steam_turbine", "steam_turbine"}, {"air_compressor", "air_compressor"}, {"pneumatic_engine", "pneumatic_engine"},
            {"magnetic_motor", "magnetic_motor"}, {"dynamo", "dynamo"}, {"gas_tank", "gas_tank"}, {"pipe_pump", "pipe_pump"},
            {"distiller", "distiller"}, {"fuel_enhancer", "fuel_enhancer"}, {"big_furnace", "big_furnace"}};
    /** The ones that stand still (no moving parts in the original). */
    private static final Set<String> STILL = Set.of("fractionator", "pulse_furnace", "extractor", "refrigerator", "sprinkler", "reservoir", "vacuum", "auto_breeder",
            "bait_box", "mob_harvester", "spawner_controller", "generator", "bevel_gear", "splitter", "splitter2", "gas_tank", "pipe_pump", "distiller", "big_furnace");

    /** The ones that are simply a couple of boxes in the original. */
    private static final Set<String> SIMPLE = Set.of("dynamo");

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
            helper.assertTrue(parts.size() > (SIMPLE.contains(pair[0]) ? 1 : 3), pair[0] + " has only " + parts.size() + " parts");
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
                helper.assertTrue(drawn > (SIMPLE.contains(pair[0]) ? 1 : 3), pair[0] + " draws only " + drawn + " parts");
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

    /** What clients are told: the machine's speed, and the looks of the ones whose model changes with their state. */
    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "models")
    public static void clientsAreToldTheSpeedAndLooksOfMachines(GameTestHelper helper) {
        net.minecraft.core.BlockPos at = new net.minecraft.core.BlockPos(2, 2, 2);
        helper.setBlock(at, net.scwunge.rotarycraft.registry.RotaryBlocks.DC_ENGINE.get());
        net.scwunge.rotarycraft.blockentity.DCEngineBlockEntity engine = helper.getBlockEntity(at);
        net.minecraft.nbt.CompoundTag power = new net.minecraft.nbt.CompoundTag();
        power.putInt("torque", 24);
        power.putInt("omega", 777);
        engine.loadCustomOnly(power, helper.getLevel().registryAccess());
        net.minecraft.nbt.CompoundTag sent = engine.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(sent.getInt("torque") == 24 && sent.getInt("omega") == 777, "speed not sent: " + sent);

        helper.setBlock(at, net.scwunge.rotarycraft.registry.RotaryBlocks.HYDRO_ENGINE.get());
        net.scwunge.rotarycraft.blockentity.HydroEngineBlockEntity hydro = helper.getBlockEntity(at);
        net.minecraft.nbt.CompoundTag state = new net.minecraft.nbt.CompoundTag();
        state.putBoolean("failed", true);
        state.putBoolean("bedrock", true);
        hydro.loadCustomOnly(state, helper.getLevel().registryAccess());
        net.minecraft.nbt.CompoundTag told = hydro.getUpdateTag(helper.getLevel().registryAccess());
        helper.assertTrue(told.getBoolean("failed") && told.getBoolean("bedrock"), "hydro state not sent: " + told);

        helper.setBlock(at, net.scwunge.rotarycraft.registry.RotaryBlocks.FRICTION_HEATER.get());
        net.scwunge.rotarycraft.blockentity.FrictionHeaterBlockEntity heater = helper.getBlockEntity(at);
        heater.setTemperature(1350);
        helper.assertTrue(heater.glowStage() == 4, "glow " + heater.glowStage());
        helper.assertTrue(heater.getUpdateTag(helper.getLevel().registryAccess()).getInt("glow") == 4, "glow not sent");
        heater.setTemperature(300);
        helper.assertTrue(heater.glowStage() == 0, "glow at 300");

        helper.setBlock(at, net.scwunge.rotarycraft.registry.RotaryBlocks.PULSE_FURNACE.get());
        net.scwunge.rotarycraft.blockentity.PulseFurnaceBlockEntity furnace = helper.getBlockEntity(at);
        furnace.setTemperature(10);
        helper.assertTrue(furnace.glowStage() == 0, "cold furnace glows");
        furnace.setTemperature(990);
        helper.assertTrue(furnace.glowStage() == 4, "hot furnace glow " + furnace.glowStage());
        helper.succeed();
    }
}
