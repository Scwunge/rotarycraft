# RotaryCraft (1.21.1 NeoForge)

A port of Reika Kalseki's RotaryCraft to Minecraft 1.21.1 on NeoForge: realistic mechanical power. Engines produce torque
and angular speed, shafts and gearboxes carry and transform it, and machines use it. Power is torque x speed, in watts.

## Status: phases 1-2 done, phase 3 (processing machines) in progress

| Block / item | What it does |
|---|---|
| DC Electric Engine | Runs on a redstone signal: 4 N*m at 256 rad/s (1 kW), out of its front |
| Wind Engine | 8 N*m at up to 1024 rad/s. Needs the 3x3 around its blades (back side) clear; obstructions in a 32-block cone behind it slow it down |
| Steam Engine | 32 N*m at 512 rad/s. Needs water (buckets or pipes) and fire below it; runs from 100 C. Lava below overheats it past 150 C and it bursts |
| Flywheels (wood, stone, iron, gold, bedrock) | Store rotation: spin up with torque / inertia, pass on up to their rated torque, coast down when the input stops. Need at least a quarter of their rating to turn; overspun ones burst |
| Clutch | Passes power while powered by redstone (right-click to invert) |
| Shaft Junction | Merge (back + branch into the front: matching speeds add torque) or split (front + branch, torque divided evenly or 1/2 ... 1/32). Right-click: ratio, sneak-right-click: mode |
| Bevel Gears | Turn a power line through any angle (screwdriver: right-click turns the output, sneak-right-click the input) |
| Shafts (wood, stone, steel, diamond, bedrock) | Carry power unchanged. Break when torque or speed exceeds the material's limit (bedrock never breaks) |
| Gearboxes (2:1, 4:1, 8:1, 16:1) | Reduction (slower, more torque) or acceleration (faster, less torque); right-click to switch. Power is conserved |
| Dynamometer | Passes power through; comparators read it (logarithmic) |
| Grinder | Needs 128 N*m and 4 kW. Grinds stone, gravel, glass, bricks, wood (into sawdust), wheat (flour), bone, blaze rods and more; faster shafts grind faster (840 - 60 x log2(speed) ticks). Original GUI; hoppers fill the input and empty the output. Recipes are data-driven (`rotarycraft:grinding`) |
| Extractor | The original's 4-stage ore processor: ore -> dust -> slurry -> solution -> flakes, each stage with its own power need (512 N*m/64 kW, 2048 rad/s/16 kW, 8192 rad/s/32 kW, 256 N*m/64 kW) and a 50% chance to double (80% nether, 90% rare ores), so one ore averages about 5 ingots. Stages 2-3 use water. Bonus items as in the original (iron -> tungsten, gold -> silver, copper -> gold, coal -> gunpowder, lead -> nickel). Flakes smelt in a furnace into whatever ingot the pack has for that metal (`c:ingots/...`). Ore types are data-driven (`rotarycraft:extraction`); 21 included, mod ores only when present |
| Blast Furnace | Works by temperature, not shaft power: heats towards its surroundings (+600 C beside lava, +200 C beside fire, cooled by water and ice) at 1-2 C a second, or fast with a Friction Heater. Iron + coal/charcoal/coke additives -> HSLA steel at 600 C (gunpowder and sand used up only occasionally, as in the original); coal -> coke at 400 C; steel + coke + redstone -> spring steel at 1000 C. Original GUI with thermometer |
| Friction Heater | Needs 32 N*m and 8 kW. Heats the machine in front to 30 + 12 x log2(speed) x log2(torque) C: steel temperature needs about 32 N*m at 1024 rad/s |
| Generator | Shaft power -> Forge Energy (power / 20 W per FE per tick by default); pushes FE into neighbours |
| Electric Motor | Forge Energy -> shaft power (16 N*m at 256 rad/s by default) |
| Screwdriver | Rotates machines (sneak to face the clicked side) |
| Angular Transducer | Shows torque, speed and power at a machine |
| HSLA Steel Ingot | Structural steel (tagged `c:ingots/steel`, so any mod's steel works in recipes and packs with Almost Unified merge them); made in the Blast Furnace |

Engines spin up gradually and coast down when they stop, as in the original. Machines take power in at the back and pass it out of the front. When placed they face where you are looking.
Shaft limits use the original's formulas from each material's shear and tensile strength and density.

The remaining phases (processing machines, fluids, farming, tools and armour, weapons, ReactorCraft) are in progress.
Block models are simple placeholders until the original machine renderers are ported.

## Configuration
`serverconfig/rotarycraft-server.toml` (per world, synced to clients): shaft failure on/off, watts per FE, generator and motor buffers, motor output.

## Building
```
./gradlew build
./gradlew runGameTestServer
```

## Credits
RotaryCraft, ReactorCraft and DragonAPI by Reika Kalseki, ported with permission. 1.21.1 port by Scwunge.

## Licence
MIT. See `LICENSE`.
