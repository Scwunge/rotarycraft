"""The engine upgrades and the integrated gearbox upgrade (run by gen_assets.py): sprites from the original's item sheet, models, recipes and lang."""
import json
import os

from PIL import Image

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
SHEET = Image.open('reference/RotaryCraft/Textures/Items/items2.png').convert('RGBA')


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def sprite(index, name):
    x, y = (index % 16) * 16, (index // 16) * 16
    os.makedirs(A + '/textures/item', exist_ok=True)
    SHEET.crop((x, y, x + 16, y + 16)).save('%s/textures/item/%s.png' % (A, name))
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name}})


def item(i):
    return {'item': 'rotarycraft:' + i}


def vanilla(i):
    return {'item': 'minecraft:' + i}


def tag(t):
    return {'tag': t}


STEEL = tag('c:ingots/steel')
GOLD = vanilla('gold_ingot')


def shaped(name, pattern, key, extra=None):
    obj = {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key, 'result': {'id': 'rotarycraft:' + name, 'count': 1}}
    obj.update(extra or {})
    w('%s/recipe/%s.json' % (D, name), obj)


def blast(name, pattern, key, temperature, speed, extra=None):
    obj = {'type': 'rotarycraft:blast_crafting', 'pattern': pattern, 'key': key, 'result': {'id': 'rotarycraft:' + name, 'count': 1}, 'temperature': temperature, 'speed': speed}
    obj.update(extra or {})
    w('%s/recipe/blast_crafting/%s.json' % (D, name), obj)


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))

# name, sprite, title, description
UPGRADES = [
    ('performance_upgrade', 240, 'Performance Upgrade', 'performance', 'Upgrades Gas Engines to Performance Engines'),
    ('magnetostatic_upgrade_1', 241, 'Magnetostatic Upgrade (Tier 1)', 'magnetostatic1', 'Converter engine upgrade, tier 1'),
    ('magnetostatic_upgrade_2', 242, 'Magnetostatic Upgrade (Tier 2)', 'magnetostatic2', 'Converter engine upgrade, tier 2'),
    ('magnetostatic_upgrade_3', 243, 'Magnetostatic Upgrade (Tier 3)', 'magnetostatic3', 'Converter engine upgrade, tier 3'),
    ('magnetostatic_upgrade_4', 244, 'Magnetostatic Upgrade (Tier 4)', 'magnetostatic4', 'Converter engine upgrade, tier 4'),
    ('magnetostatic_upgrade_5', 245, 'Magnetostatic Upgrade (Tier 5)', 'magnetostatic5', 'Converter engine upgrade, tier 5'),
    ('efficiency_upgrade', 247, 'Efficiency Upgrade', 'efficiency', 'Improves energy efficiency on converter engines'),
    ('flux_upgrade', 248, 'Flux Upgrade', 'flux', 'Lets the Dynamo take twice the torque'),
    ('redstone_upgrade', 249, 'Redstone Upgrade', 'redstone', 'Adds an integrated redstone clock (AC Engine, Magnetizer, Worktable)'),
    ('lodestone_upgrade', 250, 'Lodestone Upgrade', 'lodestone', 'Makes the Magnetizer work twice as fast'),
]
for name, index, title, key, description in UPGRADES:
    sprite(index, name)
    lang['item.rotarycraft.' + name] = title
    lang['upgrade.rotarycraft.' + key] = description
sprite(246, 'afterburner_upgrade')

lang.update({
    'upgrade.rotarycraft.magnetized_to': 'Magnetized to %s microteslas',
    'upgrade.rotarycraft.must_be_magnetized': 'Must be magnetized to %s microteslas (in the Magnetizer) to be used',
    'upgrade.rotarycraft.gear.frame': 'Takes a bedrock gear unit to make an integrated gearbox',
    'upgrade.rotarycraft.gear.ratio': 'Ratio: %sx',
    'upgrade.rotarycraft.gear.needs_fluid': 'Requires 500 mB of lubricant or liquid nitrogen',
    'upgrade.rotarycraft.gear.holds': 'Is %s%% full of %s',
    'upgrade.rotarycraft.gear.torque_mode': 'Torque mode: more torque, less speed',
    'upgrade.rotarycraft.gear.speed_mode': 'Speed mode: more speed, less torque',
    'item.rotarycraft.gear_upgrade_frame': 'Integrated Gearbox Frame',
})
sprite(65, 'gear_upgrade_frame')
for exponent in range(1, 5):
    name = 'gear_upgrade_%dx' % (1 << exponent)
    sprite(66, name)
    lang['item.rotarycraft.' + name] = 'Integrated Gearbox (%dx)' % (1 << exponent)
w(A + '/lang/en_us.json', lang)

# recipes
shaped('performance_upgrade', ['sRs', 'gGg', ' b '], {'s': item('silumin_ingot'), 'R': item('radiator'), 'g': GOLD, 'G': item('steel_gear_unit_2'), 'b': item('base_panel')})
shaped('magnetostatic_upgrade_1', ['gRg', 'RER', 'SGS'], {'g': GOLD, 'R': vanilla('redstone'), 'E': item('ethanol_crystals'), 'S': STEEL, 'G': item('impeller')})
shaped('magnetostatic_upgrade_2', ['SCS', 'ERE', 'SCS'], {'C': item('red_gold_ingot'), 'R': item('gold_coil'), 'S': STEEL, 'E': item('tungsten_shaft_core')})
shaped('magnetostatic_upgrade_3', ['SES', 'ERE', 'ScS'], {'c': item('circuit_board'), 'R': item('tungsten_ingot'), 'S': STEEL, 'E': item('red_gold_ingot')})
blast('magnetostatic_upgrade_4', ['cEc', 'ERE', 'SES'], {'c': item('cooling_fin'), 'R': item('bedrock_ingot'), 'S': STEEL, 'E': item('tungsten_ingot')}, 1000, 4)
blast('magnetostatic_upgrade_5', ['SES', 'ERE', 'SES'], {'R': item('bedrock_gear'), 'S': STEEL, 'E': item('spring_tungsten_ingot')}, 1800, 8)
blast('efficiency_upgrade', ['IGI', 'FTF', 'BPB'], {'G': item('generator_unit'), 'I': item('red_gold_ingot'), 'B': item('water_plate'), 'P': item('power_module'),
                                                     'F': item('bedrock_ingot'), 'T': item('tungsten_ingot')}, 1980, 32,
      {'neoforge:conditions': [{'type': 'neoforge:not', 'value': {'type': 'neoforge:mod_loaded', 'modid': 'reactorcraft'}}]})
shaped('flux_upgrade', ['BeB', 'tEt', 'BeB'], {'e': item('red_gold_ingot'), 'B': item('base_panel'), 'E': item('bedrock_ingot'), 't': item('tungsten_ingot')})
shaped('redstone_upgrade', ['rQr', 'qsq', 'rEr'], {'r': vanilla('redstone'), 'Q': vanilla('clock'), 'q': vanilla('quartz'), 's': STEEL, 'E': vanilla('ender_pearl')})
blast('gear_upgrade_frame', ['sSS', 'SsS', 'SSG'], {'S': STEEL, 'G': vanilla('glass_pane'), 's': item('bedrock_shaft_core')}, 1200, 2)
for exponent in range(1, 5):
    name = 'gear_upgrade_%dx' % (1 << exponent)
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                        'ingredients': [item('gear_upgrade_frame'), item('bedrock_gear_unit_%d' % (1 << exponent))],
                                        'result': {'id': 'rotarycraft:' + name, 'count': 1}})
