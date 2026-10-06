"""World machines: chunk loader, weather controller, terraformer, borers (run by gen_assets.py, after gen_weapons.py): the original's model
textures, blockstates and models (the machines are drawn by their renderers), loot, tags, recipes and lang."""
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

# ---- Chunk Loader ----
model_texture('chunkloadertex.png', 'chunk_loader')
rendered_machine('chunk_loader')
MODELS.append('ModelChunkLoader:chunk_loader')
shaped('chunk_loader', ['sSs', 'BSB', 'PGP'], {'s': {'item': 'minecraft:nether_star'}, 'S': item('bedrock_shaft_core'), 'B': STEEL,
                                               'P': item('base_panel'), 'G': item('bedrock_gear_unit_16')})

lang.update({
    'block.rotarycraft.chunk_loader': 'Chunk Loader',
})
w(lang_path, lang)

path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += ['rotarycraft:' + m for m in MACHINES if 'rotarycraft:' + m not in data['values']]
w(path, data)

subprocess.run([sys.executable, 'tools/modelbase2json.py'] + MODELS, check=True)
