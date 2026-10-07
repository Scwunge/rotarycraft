package net.scwunge.rotarycraft.gametest;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.config.SoundConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parity with the original's configuration: gametest/original_config.txt (written by tools/config_parity.py from the original's ConfigRegistry) names each of its
 * options. Each is MAPped to a setting here, which must exist; or NA (does not apply, with the reason); or PENDING with the name its setting will get, which
 * must not exist yet, so the list is moved on to MAP when it is added.
 */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class ConfigParityGameTests {
    private static void collect(UnmodifiableConfig config, Set<String> keys) {
        for (var entry : config.entrySet()) {
            if (entry.getValue() instanceof UnmodifiableConfig inner) {
                collect(inner, keys);
            } else {
                keys.add(entry.getKey());
            }
        }
    }

    @GameTest(template = RotaryGameTests.TEMPLATE, timeoutTicks = 40, batch = "config_parity")
    public static void everyOriginalOptionIsHereOrOnTheList(GameTestHelper helper) {
        Set<String> keys = new HashSet<>();
        for (ModConfigSpec spec : new ModConfigSpec[] {RotaryConfig.SPEC, FarmConfig.SPEC, MachineConfig.SPEC, SoundConfig.SPEC}) {
            collect(spec.getValues(), keys);
        }
        List<String> problems = new ArrayList<>();
        int lines = 0;
        try (var in = ConfigParityGameTests.class.getResourceAsStream("/gametest/original_config.txt")) {
            if (in == null) {
                helper.fail("original_config.txt is missing");
                return;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                String[] part = line.split("[|]", -1);
                if (part.length < 3) {
                    continue;
                }
                lines++;
                switch (part[0]) {
                    case "MAP" -> {
                        if (!keys.contains(part[2])) {
                            problems.add(part[1] + ": no setting called " + part[2]);
                        }
                    }
                    case "PENDING" -> {
                        if (part.length > 3 && !part[3].isEmpty() && keys.contains(part[3])) {
                            problems.add(part[1] + " has its setting " + part[3] + " now: move it to MAP in tools/config_parity.py");
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
        helper.assertTrue(lines > 80, "only " + lines + " lines in the list");
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }
}
