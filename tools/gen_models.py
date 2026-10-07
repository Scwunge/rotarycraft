"""Real models for the machines that were first drawn as plain cubes (run by gen_assets.py, after the others): the original's model
classes (converted with their render programs), their textures, and the blockstates, block models and item models that leave the drawing to
the renderers (client/machine/ModelMachinesClient)."""
import json
import os
import shutil
import subprocess
import sys

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
T = A + '/textures'
REF = 'reference/RotaryCraft/Textures/TileEntityTex'

DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def texture(src, name):
    os.makedirs(T + '/machine', exist_ok=True)
    shutil.copy('%s/%s' % (REF, src), '%s/machine/%s.png' % (T, name))


def rendered(block):
    """Leaves a block to its renderer: the block model gives only its break particles, the item is drawn by the renderer."""
    particle = 'rotarycraft:block/machine_side'
    w('%s/models/block/%s.json' % (A, block), {'textures': {'particle': particle}})
    w('%s/blockstates/%s.json' % (A, block), {'variants': {'facing=' + f: {'model': 'rotarycraft:block/' + block}
                                                          for f in ('up', 'down', 'north', 'south', 'east', 'west')}})
    w('%s/models/item/%s.json' % (A, block), {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': particle}, 'display': DISPLAY})


MODELS = []

# ---- Engines ----
for block, model, tex, name in [('dc_engine', 'ModelDC', 'Engine/dc.png', 'engine_dc'), ('ac_engine', 'ModelAC', 'Engine/actex.png', 'engine_ac'),
                                ('gas_engine', 'ModelCombustion', 'Engine/combtex.png', 'engine_gas'),
                                ('performance_engine', 'ModelPerformance', 'Engine/perftex.png', 'engine_performance'),
                                ('steam_engine', 'ModelSteam', 'Engine/steamtex.png', 'engine_steam'), ('hydro_engine', 'ModelHydro', 'Engine/hydrotex.png', 'engine_hydro'),
                                ('wind_engine', 'ModelWind', 'Engine/windtex.png', 'engine_wind'), ('microturbine', 'ModelMicroTurbine', 'Engine/microtex.png', 'engine_micro'),
                                ('jet_engine', 'ModelJet', 'Engine/jettex.png', 'engine_jet')]:
    texture(tex, name)
    MODELS.append('%s:%s' % (model, block))
    rendered(block)
texture('Engine/bedhydrotex.png', 'engine_hydro_bedrock')
for k in range(1, 6):
    texture('Friction/frictiontex-%d.png' % k, 'friction_heater_%d' % k)
for k, name in enumerate(('hot-1', 'hot0', 'hot2', 'hot3'), 1):
    texture('PulseJet/pulsetex%s.png' % name, 'pulse_furnace_%d' % k)
texture('Engine/jettex_b.png', 'engine_jet_afterburner')

# ---- Transmission ----
texture('Transmission/Shaft/shafttex.png', 'clutch')
MODELS += ['ModelClutch:clutch', 'ModelVClutch:vclutch']
rendered('clutch')
MODELS.append('ModelFlywheel:flywheel')
for t in ('wood', 'stone', 'iron', 'gold', 'bedrock'):
    texture('Transmission/Flywheel/%s.png' % t, 'flywheel_' + t)
    rendered('flywheel_' + t)
for ratio, model in ((2, 'ModelGearbox'), (4, 'ModelGearbox4'), (8, 'ModelGearbox8'), (16, 'ModelGearbox16')):
    MODELS.append('%s:gearbox_%d' % (model, ratio))
for material, suffix in (('wood', 'w'), ('stone', 's'), ('steel', ''), ('diamond', 'd'), ('bedrock', 'b'), ('tungsten', 't')):
    texture('Transmission/Gear/geartex%s.png' % suffix, 'gearbox_' + material)
    for ratio in (2, 4, 8, 16):
        rendered('gearbox_%s_%dx' % (material, ratio))

# ---- Machines ----
for block, model, tex in [('pump', 'ModelPump', 'pumptex.png'), ('friction_heater', 'ModelFriction', 'Friction/frictiontex.png'),
                          ('crystallizer', 'ModelCrystallizer', 'crystaltex.png'), ('refrigerator', 'ModelFridge', 'fridgetex.png'),
                          ('grinder', 'ModelGrinder', 'grindertex.png'), ('magnetizer', 'ModelMagnetizer', 'magnettex.png'),
                          ('fractionator', 'ModelFraction', 'fractex.png'), ('pulse_furnace', 'ModelPulseFurnace', 'PulseJet/pulsetex.png'),
                          ('rock_melter', 'ModelLavaMaker', 'lavamakertex.png'), ('extractor', 'ModelExtractor', 'extractortex.png'),
                          ('compactor', 'ModelCompactor', 'compactortex.png'), ('centrifuge', 'ModelCentrifuge', 'centrifugetex.png'),
                          ('electric_motor', 'ModelElecMotor', 'Converter/elecmotortex.png'), ('generator', 'ModelGenerator', 'Converter/generatortex.png')]:
    texture(tex, block)
    MODELS.append('%s:%s' % (model, block))
    rendered(block)

# ---- Bevel gear and splitter: their blocks have one blockstate for every arrangement of shafts; the renderers do the turning ----
import re

for block in ('bevel_gear', 'splitter'):
    w('%s/blockstates/%s.json' % (A, block), {'variants': {'': {'model': 'rotarycraft:block/' + block}}})
    w('%s/models/block/%s.json' % (A, block), {'textures': {'particle': 'rotarycraft:block/machine_side'}})
    w('%s/models/item/%s.json' % (A, block), {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': 'rotarycraft:block/machine_side'}, 'display': DISPLAY})
texture('Transmission/beveltex.png', 'bevel_gear')
texture('Transmission/splittertex.png', 'splitter')
texture('Transmission/bedsplittertex.png', 'splitter_bedrock')
MODELS += ['ModelBevel:bevel_gear', 'ModelSplitter:splitter', 'ModelSplitter2:splitter2']

# the bevel gear's table: which shafts (in, out) it joins, and how the original turned its model for them
src = open('reference/RotaryCraft/TileEntities/Transmission/TileEntityBevelGear.java', encoding='utf-8', errors='replace').read()
blk = src[src.index('case 0://-x'):src.index('directions.put')]
parts = re.split(r'case (\d+):', blk)
ios = {}
for k in range(1, len(parts), 2):
    body = parts[k + 1]
    ios[int(parts[k])] = (re.search(r'read = ForgeDirection\.(\w+)', body).group(1).lower(), re.search(r'write = ForgeDirection\.(\w+)', body).group(1).lower())
src = open('reference/RotaryCraft/Renders/RenderBevel.java', encoding='utf-8', errors='replace').read()
blk = src[src.index('switch(tile.direction)'):src.index('GL11.glRotatef(var11')]
parts = re.split(r'case (\d+):', blk)
table = []
for k in range(1, len(parts), 2):
    n = int(parts[k])
    body = parts[k + 1]
    v = re.search(r'var11 = (-?\d+); var12 = (-?\d+)', body)
    t = re.search(r'glTranslatef\((-?\d+)F, (-?\d+)F, (-?\d+)F\)', body)
    table.append({'read': ios[n][0], 'write': ios[n][1], 'y': int(v.group(1)), 'x': int(v.group(2)), 't': [int(g) for g in t.groups()] if t else [0, 0, 0],
                  'dir': -1 if 'dir = -1' in body else 1})
os.makedirs(A + '/reika_models', exist_ok=True)
w(A + '/reika_models/bevel_orientations.json', table)

subprocess.run([sys.executable, 'tools/modelbase2json.py'] + MODELS, check=True)
