"""Farming and automation machines (run by gen_assets.py, after gen_world.py): the original's model textures, blockstates and models (the
machines are drawn by their renderers), loot, tags, recipes and lang. Add a machine by appending to the lists below."""
import json
import os
import shutil
import subprocess
import sys

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
MODELS = []


def rendered_machine(name, particle='rotarycraft:block/shaft_steel'):
    """A machine drawn entirely by its renderer: the block model only gives break particles; the item is drawn by the renderer too."""
    MACHINES.append(name)
    w('%s/models/block/%s.json' % (A, name), {'textures': {'particle': particle}})
    w('%s/blockstates/%s.json' % (A, name), {'variants': {'facing=' + f: {'model': 'rotarycraft:block/' + name}
                                                         for f in ('up', 'down', 'north', 'south', 'east', 'west')}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': particle},
                                             'display': BLOCK_DISPLAY})
    w('%s/loot_table/blocks/%s.json' % (D, name), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def item(i):
    return {'item': 'rotarycraft:' + i}


def tag(t):
    return {'tag': t}


def vanilla(i):
    return {'item': 'minecraft:' + i}


STEEL = tag('c:ingots/steel')
PANEL = item('base_panel')
IMPELLER = item('impeller')
PIPE = item('pipe')
SHAFT = item('shaft_core')
PLANKS = tag('minecraft:planks')


def shaped(name, pattern, key, count=1, result=None, suffix=''):
    w('%s/recipe/%s%s.json' % (D, name, suffix), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                                  'result': {'id': 'rotarycraft:' + (result or name), 'count': count}})


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))

# ---- Fan ----
model_texture('fantex.png', 'fan')
model_texture('fantex_wide.png', 'fan_wide')
rendered_machine('fan')
MODELS.append('ModelFan:fan')
shaped('fan', ['WWW', 'WIW', '#s#'], {'#': PANEL, 'W': PLANKS, 'I': IMPELLER, 's': SHAFT})

# ---- Sprinkler ----
model_texture('sprinklertex.png', 'sprinkler')
rendered_machine('sprinkler')
MODELS.append('ModelSprinkler:sprinkler')
shaped('sprinkler', [' s ', ' p ', ' i '], {'s': STEEL, 'p': PIPE, 'i': IMPELLER}, count=4)
shaped('sprinkler', [' s ', ' p ', ' i '], {'s': tag('c:ingots/tin'), 'p': PIPE, 'i': IMPELLER}, count=4, suffix='_from_tin')

# ---- Lawn Sprinkler ----
model_texture('lawnsprinklertex.png', 'lawn_sprinkler')
rendered_machine('lawn_sprinkler')
MODELS.append('ModelLawnSprinkler:lawn_sprinkler')
shaped('lawn_sprinkler', ['PPP', ' P ', 'BIB'], {'P': PIPE, 'B': PANEL, 'I': IMPELLER})

# ---- Ground Hydrator (drawn from the reservoir's model, as the original) ----
model_texture('hydratortex.png', 'ground_hydrator')
rendered_machine('ground_hydrator')
MODELS.append('ModelReservoir:reservoir')
shaped('ground_hydrator', ['sls', 'p p', 'PpP'], {'s': STEEL, 'l': vanilla('ladder'), 'p': PLANKS, 'P': PANEL})

# ---- Fertilizer ----
model_texture('fertilizertex.png', 'fertilizer')
rendered_machine('fertilizer')
MODELS.append('ModelFertilizer:fertilizer')
shaped('fertilizer', ['PIP', ' S ', 'BCB'], {'P': PIPE, 'S': SHAFT, 'I': IMPELLER, 'C': vanilla('chest'), 'B': PANEL})

# ---- Defoliator ----
model_texture('defoliatortex.png', 'defoliator')
rendered_machine('defoliator')
MODELS.append('ModelDefoliator:defoliator')
os.makedirs(T + '/gui', exist_ok=True)
shutil.copy(REF + '/Textures/GUI/defoliatorgui.png', T + '/gui/defoliator.png')
shaped('defoliator', ['P P', 'SPS', 'BIB'], {'P': PIPE, 'S': STEEL, 'B': PANEL, 'I': IMPELLER})

# ---- Item Pump (the original's Blower), a plain block with a front and a back ----
from PIL import Image, ImageDraw

BLOCK_TEX = T + '/block'
side = Image.open(BLOCK_TEX + '/machine_side.png').convert('RGBA')


def plain_machine(name, front):
    front.save('%s/%s_front.png' % (BLOCK_TEX, name))
    MACHINES.append(name)
    w('%s/models/block/%s.json' % (A, name), {'parent': 'minecraft:block/cube', 'textures': {
        'north': 'rotarycraft:block/%s_front' % name, 'south': 'rotarycraft:block/%s_back' % name, 'east': 'rotarycraft:block/machine_side',
        'west': 'rotarycraft:block/machine_side', 'up': 'rotarycraft:block/machine_side', 'down': 'rotarycraft:block/machine_side',
        'particle': 'rotarycraft:block/machine_side'}})
    rot = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    w('%s/blockstates/%s.json' % (A, name), {'variants': {'facing=' + f: dict({'model': 'rotarycraft:block/' + name}, **r) for f, r in rot.items()}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'rotarycraft:block/' + name})
    w('%s/loot_table/blocks/%s.json' % (D, name), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


front = side.copy()
g = ImageDraw.Draw(front)
g.rectangle([3, 3, 12, 12], fill=(30, 30, 34, 255), outline=(150, 154, 162, 255))
g.polygon([(5, 8), (9, 4), (9, 7), (11, 7), (11, 9), (9, 9), (9, 12)], fill=(200, 205, 215, 255))
back = side.copy()
g = ImageDraw.Draw(back)
g.rectangle([3, 3, 12, 12], fill=(70, 72, 78, 255), outline=(150, 154, 162, 255))
g.polygon([(11, 8), (7, 4), (7, 7), (5, 7), (5, 9), (7, 9), (7, 12)], fill=(120, 124, 134, 255))
back.save(BLOCK_TEX + '/blower_back.png')
plain_machine('blower', front)
shutil.copy(REF + '/Textures/GUI/blowergui.png', T + '/gui/blower.png')
shaped('blower', ['BBB', 'PIP', 'BBB'], {'B': PANEL, 'I': IMPELLER, 'P': PIPE}, count=16)

# ---- Item Vacuum ----
model_texture('vactex.png', 'vacuum')
rendered_machine('vacuum')
MODELS.append('ModelVacuum:vacuum')
shaped('vacuum', ['SwS', 'wIw', 'SCS'], {'C': vanilla('chest'), 'S': STEEL, 'I': IMPELLER, 'w': vanilla('black_wool')})

# ---- Auto-Breeder ----
model_texture('breedertex.png', 'auto_breeder')
model_texture('emptybreedertex.png', 'auto_breeder_empty')
rendered_machine('auto_breeder')
MODELS.append('ModelBreeder:auto_breeder')
shaped('auto_breeder', ['B B', 'BBB'], {'B': PANEL})

# ---- Bait Box ----
model_texture('baitboxtex.png', 'bait_box')
rendered_machine('bait_box')
MODELS.append('ModelBaitBox:bait_box')
shaped('bait_box', ['BBB', 'BAB', 'BBB'], {'B': vanilla('iron_bars'), 'A': item('auto_breeder')})

# ---- Mob Harvester ----
model_texture('harvestertex.png', 'mob_harvester')
rendered_machine('mob_harvester')
MODELS.append('ModelHarvester:mob_harvester')
shaped('mob_harvester', ['shs', 'sps'], {'h': item('igniter'), 'p': vanilla('ender_pearl'), 's': PANEL})

# ---- Spawner Controller ----
model_texture('spawnertex.png', 'spawner_controller')
rendered_machine('spawner_controller')
MODELS.append('ModelSpawner:spawner_controller')
shutil.copy(REF + '/Textures/GUI/spawnercontrollergui.png', T + '/gui/spawner_controller.png')
shaped('spawner_controller', ['PCP', 'OGO', 'g g'], {'O': vanilla('obsidian'), 'P': PANEL, 'G': tag('c:ingots/gold'), 'g': vanilla('glowstone'),
                                                       'C': item('circuit_board')})

lang.update({
    'block.rotarycraft.fan': 'Fan',
    'block.rotarycraft.sprinkler': 'Sprinkler',
    'block.rotarycraft.lawn_sprinkler': 'Lawn Sprinkler',
    'block.rotarycraft.ground_hydrator': 'Ground Hydrator',
    'block.rotarycraft.fertilizer': 'Fertilizer',
    'block.rotarycraft.defoliator': 'Defoliator',
    'block.rotarycraft.blower': 'Item Pump',
    'block.rotarycraft.vacuum': 'Item Vacuum',
    'block.rotarycraft.auto_breeder': 'Auto-Breeder',
    'block.rotarycraft.bait_box': 'Bait Box',
    'block.rotarycraft.mob_harvester': 'Mob Harvester',
    'block.rotarycraft.spawner_controller': 'Spawner Controller',
    'gui.rotarycraft.farm.poison': 'Poison: %s / %s mB',
    'gui.rotarycraft.farm.spawner.disable': 'Disable/Enable',
    'gui.rotarycraft.farm.spawner.delay': 'Spawn Delay:',
    'gui.rotarycraft.farm.spawner.no_spawner': 'No spawner below',
    'gui.rotarycraft.farm.whitelist': 'Whitelist',
    'gui.rotarycraft.farm.blacklist': 'Blacklist',
    'gui.rotarycraft.farm.metadata_on': 'Match damage',
    'gui.rotarycraft.farm.metadata_off': 'Ignore damage',
    'gui.rotarycraft.farm.nbt_on': 'Match data',
    'gui.rotarycraft.farm.nbt_off': 'Ignore data',
    'gui.rotarycraft.farm.exact': 'Match exactly',
    'gui.rotarycraft.farm.tags': 'Match any shared tag',
    'gui.rotarycraft.farm.power': 'Power: %s N*m x %s rad/s = %s W',
    'gui.rotarycraft.farm.water': 'Water: %s / %s mB',
    'gui.rotarycraft.farm.range': 'Range: %s blocks',
    'gui.rotarycraft.farm.tries': 'Tries a tick: %s',
    'menu.rotarycraft.farm_machine': 'Machine',
})
w(lang_path, lang)

path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += ['rotarycraft:' + m for m in MACHINES if 'rotarycraft:' + m not in data['values']]
w(path, data)

subprocess.run([sys.executable, 'tools/modelbase2json.py'] + MODELS, check=True)
