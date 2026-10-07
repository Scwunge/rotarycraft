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
    'SUCTION': 'suction_pipe', 'DISTRIBCLUTCH': 'distribution_clutch', 'DROPS': 'drop_processor', 'DISK': 'music_disc', 'PULSEJET': 'pulse_furnace', 'FIREBALL': 'fireball_launcher', 'HELDPISTON': 'spring_piston', 'ADVANCEDGEARS': 'worm_drive,cvt,energy_coil,bedrock_energy_coil,high_gear', 'BELT': 'belt_hub', 'CHAIN': 'chain_drive', 'SPLITBELT': 'split_belt', 'ECU': 'engine_controller', 'VANDEGRAFF': 'van_de_graaff', 'FUELENGINE': 'gas_engine',
    'STEELPICK': 'steel_pickaxe', 'STEELAXE': 'steel_axe', 'STEELSHOVEL': 'steel_shovel', 'STEELHOE': 'steel_hoe', 'STEELSWORD': 'steel_sword',
    'STEELSHEARS': 'steel_shears', 'STEELSICKLE': 'steel_sickle', 'STEELHELMET': 'steel_helmet', 'STEELCHEST': 'steel_chestplate', 'STEELLEGS': 'steel_leggings',
    'STEELBOOTS': 'steel_boots',
    'BEDPICK': 'bedrock_pickaxe', 'BEDAXE': 'bedrock_axe', 'BEDSHOVEL': 'bedrock_shovel', 'BEDHOE': 'bedrock_hoe', 'BEDSWORD': 'bedrock_sword',
    'BEDSHEARS': 'bedrock_shears', 'BEDSICKLE': 'bedrock_sickle', 'BEDHELM': 'bedrock_helmet', 'BEDCHEST': 'bedrock_chestplate', 'BEDLEGS': 'bedrock_leggings',
    'BEDBOOTS': 'bedrock_boots',
    'STUNGUN': 'stun_gun', 'RANGEFINDER': 'range_finder', 'ULTRASOUND': 'ultrasound', 'MOTION': 'motion_tracker', 'NVG': 'night_vision_goggles',
    'HANDCRAFT': 'handheld_crafting', 'TARGET': 'target', 'IOGOGGLES': 'io_goggles', 'bedingotblock': 'bedrock_ingot_block', 'SHELL': 'explosive_shell', 'MINECART': 'ethanol_minecart', 'FUEL': 'fuel_tank', 'JUMP': 'jump_boots', 'BEDJUMP': 'bedrock_jump_boots',
    'ENGINE': 'dc_engine,ac_engine,wind_engine,steam_engine,gas_engine,performance_engine,hydro_engine,jet_engine',
    'FLYWHEEL': 'flywheel_wood,flywheel_stone,flywheel_iron,flywheel_gold,flywheel_bedrock',
    'SHAFT': 'shaft_wood,shaft_stone,shaft_steel,shaft_diamond,shaft_bedrock',
    'GEARBOX': ','.join('gearbox_%s_%dx' % (m, r) for m in ('wood', 'stone', 'steel', 'tungsten', 'diamond', 'bedrock') for r in (2, 4, 8, 16)),
    'UPGRADE': 'performance_upgrade,magnetostatic_upgrade_1,magnetostatic_upgrade_2,magnetostatic_upgrade_3,magnetostatic_upgrade_4,magnetostatic_upgrade_5,efficiency_upgrade,flux_upgrade,redstone_upgrade,afterburner_upgrade',
    'GEARUPGRADE': 'gear_upgrade_frame,gear_upgrade_2x,gear_upgrade_4x,gear_upgrade_8x,gear_upgrade_16x', 'KEY': 'cannon_key', 'METER': 'angular_transducer', 'SEPARATION': 'separator',
    # parts, ItemStacks names
    'aluminumcylinder': 'silumin_cylinder', 'anthrablock': 'anthracite_block', 'bedingot': 'bedrock_ingot', 'bedrockshaft': 'bedrock_shaft_core',
    'compoundcompress': 'compound_compressor', 'compoundturb': 'compound_turbine', 'diamondshaft': 'diamond_shaft_core',
    'lim': 'linear_induction_motor', 'lonsblock': 'lonsdaleite_block', 'lonsda': 'lonsdaleite', 'pcb': 'circuit_board', 'power': 'power_module',
    'prop': 'propeller', 'radar': 'radar_unit', 'railaim': 'rail_aiming_unit', 'shaftitem': 'shaft_core', 'sonar': 'sonar_unit',
    'tenscoil': 'strong_coil', 'tungstenshaft': 'tungsten_shaft_core',
}
# not ported, and what is in the way
PENDING_NOTE = {
    'BEDGRAFTER': 'stays: the Grafter works on Forestry trees and needs Forestry', 'GRAFTER': 'stays: the Grafter works on Forestry trees and needs Forestry',
    'BEDSAW': 'stays: the saw cuts ForgeMultipart microblocks, which need that mod', 'BEDKNIFE': 'stays: commented out in the original (it needs Applied Energistics)',
    'NVH': 'stays: commented out in the original', 'BUNDLEDBUS': 'stays: the Bundled Bus needs ProjectRed (and Applied Energistics for its recipe)',
    'BED': 'bedrock tools and armour (items, section 6)', 'STEEL': 'steel tools and armour (items, section 6)',
}
BY_TAG = {
    'steelingot': 'steel is the c:ingots/steel tag (made in the Blast Furnace)', 'steelblock': 'c:storage_blocks/steel from other mods',
    'cokeblock': 'no coke block; coke is crafted in the Blast Furnace', 'ironscrap': 'scrap is made by the Grinder',
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
