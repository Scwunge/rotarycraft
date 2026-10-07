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

lang.update({
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
