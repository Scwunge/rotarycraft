"""The handheld tools (run by gen_assets.py): sprites from the original item sheet, models, recipes, the gravel gun's damage type and lang. Written beside
gen_gadgets.py, which does the charged gadgets."""
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


def shaped(name, pattern, key):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern, 'key': key,
                                       'result': {'id': 'rotarycraft:' + name, 'count': 1}})


# name, sprite index in items2.png, title, model parent
TOOLS = [
    ('gravel_gun', 176, 'Gravel Gun', 'minecraft:item/handheld'),
    ('vacuum_gun', 160, 'Vacuum Gun', 'minecraft:item/handheld'),
    ('flamethrower', 28, 'Flamethrower', 'minecraft:item/handheld'),
    ('hand_pump', 29, 'Hand Pump', 'minecraft:item/handheld'),
    ('spring_piston', 43, 'Spring Piston', 'minecraft:item/handheld'),
    ('tile_selector', 11, 'Tile Selector', 'minecraft:item/handheld'),
    ('match_filter', 50, 'Match Filter', 'minecraft:item/generated'),
]
lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
for name, index, title, parent in TOOLS:
    sprite(index, name, parent)
    lang['item.rotarycraft.' + name] = title

# the fireball launcher: nine icons, by how far it is wound up
for level in range(9):
    sprite(224 + level, 'fireball_launcher_%d' % level, 'minecraft:item/handheld')
w(A + '/models/item/fireball_launcher.json', {'parent': 'rotarycraft:item/fireball_launcher_0', 'overrides': [
    {'predicate': {'rotarycraft:blast': level / 8}, 'model': 'rotarycraft:item/fireball_launcher_%d' % level} for level in range(1, 9)]})
lang['item.rotarycraft.fireball_launcher'] = 'Fireball Launcher'

shaped('gravel_gun', [' d ', 'gcg', 'sas'], {'a': STEEL, 's': STEEL, 'c': vanilla('chest'), 'd': vanilla('dispenser'), 'g': item('steel_gear')})
shaped('vacuum_gun', [' n ', 'scs', ' s '], {'s': STEEL, 'c': item('impeller'), 'n': item('diffuser')})
shaped('fireball_launcher', ['b b', 'scs', 'srs'], {'s': STEEL, 'c': item('combustor'), 'r': vanilla('redstone'), 'b': vanilla('blaze_rod')})
shaped('tile_selector', [' l ', 'srs', 'ses'], {'s': STEEL, 'e': vanilla('ender_pearl'), 'r': vanilla('redstone'), 'l': vanilla('lapis_lazuli')})
shaped('hand_pump', [' sP', 'sIs', 'sRs'], {'s': STEEL, 'P': item('pipe'), 'I': item('impeller'), 'R': item('reservoir')})
shaped('spring_piston', [' sP', 'sRs', 'gs '], {'s': STEEL, 'g': vanilla('glowstone_dust'), 'P': vanilla('piston'), 'R': vanilla('redstone_block')})

# what a flint hit looks like when it kills
w(D + '/damage_type/gravel_gun.json', {'message_id': 'rotarycraft.gravel_gun', 'scaling': 'never', 'exhaustion': 0.1})
lang.update({
    'death.attack.rotarycraft.gravel_gun': '%1$s was hit by supersonic flint',
    'death.attack.rotarycraft.gravel_gun.player': '%1$s was hit by supersonic flint from %2$s',
    'item.rotarycraft.gravel_gun.damage': 'Dealing %s hearts of damage per shot',
    'item.rotarycraft.gravel_gun.empty': 'Unable to fire - requires charging',
    'item.rotarycraft.hand_pump.contents': 'Contents: %s mB of %s',
    'item.rotarycraft.hand_pump.mode_drain': 'Mode: Drain',
    'item.rotarycraft.hand_pump.mode_place': 'Mode: Place',
    'item.rotarycraft.match_filter.holds': 'Holds: %s',
    'message.rotarycraft.pump.drain': 'Hand pump: drain mode',
    'message.rotarycraft.pump.place': 'Hand pump: place mode',
    'message.rotarycraft.selector.linked': 'Linked to %s',
    'message.rotarycraft.selector.unlinked': 'Tile selector unlinked',
    'message.rotarycraft.selector.added': 'Added [%s, %s, %s] to %s',
    'message.rotarycraft.selector.refused': '%4$s would not take [%1$s, %2$s, %3$s]',
})
w(lang_path, lang)
