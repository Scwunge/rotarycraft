# RotaryCraft (1.21.1 NeoForge)

A port of Reika Kalseki's RotaryCraft to Minecraft 1.21.1 on NeoForge: realistic mechanical power. Engines produce torque
and angular speed, shafts and gearboxes carry and transform it, and machines use it. Power is torque x speed, in watts.

## Status: phases 1-2 done, phase 3 (processing machines) in progress

| Block / item | What it does |
|---|---|
| DC Electric Engine | Runs on a redstone signal: 4 N*m at 256 rad/s (1 kW), out of its front |
| Wind Engine | 8 N*m at up to 1024 rad/s. Needs the 3x3 around its blades (back side) clear; obstructions in a 32-block cone behind it slow it down |
| Steam Engine | 32 N*m at 512 rad/s. Needs water (buckets or pipes) and fire below it; runs from 100 C. Lava below overheats it past 150 C and it bursts |
| Gas Engine | 128 N*m at 512 rad/s (65.5 kW) on ethanol (`c:ethanol`): 10 mB every 12 ticks at speed, four times that while spinning up; a 240-bucket tank filled by buckets, pipes or ethanol crystals in its slot. Stops when drowned. Original GUI |
| Performance Engine | 256 N*m at 1024 rad/s (262 kW) on ethanol plus additives (redstone 1, gunpowder 2, blaze powder 4; each 10 mB burned has a 1 in 30 chance to use one); without additives it gives Gas Engine power. Burns 10 mB every 6 ticks. Heats 1 C a second while running unless it has water (20 mB a second); above 240 C it explodes into scrap (nine scrap melt back into steel in the Blast Furnace). Original GUI |
| Microturbine | 16 N*m at 131072 rad/s (2.1 MW) on jet fuel (`c:jet_fuel`, from the Fractionation Unit), 10 mB every 48 ticks; takes about 90 s to spin up. Needs air. Original GUI |
| AC Electric Engine | 512 N*m at 256 rad/s (131 kW). Needs an alternating redstone signal (a clock that changes at least every 3 ticks) and a magnetized shaft core, which loses 1 uT every 30 s of running (tungsten cores half as often) |
| Magnetizer | Needs 2048 rad/s and 16 kW plus an alternating signal. Charges a shaft core by 1 uT a cycle (half the cycles for steel cores), up to speed / 2 uT |
| Flywheels (wood, stone, iron, gold, bedrock) | Store rotation: spin up with torque / inertia, pass on up to their rated torque, coast down when the input stops. Need at least a quarter of their rating to turn; overspun ones burst |
| Clutch | Passes power while powered by redstone (right-click to invert) |
| Shaft Junction | Merge (back + branch into the front: matching speeds add torque) or split (front + branch, torque divided evenly or 1/2 ... 1/32). Right-click: ratio, sneak-right-click: mode |
| Bevel Gears | Turn a power line through any angle (screwdriver: right-click turns the output, sneak-right-click the input) |
| Shafts (wood, stone, steel, diamond, bedrock) | Carry power unchanged. Break when torque or speed exceeds the material's limit (bedrock never breaks) |
| Gearboxes (wood, stone, steel, tungsten, diamond, bedrock; 2:1 to 16:1) | Reduction (slower, more torque) or acceleration (faster, less torque); right-click with an empty hand to switch and see wear and lubricant. Power is conserved, less 1% of torque per point of wear. Stone, steel and tungsten need lubricant (8, 24, 24 buckets; buckets or pipes); run dry they wear. Diamond holds a bucket it never uses; wood needs none but heats up and wears when hot (and can catch fire); bedrock never wears or breaks. Repair with a gear of the same material; fit a better bearing (up to two tiers up) to use less lubricant. Each breaks like a shaft of its material when overloaded. Wear and lubricant stay with the item when broken |
| Dynamometer | Passes power through; comparators read it (logarithmic) |
| Grinder | Needs 128 N*m and 4 kW. Grinds stone, gravel, glass, bricks, wood (into sawdust), wheat (flour), coal (coal dust), flowers (6 dye), bone, blaze rods and more; faster shafts grind faster (840 - 60 x log2(speed) ticks). Original GUI; hoppers fill the input and empty the output. Recipes are data-driven (`rotarycraft:grinding`) |
| Extractor | The original's 4-stage ore processor: ore -> dust -> slurry -> solution -> flakes, each stage with its own power need (512 N*m/64 kW, 2048 rad/s/16 kW, 8192 rad/s/32 kW, 256 N*m/64 kW) and a 50% chance to double (80% nether, 90% rare ores), so one ore averages about 5 ingots. Stages 2-3 use water. Bonus items as in the original (iron -> tungsten, gold -> silver, copper -> gold, coal -> gunpowder, lead -> nickel). Flakes smelt in a furnace into whatever ingot the pack has for that metal (`c:ingots/...`). Ore types are data-driven (`rotarycraft:extraction`); 21 included, mod ores only when present |
| Blast Furnace | Works by temperature, not shaft power: heats towards its surroundings (+600 C beside lava, +200 C beside fire, cooled by water and ice) at 1-2 C a second, or fast with a Friction Heater. Iron + coal/charcoal/coke additives -> HSLA steel at 600 C (gunpowder and sand used up only occasionally, as in the original); coal -> coke at 400 C; steel + coke + redstone -> spring steel at 1000 C. Original GUI with thermometer |
| Friction Heater | Needs 32 N*m and 8 kW. Heats the machine in front to 30 + 12 x log2(speed) x log2(torque) C: steel temperature needs about 32 N*m at 1024 rad/s |
| Fermenter | Needs 32 rad/s and 1 kW, and water (50 mB a batch; it also draws from an adjacent water source). Sugar + dirt -> yeast; yeast + plant matter -> sludge (leaves and grass 2, tall flowers 4, most plants 1; tags `rotarycraft:mulch/1..8`). Fastest at 25 C (yeast) / 35 C (sludge), slow outside 20-40 C, yeast dies at 60 C. Original GUI |
| Centrifuge | Powered from below: needs 4096 rad/s and 16 kW (1200 - 60 x log2(speed) ticks a spin). Splits items into chanced outputs, with the original's recipes and chances: sludge -> clean sludge and compost (two a spin), dirt -> sand, clay, seeds; gravel -> flint and sand; magma cream -> slime and blaze powder; clay -> dirt, silicon dust, rare iron/gold flakes and water; sulfur (Mekanism's) from netherrack dust and blaze powder. 10-bucket tank for fluid outputs. Data-driven (`rotarycraft:centrifuge`) |
| Fractionation Unit | Ethanol -> jet fuel. Powered from below (8192 rad/s, 64 kW); needs one each of blaze powder, coal dust, magma cream, pink dye, netherrack dust and tar, plus a ghast tear (never used up). Each batch takes 250 mB of ethanol and one or two ingredients. Yield depends on the pressure the input torque builds (slowly): about 460 N*m holds 720 kPa, where fuel out equals ethanol in, up to 2.5x at 1000 kPa. Jet fuel comes out of the top. Original GUI |
| Rock Melter | Any shaft power from below heats it, settling about 64 x log2(power) C above its surroundings (up to 1800 C). Once hot enough for the first item in it, the power melts that item: clean sludge or ethanol crystals -> 1000 mB ethanol (180 C), stone -> lava (1000 C, 5.2 MJ), cobblestone, netherrack (600 C), stone bricks. 64-bucket tank, drained from the four sides. Melts snow and ice next to it. Data-driven (`rotarycraft:melting`) |
| Canola | A crop for hydrated farmland: ten stages, needs light 9 to grow, can't be bonemealed. Seeds drop from grass (about 1 in 48); grown plants drop 2-26 seeds. Seeds grind into husks, and husks spin into 90 mB of lubricant in the Centrifuge |
| Fluids | Ethanol, jet fuel, lubricant and liquid nitrogen, with the original's colours, densities and temperatures; placeable, with buckets and `c:` fluid tags |
| Generator | Shaft power -> Forge Energy (power / 20 W per FE per tick by default); pushes FE into neighbours |
| Electric Motor | Forge Energy -> shaft power (16 N*m at 256 rad/s by default) |
| Screwdriver | Rotates machines (sneak to face the clicked side) |
| Angular Transducer | Shows torque, speed and power at a machine |
| HSLA Steel Ingot | Structural steel (tagged `c:ingots/steel`, so any mod's steel works in recipes and packs with Almost Unified merge them); made in the Blast Furnace |

Crafting follows the original: machines are built from its components (base panels, mounts, gears, gear units, bearings and rods in six materials, shaft cores, impellers, compressors, turbines, igniters, cylinders, coils, circuit boards and the rest), and the alloys come from the Blast Furnace (silicon, silumin, spring tungsten, bedrock alloy), which also does the original's high-temperature 3x3 crafting (high-temperature combustor, diamond and bedrock gears, bedrock rods and bearings). Aluminium comes from the Extractor (a bonus from redstone and lapis ore) or any mod's aluminium. Until the pipes and reservoir are ported, recipes that need them use copper ingots, a cauldron or a bucket.

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
