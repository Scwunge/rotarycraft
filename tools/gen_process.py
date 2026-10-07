"""The fluid and process machines (run by gen_assets.py, after gen_models.py): the original's model textures, blockstates and models (the machines
are drawn by their renderers), loot, tags, recipes and lang. Add a machine by appending to the lists below."""
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
MACHINES = []
MODELS = []


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def model_texture(src, name):
    os.makedirs(T + '/machine', exist_ok=True)
    shutil.copy('%s/Textures/TileEntityTex/%s' % (REF, src), '%s/machine/%s.png' % (T, name))


def rendered_machine(name, particle='rotarycraft:block/machine_side'):
    """A machine drawn entirely by its renderer: the block model only gives break particles; the item is drawn by the renderer too."""
    MACHINES.append(name)
    w('%s/models/block/%s.json' % (A, name), {'textures': {'particle': particle}})
    w('%s/blockstates/%s.json' % (A, name), {'variants': {'facing=' + f: {'model': 'rotarycraft:block/' + name}
                                                         for f in ('up', 'down', 'north', 'south', 'east', 'west')}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': particle}, 'display': DISPLAY})
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
GLASS = tag('c:glass_blocks')


def shaped(name, pattern, key, count=1, result=None, suffix=''):
    w('%s/recipe/%s%s.json' % (D, name, suffix), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                                  'result': {'id': 'rotarycraft:' + (result or name), 'count': count}})


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))

# ---- the c:steam fluid tag ----
w(R + '/data/c/tags/fluid/steam.json', {'values': ['rotarycraft:steam', 'rotarycraft:flowing_steam']})

# ---- Boiler, Steam Turbine, Air Compressor, Pneumatic Engine, Magnetic Motor, Dynamo ----
for block, model, tex, name in [('boiler', 'ModelBoiler', 'Converter/boilertex.png', 'boiler'),
                                ('steam_turbine', 'ModelSteamTurbine', 'Converter/steamturbtex.png', 'steam_turbine'),
                                ('air_compressor', 'ModelCompressor', 'Converter/airtex.png', 'air_compressor'),
                                ('pneumatic_engine', 'ModelPneumatic', 'Converter/pneutex.png', 'pneumatic_engine'),
                                ('magnetic_motor', 'ModelMagnetic', 'Converter/magneticmotortex.png', 'magnetic_motor'),
                                ('dynamo', 'ModelDynamo2', 'Converter/dynamotex.png', 'dynamo')]:
    model_texture(tex, name)
    MODELS.append('%s:%s' % (model, block))
    rendered_machine(block)
model_texture('Converter/dynamotex2.png', 'dynamo_running')
shaped('boiler', ['SPS', 'G G', 'SIS'], {'S': STEEL, 'P': PIPE, 'G': GLASS, 'I': IMPELLER})
shaped('steam_turbine', ['SPS', 'GTG', 'ScS'], {'S': STEEL, 'P': PANEL, 'G': GLASS, 'T': item('turbine'), 'c': item('diamond_shaft_core')})
shaped('air_compressor', ['SSS', ' G ', 'CPC'], {'S': STEEL, 'G': GLASS, 'C': item('compressor'), 'P': vanilla('piston')})
shaped('pneumatic_engine', ['ppS', 'sT ', 'PPP'], {'S': STEEL, 'p': PIPE, 's': SHAFT, 'P': PANEL, 'T': IMPELLER})
shaped('magnetic_motor', ['LCl', 'scs', 'PSP'], {'L': tag('c:ingots/iron'), 'l': tag('c:ingots/iron'), 'C': item('gold_coil'), 'c': tag('c:ingots/copper'),
                                                  's': tag('c:ingots/silver'), 'P': PANEL, 'S': item('diamond_shaft_core')})
shaped('dynamo', [' C ', 'GSG', 'SRS'], {'C': item('power_module'), 'G': item('steel_gear'), 'S': STEEL, 'R': vanilla('redstone')})

lang.update({
    'block.rotarycraft.boiler': 'Friction Boiler',
    'block.rotarycraft.steam_turbine': 'Steam Turbine',
    'block.rotarycraft.air_compressor': 'Air Compressor',
    'block.rotarycraft.pneumatic_engine': 'Pneumatic Engine',
    'block.rotarycraft.magnetic_motor': 'Magnetic Motor',
    'block.rotarycraft.dynamo': 'Dynamo',
})
w(lang_path, lang)

path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += ['rotarycraft:' + m for m in MACHINES if 'rotarycraft:' + m not in data['values']]
w(path, data)

subprocess.run([sys.executable, 'tools/modelbase2json.py'] + MODELS, check=True)
