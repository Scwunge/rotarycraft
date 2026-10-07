"""The in-game Handbook: its book item, and the pages it shows (run by gen_assets.py).

Pages are written here as slugs of entries; an entry is a machine or item with a name and a description. The machine descriptions come from the machine table in
docs/MACHINES.md and from the comments of the block entity classes (the same text JEI and the info pages use), cleaned for a player; the rest is written below.
Output: assets/rotarycraft/handbook/structure.json (chapters, pages, entry items) and handbook.rotarycraft.* keys in the lang file.
"""
import json
import os
import re
import shutil

from PIL import Image

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
JAVA = 'src/main/java/net/scwunge/rotarycraft'
REF = 'reference/RotaryCraft/Textures'

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


# ---- the book item: sprite, recipe, lang ----
sheet = Image.open(REF + '/Items/items2.png').convert('RGBA')
x, y = (208 % 16) * 16, (208 // 16) * 16
sheet.crop((x, y, x + 16, y + 16)).save(A + '/textures/item/handbook.png')
w(A + '/models/item/handbook.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/handbook'}})
w(D + '/recipe/handbook.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['RIR', 'PPP', 'PPP'],
                                'key': {'R': {'item': 'minecraft:redstone'}, 'I': {'item': 'minecraft:iron_ingot'}, 'P': {'item': 'minecraft:paper'}},
                                'result': {'id': 'rotarycraft:handbook', 'count': 1}})
lang['item.rotarycraft.handbook'] = 'Handbook'

# ---- cleaning text for a player ----
SCRUB = [
    (r',? as in the original(?=[,.:;)\s]|$)', ''),
    (r'\(as in the original\)\s*', ''),
    (r',? as the original made them from one block', ''),
    (r',? as the original', ''),
    (r"[Tt]he original's 4-stage", 'A 4-stage'),
    (r'with the original\'s recipes and chances', 'with fixed recipes and chances'),
    (r"with the original's colours, densities and temperatures", 'each with its own colour, density and temperature'),
    (r"The original's pipes:", 'Pipes:'),
    (r"The original's pipe fittings", 'Pipe fittings'),
    (r'Only the machines the original allows can be cooled', 'Only some machines can be cooled'),
    (r'with the original\'s ', 'with the '),
    (r"[Tt]he original's ", ''),
    (r'\{@link\s+#?([^}\s]*)[^}]*\}', r'\1'),
    (r'\{@code ([^}]*)\}', r'\1'),
    (r'</?(?:ul|li|p|code|b|i)>', ' '),
    (r'`', ''),
    (r'\s*\((?:see )?[A-Za-z#]+\)', ''),
    (r'^[^:]{0,60}\(TileEntity\w+\):\s*', ''),
    (r"^(?:The )?[A-Z][A-Za-z' -]{2,40}: (?=[a-z])", ''),
    (r',? by the rules of \w+', ''),
    (r'\s*\((?:handlers? for|for) [^)]*(?:Thaumcraft|IC2|Mystcraft)[^)]*\)?', ''),
    (r',? as the original\b', ''),
    (r'\bOriginal GUI\b\.?', ''),
]


def scrub(text):
    text = re.sub(r'\s*\n\s*\*\s?', ' ', text).strip()
    for pattern, repl in SCRUB:
        text = re.sub(pattern, repl, text)
    text = re.sub(r'\s+', ' ', text).strip()
    sentences = re.split(r'(?<=[.!?])\s+', text)
    sentences = [s for s in sentences if 'original' not in s.lower() and 'placeholder' not in s.lower()]
    text = ' '.join(sentences)
    return text[:1].upper() + text[1:]


def trim(text, limit=620):
    if len(text) <= limit:
        return text
    cut = text[:limit]
    end = max(cut.rfind('. '), cut.rfind('.)'))
    if end > 200:
        return cut[:end + 1]
    end = cut.rfind('; ')
    if end > 200:
        return cut[:end] + '.'
    return cut.rsplit(' ', 1)[0].rstrip(',;:') + '...'


# ---- the machine table ----
TABLE = {}
for line in open('docs/MACHINES.md', encoding='utf-8'):
    m = re.match(r'\|\s*([^|]+?)\s*\|\s*(.+?)\s*\|\s*$', line)
    if m and not m.group(1).startswith('---') and m.group(1) != 'Block / item':
        TABLE[m.group(1)] = scrub(m.group(2))

ENTRIES = {}


def table(slug, row, item, name=None):
    text = TABLE.get(row)
    if text is None:
        raise SystemExit('handbook: no table row ' + row)
    ENTRIES[slug] = (item, name or row.split(' (')[0], trim(text))


table('dc_engine', 'DC Electric Engine', 'dc_engine')
table('wind_engine', 'Wind Engine', 'wind_engine')
table('steam_engine', 'Steam Engine', 'steam_engine')
table('gas_engine', 'Gas Engine', 'gas_engine')
table('performance_engine', 'Performance Engine', 'performance_engine')
table('microturbine', 'Microturbine', 'microturbine')
table('jet_engine', 'Jet Engine', 'jet_engine')
table('hydro_engine', 'Hydrokinetic Engine', 'hydro_engine')
table('ac_engine', 'AC Electric Engine', 'ac_engine')
table('magnetizer', 'Magnetizer', 'magnetizer')
table('flywheels', 'Flywheels (wood, stone, iron, gold, bedrock)', 'flywheel_iron', 'Flywheels')
table('clutch', 'Clutch', 'clutch')
table('splitter', 'Shaft Junction', 'splitter')
table('bevel_gear', 'Bevel Gears', 'bevel_gear')
table('shafts', 'Shafts (wood, stone, steel, diamond, bedrock)', 'shaft_steel', 'Shafts')
table('gearboxes', 'Gearboxes (wood, stone, steel, tungsten, diamond, bedrock; 2:1 to 16:1)', 'gearbox_steel_2x', 'Gearboxes')
table('dynamometer', 'Dynamometer', 'dynamometer')
table('grinder', 'Grinder', 'grinder')
table('extractor', 'Extractor', 'extractor')
table('blast_furnace', 'Blast Furnace', 'blast_furnace')
table('friction_heater', 'Friction Heater', 'friction_heater')
table('fermenter', 'Fermenter', 'fermenter')
table('cooling_fin', 'Cooling Fin', 'cooling_fin')
table('compactor', 'Compactor', 'compactor')
table('centrifuge', 'Centrifuge', 'centrifuge')
table('fractionator', 'Fractionation Unit', 'fractionator')
table('rock_melter', 'Rock Melter', 'rock_melter')
table('canola', 'Canola', 'canola_seeds')
table('fluids', 'Fluids', 'ethanol_bucket')
table('pipes', 'Pipes (Lubricant Hose, Fluid Pipe, Fuel Line, Bedrock Pipe)', 'pipe', 'Pipes')
table('fittings', 'Valve, Separator, Bypass, Suction Pipe', 'valve', 'Pipe fittings')
table('pump', 'Pump', 'pump')
table('reservoir', 'Reservoir', 'reservoir')
table('generator', 'Generator', 'generator')
table('electric_motor', 'Electric Motor', 'electric_motor')
table('screwdriver', 'Screwdriver', 'screwdriver')
table('transducer', 'Angular Transducer', 'angular_transducer')
table('steel', 'HSLA Steel Ingot', 'hsla_steel_ingot', 'HSLA Steel')

# ---- machines described by their class comments ----
blocks = [k[len('block.rotarycraft.'):] for k in lang if k.startswith('block.rotarycraft.')]
classes = {}
for root, _, files in os.walk(JAVA):
    for f in files:
        if f.endswith('BlockEntity.java'):
            classes[f[:-5]] = os.path.join(root, f)


def camel(s):
    return ''.join(p.capitalize() for p in s.split('_'))


# machines whose block entity class is not named after the block
CLASS_OF = {'van_de_graaff': 'VanDeGraffBlockEntity', 'firestarter': 'IgniterBlockEntity', 'refresher': 'ItemRefresherBlockEntity', 'railgun': 'RailGunBlockEntity'}
DOCUMENTED = {}
for b in blocks:
    path = classes.get(CLASS_OF.get(b, camel(b) + 'BlockEntity'))
    if not path:
        continue
    m = re.search(r'/\*\*(.*?)\*/\s*(?:@\w+(?:\([^)]*\))?\s*)*public (?:abstract )?class', open(path, encoding='utf-8').read(), re.S)
    if m:
        DOCUMENTED[b] = trim(scrub(m.group(1)))
for b, text in DOCUMENTED.items():
    ENTRIES.setdefault(b, (b, lang['block.rotarycraft.' + b], text))


def manual(slug, item, name, text):
    ENTRIES[slug] = (item, name, text)


manual('chain_drive', 'chain_drive', 'Chain Drive', 'A chain pulley. Two of them on parallel shafts, in a line with nothing solid between, are joined by a chain (use the chain item on one, then the other). A chain takes twice the torque and eight times the speed of a belt, but breaks apart above its limit.')
manual('split_belt', 'split_belt', 'Split Belt Pulley', 'A belt pulley that takes only part of the torque off its driver\'s shaft, which carries on through it; its far end adds that to its own shaft.')
manual('worm_drive', 'worm_drive', 'Worm Gear', '64 times the torque for 1/64 the speed, less what the worm loses (more the faster it turns). Power comes in at the back and out of the front; it works along the ground only.')
manual('cvt', 'cvt', 'CVT', 'Any ratio from 1 to 32, either way (speed for torque, or torque for speed): set by hand, by the redstone signal (a ratio for on and one for off), or automatically to hold a target torque. Needs lubricant and a belt for each step in its last slot.')
manual('energy_coil', 'energy_coil', 'Energy Coil', 'Stores the shaft power that reaches it as energy, as much as it is asked for, and gives it out again as the torque and speed set in its screen while it has a redstone signal. It blows up if it is overcharged. The energy goes with the item.')
manual('bedrock_energy_coil', 'bedrock_energy_coil', 'Bedrock Energy Coil', 'An Energy Coil that holds far more and gives out more; it too blows up if it is overcharged.')
manual('high_gear', 'high_gear', '256x Gear', 'A lubricated 256 to 1 gear, either way: more torque for less speed, or the other way, switched by using the screwdriver while sneaking. Power comes in at the back and out of the front; it works along the ground only.')
manual('anthracite_block', 'anthracite_block', 'Block of Anthracite', 'Nine anthracite; craft it alone to get them back.')
manual('lonsdaleite_block', 'lonsdaleite_block', 'Block of Lonsdaleite', 'Nine lonsdaleite; craft it alone to get them back.')
manual('blast_glass', 'blast_glass', 'Blast Glass', 'Glass that survives explosions. The Pulse Furnace makes it from obsidian.')
manual('hose', 'hose', 'Lubricant Hose', 'Carries lubricant and liquid nitrogen between machines.')

# ---- chapters and pages ----
CHAPTERS = [
    ('start', 'Getting Started', 'handbook', [
        ('welcome', 'Welcome', [
            'Everything here runs on rotation. An engine turns fuel, redstone, wind or water into a spinning shaft; shafts, gearboxes and belts carry it along; and the machines at the end of the line use it to grind, smelt, pump, farm, build or fight.',
            'A good first line is a DC Electric Engine (it runs on a redstone signal) turning a shaft into a Grinder. Put the engine down, run steel shafts out of its front, and power the engine with a lever.',
            'This book lists the machines and items, what they need, and how they are made: select a machine on a page to see its crafting recipe. The recipe viewer (JEI) and the information tooltips (Jade) say the same things.',
        ], []),
        ('power', 'Torque, Speed and Power', [
            'Rotation has two numbers. Torque, in newton-metres (N*m), is how hard it turns; speed, in radians a second (rad/s), is how fast. Power in watts is the two multiplied together.',
            'Every machine needs a minimum torque and speed, and says so on its screen. A gearbox trades one for the other: a 4:1 reduction gives four times the torque at a quarter of the speed, with the same power less what the gears lose.',
            'Engines spin up gradually when started and coast down when they stop. Where two sources feed the same shaft, matching speeds add their torque.',
        ], ['transducer', 'dynamometer']),
        ('lines', 'Building a Line', [
            'Machines take power in at the back and pass it out of the front. When placed they face where you are looking, and the Screwdriver turns them (right-click to turn the output, sneak and right-click to point it at the face you clicked).',
            'Shafts and gearboxes break when the load is more than their material can take: wood and stone are weak, steel is sturdy, and bedrock never breaks. Gearboxes wear as they work and want lubricant.',
            'Wearing the IO Goggles shows every machine\'s sides in the world: green where it takes power in, red where it gives it out.',
        ], ['screwdriver', 'shafts', 'gearboxes']),
        ('parts', 'Parts and Steel', [
            'Machines are built from parts: base panels, mounts, gears, gear units, bearings and rods in six materials, shaft cores, impellers, compressors, turbines, igniters, cylinders, coils, circuit boards and more, each with its own recipe at the crafting table.',
            'Steel (HSLA) comes from the Blast Furnace, silicon, silumin, spring tungsten and the bedrock alloy too. The Blast Furnace also does the high-temperature crafting of the best parts. Any other mod\'s steel works anywhere steel is called for.',
            'The Worktable is a crafting table that also charges charged tools from a spring coil.',
        ], ['steel', 'blast_furnace']),
    ]),
    ('engines', 'Engines and Power', 'dc_engine', [
        ('electric_engines', 'Electric Engines', [], ['dc_engine', 'ac_engine', 'magnetizer', 'electric_motor', 'generator']),
        ('fuel_engines', 'Fuel Engines', [], ['gas_engine', 'performance_engine', 'microturbine', 'jet_engine']),
        ('natural_engines', 'Wind, Water and Steam', [], ['wind_engine', 'hydro_engine', 'steam_engine', 'solar_tower', 'solar_mirror']),
        ('converters', 'Converter Engines', [
            'These engines turn another kind of energy into rotation. Each takes upgrade items that raise its tier and efficiency.',
        ], ['boiler', 'steam_turbine', 'air_compressor', 'pneumatic_engine', 'magnetic_motor', 'dynamo']),
    ]),
    ('transmission', 'Transmission', 'shaft_steel', [
        ('shafts_gears', 'Shafts, Gears and Flywheels', [], ['shafts', 'gearboxes', 'flywheels', 'bevel_gear', 'splitter']),
        ('clutches', 'Clutches and Control', [], ['clutch', 'multi_clutch', 'distribution_clutch', 'engine_controller', 'power_bus', 'bus_controller']),
        ('belts', 'Belts, Chains and Gears', [
            'Belts and chains join two pulleys on parallel shafts. Use the belt item on one pulley and then the other; it uses one belt for every block between them.',
        ], ['belt_hub', 'chain_drive', 'split_belt', 'worm_drive', 'cvt', 'high_gear']),
        ('storage', 'Storing Power', [], ['winder', 'energy_coil', 'bedrock_energy_coil', 'dynamometer']),
    ]),
    ('processing', 'Processing', 'grinder', [
        ('crushing', 'Grinding and Extracting', [], ['grinder', 'extractor', 'centrifuge', 'compactor', 'anthracite_block', 'lonsdaleite_block']),
        ('heat', 'Heat', [], ['blast_furnace', 'friction_heater', 'rock_melter', 'big_furnace', 'pulse_furnace', 'cooling_fin', 'refrigerator', 'crystallizer', 'blast_glass']),
        ('chemistry', 'Fuel and Chemistry', [], ['fermenter', 'fractionator', 'distiller', 'fuel_enhancer', 'dryer', 'composter', 'canola']),
        ('crafting', 'Crafting', [], ['worktable', 'auto_crafter']),
    ]),
    ('fluids', 'Fluids and Pipes', 'pipe', [
        ('fluid_types', 'Fluids', [], ['fluids', 'fuel_tank']),
        ('pipework', 'Pipes', [], ['pipes', 'fittings', 'hose', 'pump', 'pipe_pump']),
        ('tanks', 'Tanks and Filling', [], ['reservoir', 'gas_tank', 'deco_tank', 'filling_station', 'bucket_filler']),
    ]),
    ('world', 'Farm and World', 'fan', [
        ('farming', 'Farming', [], ['fan', 'sprinkler', 'lawn_sprinkler', 'ground_hydrator', 'fertilizer', 'defoliator']),
        ('livestock', 'Trees, Items and Animals', [], ['woodcutter', 'blower', 'vacuum', 'auto_breeder', 'bait_box', 'mob_harvester', 'spawner_controller']),
        ('digging', 'Digging and the World', [], ['borer', 'bedrock_breaker', 'sonic_borer', 'terraformer', 'weather_controller', 'chunk_loader']),
        ('building', 'Building', [
            'Machines that change blocks are off until the server turns them on in the machines configuration, and act as the player who placed them, so claims and protection mods can stop them.',
        ], ['obsidian_maker', 'line_builder', 'block_filler', 'spiller', 'pile_driver', 'self_destruct']),
        ('show', 'Light and Show', [], ['lamp', 'floodlight', 'light_bridge', 'beam_mirror', 'aerosolizer', 'firework_machine', 'particle_emitter', 'music_box']),
    ]),
    ('logistics', 'Logistics and Survey', 'sorting', [
        ('detectors', 'Detectors and Utilities', [], ['player_detector', 'smoke_detector', 'heater', 'wetter', 'aggregator', 'grindstone', 'purifier', 'firestarter', 'refresher']),
        ('sorting', 'Items and Liquids', [], ['drop_processor', 'item_filter', 'sorting', 'scale_chest', 'spillway']),
        ('survey', 'Survey and Cameras', [], ['projector', 'display', 'cctv', 'spy_cam', 'cave_scanner', 'mob_radar', 'gpr']),
    ]),
    ('weapons', 'Weapons and Defence', 'railgun', [
        ('turrets', 'Turrets', [
            'Turrets shoot hostile mobs by default. They keep a whitelist (use a Cannon Key to add a player) and treat everyone else as a target if the server leaves that on.',
        ], ['gatling', 'railgun', 'anti_air', 'laser_gun', 'flame_turret', 'heat_ray', 'freeze_gun']),
        ('area_weapons', 'Area Weapons', [], ['sonic_weapon', 'emp', 'landmine', 'force_field', 'containment', 'tnt_cannon', 'van_de_graaff']),
        ('cannons', 'Cannons', [], ['item_cannon', 'arrow_gun', 'air_gun', 'block_cannon']),
    ]),
    ('items', 'Items and Tools', 'jetpack', [
        ('charge', 'Charged Tools', [
            'A charged tool or piece of armour has a charge in kilojoules, shown on its bar. A new one is empty; put it on the Worktable with a charged spring coil to wind it up. Each use takes a unit off and it warns you as it runs low.',
            'The Stun Gun throws back what is in front of you. The Range Finder tells you the block you point at and how far away it is. The Ultrasound Scanner looks five blocks ahead for ore, silverfish, fluids and caves. The Motion Tracker lists what is along your line of sight. The Target Designator aims turrets at a block. The Handheld Crafting Grid is a crafting table you carry.',
            'Night Vision Goggles give night vision while they have charge. Jump Boots give Jump Boost IV and Speed III and a stride of a block and a half, and use a unit of charge about every three minutes; the bedrock pair never runs down.',
        ], []),
        ('steel_tools', 'Steel and Bedrock Tools', [
            'Steel tools and armour come in the usual sets plus a sickle: the steel pickaxe digs a fifth faster than iron. The sickle clears a plant, a ripe crop or a stand of cane and everything of its kind around it, and replants ripe crops.',
            'Bedrock tools never wear out and carry enchantments that cannot be removed (taking one off breaks the item). The pickaxe mines anything and lifts spawners whole; the axe fells a whole tree; the shovel turns up finds; the hoe tills a square five across; the shears take blocks whole; the sword wrecks armour and gives far more experience.',
        ], []),
        ('jetpack', 'Jetpacks', [
            'A jetpack burns ethanol or jet fuel from a 30000 mB tank kept in the item. Hold jump to climb, jump and sneak to hover, and use the movement keys for thrust. Fill it from a bucket, a pipe or the Fuel Tank. Bedrock packs burn twice as much.',
            'The plain jetpack is made at the crafting table; put it with a steel or bedrock chestplate to make the chestplate with the pack built in (fuel and upgrades kept), and put such a pack alone in the grid to take it apart again.',
            'Upgrades, each made by putting the item with the pack: three ingots of its metal (steel for the plain pack) for Wings, which glide and slow your fall (use the pack while sneaking to fold them); two Cooling Fins for Fin Cooling; a Jet Engine for the Thrust Boost, which works on jet fuel.',
            'Without fin cooling, a burning pack that touches lava or a flying wearer who is on fire blows up, destroying the pack.',
        ], ['fuel_tank']),
        ('goggles', 'Goggles and Boots', [
            'The IO Goggles show every machine\'s sides in the world within 24 blocks: green where it takes power in, red where it gives it out.',
        ], []),
        ('upgrades', 'Upgrades', [
            'Engine upgrades are used on the machine they fit: a Performance Upgrade turns a Gas Engine into a Performance Engine; the magnetostatic, efficiency and flux upgrades raise the converter engines and the Dynamo; the redstone upgrade gives the AC Engine and the Magnetizer a clock of their own, and the lodestone upgrade suits the Magnetizer.',
            'An integrated gearbox upgrade is a frame with gears: filled with lubricant it works in torque mode (more torque, less speed), with liquid nitrogen in speed mode (the other way), at a ratio of 2, 4, 8 or 16. It is fitted by using it on an engine that is not running, and stays there until the engine is broken.',
        ], []),
        ('vehicles', 'Fuel, Shells and Carts', [], ['fuel_tank', 'explosive_shell', 'ethanol_minecart']),
    ]),
]

manual('fuel_tank', 'fuel_tank', 'Portable Fuel Tank', 'Holds 16000 mB of one fuel (ethanol, jet fuel or another fuel). Used on a machine with a tank it fills the machine; used on one when empty it takes the machine\'s fuel; used in the air it tops up a jetpack you carry.')
manual('explosive_shell', 'explosive_shell', 'Explosive Shell', 'Rail gun ammunition that needs no torque and explodes where it lands (blast power 8, with fire). A rail gun uses it only when it has nothing else.')
manual('ethanol_minecart', 'ethanol_minecart', 'Ethanol Minecart', 'A furnace minecart that burns ethanol crystals instead of coal, fifteen seconds each, and runs faster. Use a crystal on it, and push it the way you want it to go.')
for slug, item in (('winder', 'winder'),):
    pass


# ---- the same words for the recipe viewer's information pages (info.rotarycraft.<id>) of anything the handbook describes ----
INFO_ONLY = {
    'jetpack': 'A tank of ethanol or jet fuel (30000 mB) and a thruster to wear or fit to a steel or bedrock chestplate. Hold jump to climb, jump and sneak to hover. Wings, fin cooling and the thrust boost are added by crafting the pack with three ingots, two Cooling Fins or a Jet Engine.',
    'steel_jetpack': 'A steel chestplate with a jetpack built in; crafting it alone takes it apart again. Fuel and upgrades are kept.',
    'bedrock_jetpack': 'A bedrock chestplate with a jetpack built in. It never wears and burns twice the fuel; crafting it alone takes it apart again.',
    'jump_boots': 'Jump Boost IV, Speed III and a stride of a block and a half (not while sneaking) while they have charge; a unit about every three minutes. Charge them on the Worktable with a spring coil.',
    'bedrock_jump_boots': 'Bedrock boots with the leap, speed and stride of jump boots that never run out.',
    'io_goggles': 'Shows every shaft machine within 24 blocks with a green block where it takes power in and a red one where it gives it out.',
    'handbook': 'A guide to the machines and items. Right-click to read it.',
    'dense_canola_seeds': 'Nine canola seeds pressed together; craft it alone to get them back.',
    'nitrate': 'Made from gunpowder, redstone and coal; the main part of explosive shells.',
    'bedrock_ingot_block': 'Nine bedrock alloy ingots; craft it alone to get them back.',
    'shield_block': 'Steel round obsidian: as hard to blow up as obsidian.',
    'stun_gun': 'Throws back what is in front of you for a unit of charge. Sneaking with plenty of charge it clears every block of a kind that touches the one you point at.',
    'range_finder': 'Tells you the block you point at and how far away it is.',
    'ultrasound': 'Looks five blocks into what you point at and says whether it holds ore, silverfish, a fluid or a cave.',
    'motion_tracker': 'Lists what is along your line of sight, coloured by what it is.',
    'target': 'Aims every cannon within 16 blocks that is in target mode at the block you point at.',
    'handheld_crafting': 'A crafting table you carry.',
    'night_vision_goggles': 'Night vision while they have charge.',
}
GROUPS = {
    'shafts': ['shaft_wood', 'shaft_stone', 'shaft_steel', 'shaft_diamond', 'shaft_bedrock'],
    'gearboxes': ['gearbox_%s_%dx' % (m, r) for m in ('wood', 'stone', 'steel', 'tungsten', 'diamond', 'bedrock') for r in (2, 4, 8, 16)],
    'flywheels': ['flywheel_wood', 'flywheel_stone', 'flywheel_iron', 'flywheel_gold', 'flywheel_bedrock'],
    'pipes': ['hose', 'fuel_line', 'bedrock_pipe', 'pipe'],
    'fittings': ['valve', 'separator', 'bypass', 'suction_pipe'],
}


def known(item):
    return 'block.rotarycraft.' + item in lang or 'item.rotarycraft.' + item in lang


for slug, (item, name, text) in list(ENTRIES.items()) + [(k, (k, k, v)) for k, v in INFO_ONLY.items()]:
    for target in [item] + GROUPS.get(slug, []):
        key = 'info.rotarycraft.' + target
        if known(target) and key not in lang:
            lang[key] = text

# ---- output ----
structure = {'chapters': []}
used = set()
for cid, ctitle, cicon, pages in CHAPTERS:
    chapter = {'id': cid, 'icon': 'rotarycraft:' + cicon, 'pages': []}
    lang['handbook.rotarycraft.chapter.' + cid] = ctitle
    for pid, ptitle, paras, slugs in pages:
        page = {'id': pid, 'entries': []}
        lang['handbook.rotarycraft.page.%s.title' % pid] = ptitle
        for i, para in enumerate(paras):
            lang['handbook.rotarycraft.page.%s.text.%d' % (pid, i)] = para
        page['paragraphs'] = len(paras)
        for slug in slugs:
            if slug not in ENTRIES:
                print('handbook: no entry for', slug, '(skipped)')
                continue
            item, name, text = ENTRIES[slug]
            used.add(slug)
            lang['handbook.rotarycraft.entry.%s.name' % slug] = name
            lang['handbook.rotarycraft.entry.%s.text' % slug] = text
            page['entries'].append({'id': slug, 'item': 'rotarycraft:' + item})
        chapter['pages'].append(page)
    structure['chapters'].append(chapter)
w(A + '/handbook/structure.json', structure)
lang.update({
    'handbook.rotarycraft.title': 'RotaryCraft Handbook',
    'handbook.rotarycraft.search': 'Search',
    'handbook.rotarycraft.no_results': 'Nothing found',
    'handbook.rotarycraft.recipe': 'Crafting',
    'handbook.rotarycraft.no_recipe': 'No crafting recipe (made in a machine, found, or dropped)',
    'handbook.rotarycraft.back': 'Back',
})
w(lang_path, lang)
missing = sorted(set(DOCUMENTED) - used)
if missing:
    print('handbook: documented machines on no page:', ' '.join(missing))
print('handbook: %d chapters, %d pages, %d entries' % (len(structure['chapters']), sum(len(c['pages']) for c in structure['chapters']), len(used)))
