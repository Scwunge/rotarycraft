"""Weapons, defence and utility machines (run by gen_assets.py, before gen_parts.py): the original's model textures and item icons,
blockstates and models (the machines are drawn by their renderers), loot, tags, recipes, damage types and lang."""
import json
import os
import shutil
import subprocess
import sys

from PIL import Image

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'
REF = 'reference/RotaryCraft'


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def sprite(sheet, index, name):
    """An item icon cut from the original's sprite sheets (sheet 0 is items.png, sheet n is items<n+1>.png)."""
    im = Image.open('%s/Textures/Items/items%s.png' % (REF, '' if sheet == 0 else sheet + 1)).convert('RGBA')
    im.crop(((index % 16) * 16, (index // 16) * 16, (index % 16 + 1) * 16, (index // 16 + 1) * 16)).save('%s/item/%s.png' % (T, name))
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name}})


def model_texture(src, name):
    os.makedirs(T + '/machine', exist_ok=True)
    shutil.copy('%s/Textures/TileEntityTex/%s' % (REF, src), '%s/machine/%s.png' % (T, name))


BLOCK_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}
MACHINES = []


def rendered_machine(name, facings=('up', 'down', 'north', 'south', 'east', 'west'), particle='rotarycraft:block/shaft_steel'):
    """A machine drawn entirely by its renderer: the block model only gives break particles; the item is drawn by the renderer too."""
    MACHINES.append(name)
    w('%s/models/block/%s.json' % (A, name), {'textures': {'particle': particle}})
    w('%s/blockstates/%s.json' % (A, name), {'variants': {'facing=' + f: {'model': 'rotarycraft:block/' + name} for f in facings}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': particle},
                                             'display': BLOCK_DISPLAY})
    w('%s/loot_table/blocks/%s.json' % (D, name), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def item(i):
    return {'item': 'rotarycraft:' + i}


def tag(t):
    return {'tag': t}


STEEL = tag('c:ingots/steel')


def shaped(name, pattern, key, count=1, result=None):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                        'result': {'id': 'rotarycraft:' + (result or name), 'count': count}})


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))

# ---- turret parts (the original's MISCCRAFT row: barrel, lens, bulb, rail head, rail base, rail aiming unit) ----
for meta, name, title in ((0, 'barrel', 'Barrel'), (1, 'lens', 'Lens'), (3, 'bulb', 'Bulb'), (9, 'rail_head', 'Railgun Head'),
                          (10, 'rail_base', 'Turret Base'), (11, 'rail_aiming_unit', 'Turret Aiming Unit')):
    sprite(0, 2 * 16 + meta, name)
    lang['item.rotarycraft.' + name] = title
shaped('barrel', ['OOO', 'gtG', 'OOO'], {'t': item('tungsten_ingot'), 'O': {'item': 'minecraft:obsidian'}, 'G': item('blast_glass'), 'g': {'item': 'minecraft:glowstone'}})
shaped('bulb', ['GGG', 'BDB', 'BRB'], {'D': {'item': 'minecraft:nether_star'}, 'G': {'item': 'minecraft:glowstone'}, 'R': {'item': 'minecraft:redstone'},
                                       'B': {'item': 'minecraft:blaze_rod'}})
shaped('lens', [' D ', 'DGD', ' D '], {'D': tag('c:gems/diamond'), 'G': item('blast_glass')})
shaped('rail_head', ['LLL', 'LGL', 'LLL'], {'G': item('power_module'), 'L': item('linear_induction_motor')})
shaped('rail_base', [' S ', 'PGP'], {'P': item('base_panel'), 'G': item('steel_gear_unit_2'), 'S': item('steel_gear')})
shaped('rail_aiming_unit', ['sds', 'CRC', 'sgs'], {'R': item('radar_unit'), 'C': item('circuit_board'), 's': STEEL, 'd': tag('c:gems/diamond'),
                                                    'g': item('generator_unit')})

# ---- Rail Gun ----
model_texture('railguntex.png', 'railgun')
os.makedirs(T + '/entity', exist_ok=True)
shutil.copy(REF + '/Textures/Entity/railgun.png', T + '/entity/railgun_shot.png')
shutil.copy(REF + '/Textures/Entity/freezegun.png', T + '/entity/freeze_shot.png')
rendered_machine('railgun')
shaped('railgun', [' H ', ' A ', ' B '], {'B': item('rail_base'), 'A': item('rail_aiming_unit'), 'H': item('rail_head')})
for tier in range(16):
    sprite(1, 129 + (0 if tier < 7 else 1 if tier < 10 else 2 if tier < 13 else 3), 'railgun_ammo_%d' % tier)
    lang['item.rotarycraft.railgun_ammo_%d' % tier] = 'Railgun Ammunition (Tier %d)' % tier
shaped('railgun_ammo_0', ['ss ', 's  '], {'s': STEEL}, 3)
for tier in range(1, 16):
    # each three tiers wrap the last in more of one material: planks, stone, iron, gold, bedrock alloy (as the original)
    filler = [tag('minecraft:planks'), {'item': 'minecraft:stone'}, tag('c:ingots/iron'), tag('c:ingots/gold'), item('bedrock_ingot')][(tier - 1) // 3]
    pattern = [['p  ', ' s ', '  p'], ['p p', ' s ', 'p p'], ['ppp', 'psp', 'ppp']][(tier - 1) % 3]
    w('%s/recipe/railgun_ammo_%d.json' % (D, tier), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                                                    'key': {'s': item('railgun_ammo_%d' % (tier - 1)), 'p': filler},
                                                    'result': {'id': 'rotarycraft:railgun_ammo_%d' % tier, 'count': 1}})

# ---- Freeze Gun and Anti-Air ----
model_texture('freezeguntex.png', 'freeze_gun')
model_texture('aagun.png', 'anti_air')
rendered_machine('freeze_gun')
rendered_machine('anti_air')
shaped('freeze_gun', [' ss', 'iig', 'sb '], {'b': item('rail_base'), 'i': {'item': 'minecraft:ice'}, 's': STEEL, 'g': item('steel_gear_unit_2')})
shaped('anti_air', ['sss', 'ppc', ' Ba'], {'p': item('pipe'), 'c': item('compressor'), 's': STEEL, 'a': item('rail_aiming_unit'), 'B': item('rail_base')})
w('%s/damage_type/turret.json' % D, {'message_id': 'rotarycraft.turret', 'scaling': 'never', 'exhaustion': 0.1})

# ---- Cannon Key ----
sprite(1, 4, 'cannon_key')
shaped('cannon_key', ['s', 's', 'P'], {'P': item('base_panel'), 's': STEEL})

lang.update({
    'block.rotarycraft.railgun': 'Rail Gun',
    'item.rotarycraft.cannon_key': 'Cannon Key',
    'tooltip.rotarycraft.railgun_ammo': 'Needs %s N*m to fire',
    'tooltip.rotarycraft.cannon_key': "%s's Cannon Key",
    'message.rotarycraft.cannon_key.wrong_owner': "The key is for %s's machines, but this one is owned by %s",
    'message.rotarycraft.cannon_key.already': '%s is already on the whitelist',
    'message.rotarycraft.cannon_key.added': "%s added to %s's whitelist",
    'gui.rotarycraft.safe_players': 'Players this turret will not target',
    'gui.rotarycraft.safe_players.remove': 'Remove',
    'gui.rotarycraft.safe_players.none': 'Nobody but the owner',
    'block.rotarycraft.freeze_gun': 'Freeze Gun',
    'block.rotarycraft.anti_air': 'Anti-Aircraft Gun',
    'effect.rotarycraft.frozen_solid': 'Frozen Solid',
    'entity.rotarycraft.railgun_shot': 'Rail Gun Slug',
    'entity.rotarycraft.freeze_shot': 'Snowball',
    'entity.rotarycraft.flak_shot': 'Flak',
    'death.attack.rotarycraft.turret': '%1$s was shot by a turret',
    'death.attack.rotarycraft.turret.player': "%1$s was shot by %2$s's turret",
})
w(lang_path, lang)

path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += ['rotarycraft:' + m for m in MACHINES if 'rotarycraft:' + m not in data['values']]
w(path, data)

subprocess.run([sys.executable, 'tools/modelbase2json.py', 'ModelRailGun', 'ModelFreezeGun', 'ModelAAGun:anti_air'], check=True)
print('weapons ok,', len(MACHINES), 'machines')
