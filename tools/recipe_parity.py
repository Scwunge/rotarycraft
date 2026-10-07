"""Writes src/main/resources/gametest/original_recipes.txt: every result the original's RotaryRecipes crafts (machines, items, and the parts made with
GameRegistry), against what this port calls it. Lines are  MAP|original|port id[,port id]  where every id has a recipe, or  PENDING|original|reason
for what is not ported yet (the parity GameTest fails when a PENDING one gains a recipe, so the list is kept honest), or  BY_TAG|original|reason where the
port uses another mod's item through a tag by design. Run from the repo root; the aliases below say which names differ."""
import glob
import json
import os
import re

SRC = open('reference/RotaryCraft/RotaryRecipes.java', encoding='latin-1').read()

# the original's name -> this port's id(s), where they are not the same once capitals and underscores are ignored
ALIASES = {
    'BEDPIPE': 'bedrock_pipe', 'BEVELGEARS': 'bevel_gear', 'CRAFTER': 'auto_crafter', 'DRYING': 'dryer', 'FILLER': 'block_filler',
    'FIREWORK': 'firework_machine', 'FRICTION': 'friction_heater', 'HYDRATOR': 'ground_hydrator', 'LAVAMAKER': 'rock_melter',
    'MAGNETIC': 'magnetic_motor', 'OBSIDIAN': 'obsidian_maker', 'PARTICLE': 'particle_emitter', 'PNEUENGINE': 'pneumatic_engine',
    'SUCTION': 'suction_pipe', 'DISTRIBCLUTCH': 'distribution_clutch', 'ECU': 'engine_controller', 'VANDEGRAFF': 'van_de_graaff', 'FUELENGINE': 'gas_engine',
    'ENGINE': 'dc_engine,ac_engine,wind_engine,steam_engine,gas_engine,performance_engine,hydro_engine,jet_engine',
    'FLYWHEEL': 'flywheel_wood,flywheel_stone,flywheel_iron,flywheel_gold,flywheel_bedrock',
    'SHAFT': 'shaft_wood,shaft_stone,shaft_steel,shaft_diamond,shaft_bedrock',
    'GEARBOX': ','.join('gearbox_%s_%dx' % (m, r) for m in ('wood', 'stone', 'steel', 'tungsten', 'diamond', 'bedrock') for r in (2, 4, 8, 16)),
    'UPGRADE': 'afterburner_upgrade', 'KEY': 'cannon_key', 'METER': 'angular_transducer', 'SEPARATION': 'separator',
    # parts, ItemStacks names
    'aluminumcylinder': 'silumin_cylinder', 'anthrablock': 'anthracite_block', 'bedingot': 'bedrock_ingot', 'bedrockshaft': 'bedrock_shaft_core',
    'compoundcompress': 'compound_compressor', 'compoundturb': 'compound_turbine', 'diamondshaft': 'diamond_shaft_core',
    'lim': 'linear_induction_motor', 'lonsblock': 'lonsdaleite_block', 'lonsda': 'lonsdaleite', 'pcb': 'circuit_board', 'power': 'power_module',
    'prop': 'propeller', 'radar': 'radar_unit', 'railaim': 'rail_aiming_unit', 'shaftitem': 'shaft_core', 'sonar': 'sonar_unit',
    'tenscoil': 'strong_coil', 'tungstenshaft': 'tungsten_shaft_core',
}
# not ported, and what is in the way
PENDING_NOTE = {
    'BED': 'bedrock tools and armour (items, section 6)', 'STEEL': 'steel tools and armour (items, section 6)',
}
BY_TAG = {
    'steelingot': 'steel is the c:ingots/steel tag (made in the Blast Furnace)', 'steelblock': 'c:storage_blocks/steel from other mods',
    'cokeblock': 'no coke block; coke is crafted in the Blast Furnace', 'ironscrap': 'scrap is made by the Grinder',
    'nitrate': 'not an item of this port yet',
}


def norm(s):
    return s.lower().replace('_', '')


def originals():
    names = {}
    for m in re.finditer(r'(MachineRegistry|ItemRegistry)\.([A-Z0-9_]+)\.(add\w*)\(', SRC):
        names[m.group(2)] = 'machine/item'
    for m in re.finditer(r'GameRegistry\.add(?:Shaped|Shapeless)?Recipe\(\s*(?:new (?:ShapedOreRecipe|ShapelessOreRecipe|ShapedRecipes)\(\s*)?(?:ReikaItemHelper\.getSizedItemStack\()?ItemStacks\.(\w+)', SRC):
        names[m.group(1)] = 'part'
    return names


def recipes():
    outs = {}
    for f in glob.glob('src/main/resources/data/rotarycraft/recipe/**/*.json', recursive=True):
        d = json.load(open(f))
        r = d.get('result')
        rid = (r.get('id', r.get('item', '')) if isinstance(r, dict) else (r or '')).split(':')[-1]
        outs[norm(rid)] = rid
    return outs


def main():
    outs = recipes()
    lines = []
    for name in sorted(originals(), key=lambda s: (s.islower(), s)):
        ids = ALIASES.get(name)
        if ids is None and norm(name) in outs:
            ids = outs[norm(name)]
        if ids is not None:
            have = [i for i in ids.split(',') if norm(i) in outs]
            missing = [i for i in ids.split(',') if norm(i) not in outs]
            if missing:
                print('NO RECIPE YET FOR', name, missing)
            lines.append('MAP|%s|%s' % (name, ','.join(have) if have else ids))
        elif name in BY_TAG:
            lines.append('BY_TAG|%s|%s' % (name, BY_TAG[name]))
        else:
            note = next((v for k, v in PENDING_NOTE.items() if name.startswith(k)), 'not ported yet')
            lines.append('PENDING|%s|%s' % (name, note))
    os.makedirs('src/main/resources/gametest', exist_ok=True)
    with open('src/main/resources/gametest/original_recipes.txt', 'w') as f:
        f.write('\n'.join(lines) + '\n')
    print(len(lines), 'lines;', sum(l.startswith('PENDING') for l in lines), 'pending')


main()
