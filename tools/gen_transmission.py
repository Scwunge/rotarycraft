"""Transmission pieces beyond shafts, gears and clutches: multi-clutch and what follows (run by gen_assets.py, after gen_crafting.py): the original's
model textures and models, blockstates and models that leave the drawing to the renderers, GUI textures, loot, recipes and lang."""
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

DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}
MODELS = []
LANG = {}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def texture(src, name):
    os.makedirs(T + '/machine', exist_ok=True)
    shutil.copy('%s/Textures/TileEntityTex/%s' % (REF, src), '%s/machine/%s.png' % (T, name))


def gui(src, name):
    os.makedirs(T + '/gui', exist_ok=True)
    shutil.copy('%s/Textures/GUI/%s' % (REF, src), '%s/gui/%s.png' % (T, name))


def rendered(block, plain=False):
    """A block drawn entirely by its renderer: the block model only gives break particles; the item is drawn by the renderer too."""
    particle = 'rotarycraft:block/machine_side'
    w('%s/models/block/%s.json' % (A, block), {'textures': {'particle': particle}})
    if plain:
        w('%s/blockstates/%s.json' % (A, block), {'variants': {'': {'model': 'rotarycraft:block/' + block}}})
    else:
        w('%s/blockstates/%s.json' % (A, block), {'variants': {'facing=' + f: {'model': 'rotarycraft:block/' + block}
                                                              for f in ('up', 'down', 'north', 'south', 'east', 'west')}})
    w('%s/models/item/%s.json' % (A, block), {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': particle}, 'display': DISPLAY})
    w('%s/loot_table/blocks/%s.json' % (D, block), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + block}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
    PICKAXE.append('rotarycraft:' + block)


def shaped(name, pattern, key, count=1):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                        'result': {'id': 'rotarycraft:' + name, 'count': count}})


def item(i):
    return {'item': 'rotarycraft:' + i}


STEEL = {'tag': 'c:ingots/steel'}
PICKAXE = []

# ---- Multi-Clutch ----
texture('Transmission/multiclutchtex.png', 'multi_clutch')
MODELS.append('ModelMultiClutch:multi_clutch')
rendered('multi_clutch')
gui('multigui.png', 'multi_clutch')
shaped('multi_clutch', ['PSP', 'SGS', 'RSR'], {'P': item('base_panel'), 'S': item('shaft_steel'), 'G': item('steel_gear_unit_2'), 'R': {'item': 'minecraft:redstone'}})
LANG.update({'block.rotarycraft.multi_clutch': 'Multi-Clutch'})

# ---- lang, tags, models ----
lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update(LANG)
w(lang_path, lang)
path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += [v for v in PICKAXE if v not in data['values']]
w(path, data)
subprocess.run([sys.executable, 'tools/modelbase2json.py'] + MODELS, check=True)
print('transmission ok')
