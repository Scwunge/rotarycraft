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

lang.update({
    'block.rotarycraft.fan': 'Fan',
    'block.rotarycraft.sprinkler': 'Sprinkler',
    'block.rotarycraft.lawn_sprinkler': 'Lawn Sprinkler',
    'block.rotarycraft.ground_hydrator': 'Ground Hydrator',
    'block.rotarycraft.fertilizer': 'Fertilizer',
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
