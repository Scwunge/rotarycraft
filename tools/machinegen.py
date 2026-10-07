"""Helpers shared by gen_decor.py, gen_logistics.py and gen_items.py: blockstates and models for machines drawn by their renderers, original
textures and GUI pictures, recipes, lang and tags. Run from the project root (gen_assets.py runs the gen_*.py scripts that import this)."""
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
LANG_PATH = A + '/lang/en_us.json'

BLOCK_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}
FACINGS = ('up', 'down', 'north', 'south', 'east', 'west')

MACHINES = []
MODELS = []
LANG = {}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def model_texture(src, name):
    """The original's model texture (Textures/TileEntityTex/<src>) as textures/machine/<name>.png."""
    os.makedirs(T + '/machine', exist_ok=True)
    shutil.copy('%s/Textures/TileEntityTex/%s' % (REF, src), '%s/machine/%s.png' % (T, name))


def gui(src, name):
    """The original's GUI picture (Textures/GUI/<src>) as textures/gui/<name>.png."""
    os.makedirs(T + '/gui', exist_ok=True)
    shutil.copy('%s/Textures/GUI/%s' % (REF, src), '%s/gui/%s.png' % (T, name))


def sprite(sheet, index, name):
    """An item icon cut from the original's sprite sheets (sheet 0 is items.png, sheet n is items<n+1>.png)."""
    from PIL import Image
    os.makedirs(T + '/item', exist_ok=True)
    im = Image.open('%s/Textures/Items/items%s.png' % (REF, '' if sheet == 0 else sheet + 1)).convert('RGBA')
    im.crop(((index % 16) * 16, (index // 16) * 16, (index % 16 + 1) * 16, (index // 16 + 1) * 16)).save('%s/item/%s.png' % (T, name))
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name}})


def loot(name):
    w('%s/loot_table/blocks/%s.json' % (D, name), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def rendered_machine(name, model=None, texture=None, facings=FACINGS, particle='rotarycraft:block/shaft_steel', pickaxe=True):
    """A machine drawn entirely by its renderer: the block model only gives break particles, the item is drawn by the renderer too.
    {@code model} is the original's ModelBase class (converted to reika_models/<name>.json) and {@code texture} the file in TileEntityTex."""
    if pickaxe:
        MACHINES.append(name)
    if texture:
        model_texture(texture, name)
    if model:
        MODELS.append('%s:%s' % (model, name))
    w('%s/models/block/%s.json' % (A, name), {'textures': {'particle': particle}})
    w('%s/blockstates/%s.json' % (A, name), {'variants': {'facing=' + f: {'model': 'rotarycraft:block/' + name} for f in facings}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': particle},
                                             'display': BLOCK_DISPLAY})
    loot(name)


def cube_machine(name, textures, particle=None):
    """A plain textured cube (a facing property is ignored): textures maps cube faces ('up', 'down', 'north', ...) or 'all' to texture names."""
    MACHINES.append(name)
    faces = textures if 'all' not in textures else {f: textures['all'] for f in FACINGS}
    w('%s/models/block/%s.json' % (A, name), {'parent': 'minecraft:block/cube', 'textures': dict({f: 'rotarycraft:block/' + t for f, t in faces.items()},
                                                                                              particle='rotarycraft:block/' + next(iter(faces.values())))})
    w('%s/blockstates/%s.json' % (A, name), {'variants': {'facing=' + f: {'model': 'rotarycraft:block/' + name} for f in FACINGS}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'rotarycraft:block/' + name})
    loot(name)


def item(i):
    return {'item': 'rotarycraft:' + i}


def vanilla(i):
    return {'item': 'minecraft:' + i}


def tag(t):
    return {'tag': t}


STEEL = tag('c:ingots/steel')


def shaped(name, pattern, key, count=1, result=None):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                        'result': {'id': 'rotarycraft:' + (result or name), 'count': count}})


def shapeless(name, ingredients, count=1, result=None):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients,
                                        'result': {'id': 'rotarycraft:' + (result or name), 'count': count}})


def names(entries):
    """Adds English names to the lang file: {'block.rotarycraft.x': 'X', ...}."""
    LANG.update(entries)


def finish():
    """Writes the lang entries, the pickaxe tag and converts the models; call once at the end of a gen script."""
    lang = json.load(open(LANG_PATH))
    lang.update(LANG)
    w(LANG_PATH, lang)
    path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
    data = json.load(open(path))
    data['values'] += ['rotarycraft:' + m for m in MACHINES if 'rotarycraft:' + m not in data['values']]
    w(path, data)
    if MODELS:
        subprocess.run([sys.executable, 'tools/modelbase2json.py'] + MODELS, check=True)
    MACHINES.clear()
    MODELS.clear()
    LANG.clear()
