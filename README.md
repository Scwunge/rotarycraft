# RotaryCraft (1.21.1 NeoForge)

A port of Reika Kalseki's RotaryCraft to Minecraft 1.21.1 on NeoForge: realistic mechanical power. Engines produce torque
and angular speed, shafts and gearboxes carry and transform it, and machines use it. Power is torque x speed, in watts.

## Status: phase 1 (power transmission)

| Block / item | What it does |
|---|---|
| DC Electric Engine | Runs on a redstone signal: 4 N*m at 256 rad/s (1 kW), out of its front |
| Shafts (wood, stone, steel, diamond, bedrock) | Carry power unchanged. Break when torque or speed exceeds the material's limit (bedrock never breaks) |
| Gearboxes (2:1, 4:1, 8:1, 16:1) | Reduction (slower, more torque) or acceleration (faster, less torque); right-click to switch. Power is conserved |
| Dynamometer | Passes power through; comparators read it (logarithmic) |
| Generator | Shaft power -> Forge Energy (power / 20 W per FE per tick by default); pushes FE into neighbours |
| Electric Motor | Forge Energy -> shaft power (16 N*m at 256 rad/s by default) |
| Screwdriver | Rotates machines (sneak to face the clicked side) |
| Angular Transducer | Shows torque, speed and power at a machine |
| HSLA Steel Ingot | Structural steel (tagged `c:ingots/steel`); blast iron for now, until the mod's own Blast Furnace is ported |

Machines take power in at the back and pass it out of the front. When placed they face where you are looking.
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
