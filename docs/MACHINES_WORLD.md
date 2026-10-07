# World, decoration and logistics machines

What each machine does. This page is written by `tools/gen_info.py` from the comments in the code, and JEI shows the same text. Machines that change blocks or
burn things (the Spillway, Self Destruct, Pile Driver, Line Builder, Block Filler, Spiller, Block Cannon, Firestarter) are off until the server turns them on in
`rotarycraft-machines.toml`.

## Aerosolizer

Nine slots take potions, which it empties into bottles and keeps as a store of up to 64 per slot (one for a potion, three for an extended one), then every living thing in the space around it, as far as the nearest solid block along each axis, gets the effect: five seconds' worth, topped up every 20 ticks. Several slots of the same potion strengthen it and top it up faster. A stored unit lasts two minutes. Instant potions are not taken. Power comes in from any side; it needs 16 kW. The comparator reads how full it is.

## Aggregator

Condenses water out of the air, into a tank of 128 buckets, which it gives to anything that takes fluid beside it. It makes the biome's humidity times torque squared (at least two) divided by (80 less 5 per doubling of the speed) mB a tick, but only while it is colder than the air round it, which needs cooling, and not above 100 C. Power comes from below: 1 N*m at 4096 rad/s and 8 kW.

## Air Gun

A blast of air along the way it faces, as far as ten blocks plus two for every doubling of the torque. Every living thing in the beam that is standing on something is thrown along it (at a quarter of log2 of the torque, blocks a tick) and up; whoever placed the gun is never moved. It needs 512 N*m and 16 kW, and fires as often as every four ticks, faster the faster the shaft turns.

## Arrow Gun

A machine gun for arrows. Loaded with up to 27 stacks, it looks along the way it faces, as far as ten blocks plus two for every doubling of the torque or the first solid block, and shoots an arrow at a speed of log2 of the torque blocks a tick whenever there is something living there (other than the player who placed it, who is left alone), as often as every four ticks, faster the faster the shaft turns. It needs 1 kW.

## Beam Mirror

A mirror that follows the sun and throws daylight along the way it faces, a line of light as far as the sun is strong (two to the power of seven times the sun's strength, up to the flood light's range) or the first opaque block, which it only does with open sky above it, and not at night. Undead in the beam catch fire. It needs no power. Light and fire both stop with the switch.

## Block Cannon

Loaded with blocks, it throws one every half second, as a falling block, at the compass bearing, elevation and speed set on its screen (speed in blocks a second), or in target mode at a block you name (it works out the speed and angle). Each block needs torque in proportion to how heavy it is and how fast it is thrown: the next power of two above speed times density, divided by four, in N*m. It takes 65 kW from any side. It is off unless the server enables it, and it will not fire a block that would land where its owner may not build.

## Block Filler

Holds a stack of one kind of block and, with power, fills the space beneath it with it, a block at a time, lowest layer first. Soft blocks take 512 W, ordinary ones 1024 W, stone 2048 W and metal 4096 W. Off unless the server enables it.

## Bucket Filler

Fills the empty buckets (and any other item that holds fluid) in its eighteen slots from its 24 bucket tank, which takes fluid from pipes at the sides, or, switched the other way with the button, empties the full ones into the tank, one a go. Filled containers come out of it when filling, empty ones when emptying. Needs 1 N*m at 512 rad/s and 2 kW from any side, and works faster the faster it turns.

## Drop Processor

Turns the blocks put in it into what they drop when they are broken, as if mined with a Fortune tool if it has been given Fortune from an enchanted book (Efficiency from a book makes it quicker). A block's drops that do not fit in the output slot wait their turn. Needs 32 N*m and 1 kW; a run takes 300 ticks less 20 for each doubling of the speed. (The original's handlers for Thaumcraft, IC2, Mystcraft and similar mods' items are not ported.)

## Filling Station

A 32 bucket tank that fills the item put in it, anything with a fluid tank of its own (jetpack fuel tanks, Decorative Tanks, buckets): its screen has a slot for the item to fill, one for a full container to empty into the tank, an output where filled items go, and an input that feeds the filling slot. It adds four mB a tick for each doubling of the speed (starting from 1 rad/s), and needs 1 kW. Fluid comes in by pipe from any side.

## Firework Machine

Fed paper, gunpowder, dyes and the extras of fireworks, it makes stars (a dye and gunpowder, with diamond for a trail, glowstone for a twinkle, and a fire charge, gold nugget, feather or head for a shape, and sometimes a second dye to fade to) and rockets from them, and launches them one after another, so long as it has the turning speed. Ready-made stars are used before new ones are made. It uses up its ingredients only some of the time, less often the more power it has over what it needs.

## Flood Light

Given 1 kW it fills the air in front of it with invisible light, as far as the first opaque block, up to 64 blocks (the range setting may raise this). A fresnel lens (right-click with it, which the light keeps) spreads the light into a cone of at most 24 blocks. Sneak with the screwdriver to make the light a visible beam. Refreshed on part of every sixteen ticks, like the original.

## Grindstone

Sharpens and repairs tools, swords and shears, a point of damage for each hundred mB of water. A tool can only be repaired so far: once it has been in a grindstone it remembers a budget of twice its durability in repairs, and each point it takes uses one, so the machine takes a tool back from automation only when it cannot repair it more. It needs 256 N*m and 16 kW, at either end, and works faster the faster it turns.

## Heater

Burns fuel from its eighteen slots to hold itself at the temperature set on its screen (up to 2000 C), taking the hottest fuel that does not overshoot (a fuel is worth its burn time over 25 degrees), and passes its heat on to a heatable machine on top of it; things on top are set alight from 240 C and the block above is heated (snow melts, water boils, flammables catch). It cools towards the surroundings. Power comes in from below.

## Igniter

Burns fuel to heat itself, up to 2500 C, and then sets alight, melts or boils things all round it, in a range that grows with its heat, a number of random spots every tick (one for every 50 degrees), and from 280 C sets the living things there on fire for a second (the player who placed it excepted). The fuels, hottest used first: wood 400 C, coal 600, blaze powder 800, a lava bucket 1200, and thermite 2500 (aluminium dust and iron). Needs 1 N*m at 1024 rad/s and 32 kW, from any side. Every spot it changes is checked against claims as its owner, and it is off unless the server enables it.

## Item Cannon

A pair of these send items between them, wherever they are: it takes the first stack it holds, and every eight ticks shoots one item (the whole stack with 512 kW) into the inventory of the Item Cannon at the coordinates and dimension set on its screen (0 is the Overworld, -1 the Nether, 1 the End), if that one is loaded and has room. Power comes in from below. It holds nine stacks.

## Item Filter

An inventory of one slot that takes only the items that match the item in its template slot, and none that are in its sixteen blacklist slots; a redstone signal turns that round, so that it takes what does not match. Pipes and hoppers feed it and empty it, while it has 1 kW of power. (The original could also pull matching items out of an Applied Energistics network; that is not ported.)

## Item Refresher

Keeps dropped items from despawning, in a cube round it: with 16 kW from any side its range is four blocks, and a block more for each kilowatt over, up to 128. Any item within reach has its lifetime topped up so it is always twenty ticks (a second) from despawning, and items lying still hop up, as the original's do.

## Lamp

Runs off a wound coil, and while lit fills the empty air around it, up to twelve blocks (the range setting) along each axis and out along the diagonals, with invisible light, a coil charge lasting 120 ticks times the coil's stiffness. A redstone signal puts it out, and stops the coil unwinding. Its light is gone when it is broken.

## Light Bridge

Lays a walkable beam of light across gaps, one block more each tick, as far as its power reaches (range is power over 32 MW times the limit) or until something solid is in the way. It needs light level 13 or more on the block above it, the original's stand-in for sunlight, and the beam is gone when it loses power or light or is broken. The beam is built as the machine's owner, so claims can stop it.

## Line Builder

A ram that pushes a line of blocks along, one step at a time, and adds a block from its nine slots to the end nearest it. The line is the run of solid blocks directly in front of it; it moves a block further out only if there is room at the far end (air, water and the like). It will not push anything with a block entity, bedrock, or anything its owner may not break. Off unless the server enables it.

## Music Box

A sequencer with sixteen channels, each a line of notes (a length, a pitch of up to 49 semitones and one of six voices) written on its screen. Without power it plays once through when it gets a redstone signal; with 1 kW or more from any side it plays over and over. Music can be put on a Music Disc (and the box loaded from one) and a demo piece is built in.

## Obsidian Maker

Driven from below at 2048 rad/s and 32768 W or more, it mixes water and lava it is piped (from the sides) into cobblestone or obsidian, by its temperature: lava heats it three degrees a second, water cools it, and obsidian forms between 550 and 750 C (fading out to either side as far as 500 and 900 C). Obsidian takes a bucket of lava and 2500 mB of water, cobblestone a bucket of water. It stops when its nine slots are full, and at 1000 C it overheats into lava.

## Particle Emitter

A decorative machine run by a wound coil (no shaft), that puffs the chosen particle above itself, three a tick, for as long as the coil lasts. The 27 particles of the original are picked on its screen; each coil charge lasts 600 ticks times the coil's stiffness.

## Pile Driver

A hammer that drives a pile down into the ground below it. Each stroke smashes the layer under the pile, a five by five square without its corners; stone becomes cobblestone, most things break, obsidian takes five hits, and the shock breaks netherrack, glowstone, glass and wool a few layers deeper. When the layer is clear it lays a length of pile there and goes on down. A stroke also kills what is in the hole (not players), throws everything within 24 blocks into the air, makes players within 15 blocks sick, and breaks glass, glowstone, plants, webs, ice and hanging things within five blocks. Power comes in at either end, it needs 80 kN*m, and 16 kW for every metre of pile plus one. The more power over that, the faster it strikes. Every block it breaks is checked against claims as its owner; it is off unless the server enables it.

## Player Detector

Gives a redstone signal while a player is within its range. The range is what the power allows (one block for every 128 W) up to the detector range setting, and no more than the range set on its screen. Its reaction time is 100 ticks less one tick for every 32 rad/s (so instant at 3200). A screwdriver switches it to analog, where the signal is the number of players in range, to fifteen. Power comes in from below.

## Purifier

Turns the steel ingots of other mods (any steel but RotaryCraft's own) into RotaryCraft steel, which it makes from up to five of them with a gunpowder and a block of sand, at 600 C or more. It heats itself from what is beside it: fire adds 200 C to the air it sits in, lava 600, water halves it and ice quarters it (and melts into water); over 1000 C it burns up. A run takes 800 ticks less 40 for each doubling of the speed. The gunpowder is used a twenty-fifth of the time and the sand a fifth. It needs 64 N*m and 16 kW from any side.

## Scale-able Chest

Nine slots at 4 kW, and a slot more for every 128 W over that, up to 972, shown 54 to a page. It opens only while powered. Switching the power on and off more than three times in a second makes it smoke and fizz, over eight spits sparks, and over ten it blows up (dropping itself with its contents). Its contents stay in it when it is broken.

## Self Destruct

A dead man's switch. Power from any side keeps it quiet; the moment that power stops, it starts shelling the ground around it, an explosion every tick at a random place within six blocks, thirty-two of them, and then a last great one of twelve at itself. Power coming back in time stops it. Off unless the server enables it; every blast is checked against claims as its owner, and respects mobGriefing.

## Smoke Detector

Runs on a wound coil, looking for fire within eight blocks. When it sees any it sounds its alarm every four ticks and gives a redstone signal; with a coil nearly spent it chirps every thirty seconds instead. Each unit of charge lasts 1200 ticks times the coil's stiffness.

## Sorting Machine

Takes the items from the inventory above it, or lying on top of it, and sends each out of the side its pattern says: the three rows of nine pattern slots on its screen are the three sides other than the one it takes power from, and an item goes out of the side whose row has that item in it, or out of the bottom if no row has. Into an inventory there if it can, otherwise as a dropped item thrown out. It sorts one item a tick at 1 kW, and up to 64 at 16 kW.

## Spiller

A 4000 mB tank that, with power, turns each bucket of fluid in it into a source block in the space beneath it, thick fluids needing more power and working more slowly. Fluid comes in at the top and sides. Off unless the server enables it.

## Spillway

An 8 bucket tank that takes the water from the side it faces, with no power. Water falling there gives 250 mB a tick without being used up, and a source with water above it gives 50 (scaled by the free water factor in the config); a pool of still water is drained a bucket a tick (the block nearest and highest first, within 64 blocks across and 24 up, and nothing the owner's claims forbid). Water that gets above the spillway is removed. Pipes take the water from underneath. Off unless the server enables it, as it removes blocks.

## Wetter

Soaks the item in its one slot in the fluid in its thousand mB tank, as the wetting recipes say (sand in lubricant becomes soul sand, cobblestone in jet fuel becomes netherrack). It needs 1 N*m at 1024 rad/s and 4 kW from below. The soaking takes the recipe's time less five ticks for each 1024 rad/s over the minimum, and goes faster as the speed rises. The item cannot be taken out while it is soaking, nor for half a second after. Fluid comes in at the sides.
