"""The charged tools and gadgets (run by gen_assets.py): sprites from the original's item sheet, models, recipes and lang. Add one by appending to GADGETS."""
import json
import os
import shutil

from PIL import Image

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
REF = 'reference/RotaryCraft/Textures'
SHEET = Image.open(REF + '/Items/items2.png').convert('RGBA')


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def sprite(index, name, parent='minecraft:item/generated'):
    x, y = (index % 16) * 16, (index // 16) * 16
    os.makedirs(A + '/textures/item', exist_ok=True)
    SHEET.crop((x, y, x + 16, y + 16)).save('%s/textures/item/%s.png' % (A, name))
    w('%s/models/item/%s.json' % (A, name), {'parent': parent, 'textures': {'layer0': 'rotarycraft:item/' + name}})


def item(i):
    return {'item': 'rotarycraft:' + i}


def vanilla(i):
    return {'item': 'minecraft:' + i}


STEEL = {'tag': 'c:ingots/steel'}


def shaped(name, pattern, key, count=1):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern, 'key': key,
                                       'result': {'id': 'rotarycraft:' + name, 'count': count}})


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))

# name, sprite, title, parent
GADGETS = [
    ('stun_gun', 192, 'Stun Gun', 'minecraft:item/handheld'),
    ('range_finder', 42, 'Range Finder', 'minecraft:item/generated'),
    ('ultrasound', 128, 'Ultrasound Scanner', 'minecraft:item/generated'),
    ('motion_tracker', 144, 'Motion Tracker', 'minecraft:item/generated'),
    ('night_vision_goggles', 97, 'Night Vision Goggles', 'minecraft:item/generated'),
    ('handheld_crafting', 33, 'Handheld Crafting Grid', 'minecraft:item/generated'),
    ('target', 98, 'Target Designator', 'minecraft:item/generated'),
    ('io_goggles', 1, 'IO Goggles', 'minecraft:item/generated'),
    ('fuel_tank', 27, 'Portable Fuel Tank', 'minecraft:item/generated'),
    ('explosive_shell', 5, 'Explosive Shell', 'minecraft:item/generated'),
    ('ethanol_minecart', 6, 'Ethanol Minecart', 'minecraft:item/generated'),
    ('jump_boots', 30, 'Jump Boots', 'minecraft:item/generated'),
    ('bedrock_jump_boots', 31, 'Bedrock Jump Boots', 'minecraft:item/generated'),
]
for name, index, title, parent in GADGETS:
    sprite(index, name, parent)
    lang['item.rotarycraft.' + name] = title

shaped('ultrasound', [' n ', 'scs', ' s '], {'s': STEEL, 'c': item('screen'), 'n': item('sonar_unit')})
shaped('motion_tracker', [' nr', 'scs', ' s '], {'s': STEEL, 'c': item('screen'), 'n': item('sonar_unit'), 'r': item('radar_unit')})
shaped('stun_gun', [' n ', 'scs', ' s '], {'s': STEEL, 'c': item('sonar_unit'), 'n': item('diffuser')})
shaped('range_finder', [' e ', 'rGr', 'sss'], {'s': STEEL, 'G': vanilla('glowstone'), 'r': vanilla('redstone'), 'e': vanilla('ender_pearl')})
shaped('handheld_crafting', [' g ', 'scs', ' g '], {'s': STEEL, 'g': vanilla('gold_ingot'), 'c': vanilla('crafting_table')})
shaped('night_vision_goggles', ['scs', 'ese'], {'s': STEEL, 'c': item('screen'), 'e': vanilla('ender_eye')})
shaped('target', [' E ', 'SRS', 'SLS'], {'S': STEEL, 'R': vanilla('redstone'), 'E': vanilla('ender_pearl'), 'L': vanilla('lapis_lazuli')})
shaped('io_goggles', ['scs', 'ese'], {'s': STEEL, 'c': vanilla('ender_pearl'), 'e': vanilla('redstone')})
shaped('jump_boots', ['GbG', 'SgS', 'B B'], {'B': item('base_panel'), 'G': item('steel_gear'), 'b': item('steel_boots'), 'g': item('steel_gear_unit_2'), 'S': item('spring')})
w(D + '/recipe/bedrock_jump_boots.json', {'type': 'minecraft:crafting_shapeless', 'category': 'equipment', 'ingredients': [item('bedrock_boots'), item('jump_boots')],
                                          'result': {'id': 'rotarycraft:bedrock_jump_boots', 'count': 1}})

# the jetpacks: a sprite, and the same with wings (the item's `winged` property chooses)
for name, index, wing, title in (('jetpack', 28, 60, 'Jetpack'), ('steel_jetpack', 44, 61, 'Steel Jetpack'), ('bedrock_jetpack', 12, 59, 'Bedrock Jetpack')):
    sprite(index, name)
    sprite(wing, name + '_wing')
    w('%s/models/item/%s_winged.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name, 'layer1': 'rotarycraft:item/' + name + '_wing'}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name},
                                           'overrides': [{'predicate': {'rotarycraft:winged': 1}, 'model': 'rotarycraft:item/' + name + '_winged'}]})
    lang['item.rotarycraft.' + name] = title
    os.remove('%s/models/item/%s_wing.json' % (A, name))
shaped('jetpack', ['CRC', 'cBc', 'd d'], {'C': item('combustor'), 'R': item('reservoir'), 'B': item('base_panel'), 'd': item('diffuser'), 'c': item('compressor')})
w(D + '/recipe/jetpack_work.json', {'type': 'rotarycraft:jetpack', 'category': 'equipment'})
shaped('fuel_tank', ['SBS', 'BGB', 'SPS'], {'S': STEEL, 'B': item('base_panel'), 'G': vanilla('glass'), 'P': item('pipe')})
shaped('explosive_shell', [' s ', 'sns', ' s '], {'s': STEEL, 'n': item('nitrate')}, 16)
w(D + '/recipe/nitrate.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [vanilla('gunpowder'), vanilla('redstone'), {'tag': 'minecraft:coals'}],
                              'result': {'id': 'rotarycraft:nitrate', 'count': 4}})
shaped('ethanol_minecart', ['g', 'm'], {'g': item('gas_engine'), 'm': vanilla('minecart')})

lang.update({
    'item.rotarycraft.fuel_tank.empty': 'Empty',
    'item.rotarycraft.fuel_tank.contents': 'Contents: %s mB of %s',
    'tooltip.rotarycraft.explosive_shell': 'Needs no torque; explodes where it lands',
    'entity.rotarycraft.ethanol_minecart': 'Ethanol Minecart',
    'item.rotarycraft.jetpack.fuel': 'Fuel: %s mB of %s',
    'item.rotarycraft.jetpack.empty': 'No Fuel',
    'item.rotarycraft.jetpack.wings_off': '[Wing Disabled]',
    'item.rotarycraft.charge': 'Charge: %s / %s kJ',
    'message.rotarycraft.tool.depleted': 'Tool charge is depleted!',
    'message.rotarycraft.tool.very_low': 'Tool charge is very low (%s kJ)!',
    'message.rotarycraft.tool.low': 'Tool charge is low (%s kJ)!',
    'message.rotarycraft.armor.depleted': 'Armor charge is depleted!',
    'message.rotarycraft.armor.very_low': 'Armor charge is very low (%s kJ)!',
    'message.rotarycraft.armor.low': 'Armor charge is low (%s kJ)!',
    'message.rotarycraft.range_finder': "Block '%s' is %sm away.",
    'message.rotarycraft.ultrasound.ore': 'Ore Detected!',
    'message.rotarycraft.ultrasound.silverfish': 'Silverfish Detected!',
    'message.rotarycraft.ultrasound.fluid': '%s Detected!',
    'message.rotarycraft.ultrasound.cave': 'Cave Detected!',
    'message.rotarycraft.motion.away': '%s %sm away.',
    'message.rotarycraft.motion.attacking': 'Mob is Attacking!',
    'message.rotarycraft.target': 'Set %s cannon(s) to [%s, %s, %s]',
})
w(lang_path, lang)

os.makedirs(A + '/textures/models/armor', exist_ok=True)
shutil.copy(REF + '/Misc/NVGoggles.png', A + '/textures/models/armor/night_vision_layer_1.png')
shutil.copy(REF + '/Misc/NVGoggles.png', A + '/textures/models/armor/night_vision_layer_2.png')
shutil.copy(REF + '/Misc/IOGoggles.png', A + '/textures/models/armor/io_goggles_layer_1.png')
shutil.copy(REF + '/Misc/IOGoggles.png', A + '/textures/models/armor/io_goggles_layer_2.png')
for src, name in (('jet', 'jet'), ('bedrock_jet', 'bedrock_jet')):
    for layer in (1, 2):
        shutil.copy(REF + '/Misc/%s.png' % src, A + '/textures/models/armor/%s_layer_%d.png' % (name, layer))
