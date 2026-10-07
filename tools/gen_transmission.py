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

# ---- Distribution Clutch ----
texture('Transmission/distribclutchtex.png', 'distribution_clutch')
MODELS.append('ModelDistribClutch:distribution_clutch')
rendered('distribution_clutch')
gui('distribclutchgui.png', 'distribution_clutch')
shaped('distribution_clutch', ['sgs', 'SGS', 'PrP'], {'s': STEEL, 'g': item('steel_gear'), 'S': item('shaft_steel'), 'G': item('steel_gear_unit_2'),
                                                      'P': item('base_panel'), 'r': item('circuit_board')})
LANG.update({'block.rotarycraft.distribution_clutch': 'Distribution Clutch',
             'gui.rotarycraft.distribution_clutch.gui': 'Control: Screen',
             'gui.rotarycraft.distribution_clutch.redstone': 'Control: Redstone',
             'gui.rotarycraft.distribution_clutch.input': 'Input side',
             'gui.rotarycraft.distribution_clutch.front': 'Front: gets what is left over'})

# ---- Power Bus and Bus Controller ----
from PIL import Image, ImageDraw

BLOCK_TEX = T + '/block'
side = Image.open(BLOCK_TEX + '/machine_side.png').convert('RGBA')
bus = side.copy()
g = ImageDraw.Draw(bus)
g.rectangle([1, 1, 14, 14], outline=(52, 54, 60, 255))
g.rectangle([6, 0, 9, 15], fill=(96, 100, 108, 255))
g.rectangle([0, 6, 15, 9], fill=(96, 100, 108, 255))
g.rectangle([5, 5, 10, 10], fill=(60, 62, 68, 255), outline=(190, 194, 200, 255))
bus.save(BLOCK_TEX + '/power_bus.png')
w(A + '/models/block/power_bus.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'rotarycraft:block/power_bus'}})
w(A + '/blockstates/power_bus.json', {'variants': {'': {'model': 'rotarycraft:block/power_bus'}}})
w(A + '/models/item/power_bus.json', {'parent': 'rotarycraft:block/power_bus'})
w(D + '/loot_table/blocks/power_bus.json', {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:power_bus'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
front = side.copy()
g = ImageDraw.Draw(front)
g.ellipse([2, 2, 13, 13], fill=(40, 44, 52, 255), outline=(190, 194, 200, 255))
g.line([(4, 8), (11, 8)], fill=(80, 200, 90, 255))
g.line([(9, 6), (11, 8), (9, 10)], fill=(80, 200, 90, 255))
front.save(BLOCK_TEX + '/bus_controller_front.png')
w(A + '/models/block/bus_controller.json', {'parent': 'minecraft:block/cube', 'textures': {
    'north': 'rotarycraft:block/bus_controller_front', 'south': 'rotarycraft:block/machine_back', 'east': 'rotarycraft:block/machine_side',
    'west': 'rotarycraft:block/machine_side', 'up': 'rotarycraft:block/machine_side', 'down': 'rotarycraft:block/machine_side', 'particle': 'rotarycraft:block/machine_side'}})
ROT = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
w(A + '/blockstates/bus_controller.json', {'variants': {'facing=' + f: dict({'model': 'rotarycraft:block/bus_controller'}, **r) for f, r in ROT.items()}})
w(A + '/models/item/bus_controller.json', {'parent': 'rotarycraft:block/bus_controller'})
w(D + '/loot_table/blocks/bus_controller.json', {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:bus_controller'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
PICKAXE += ['rotarycraft:power_bus', 'rotarycraft:bus_controller']
gui('bus.png', 'power_bus')
shaped('power_bus', ['SMS', 'MCM', 'SMS'], {'S': STEEL, 'M': item('steel_bearing'), 'C': item('belt')}, 4)
shaped('bus_controller', ['SMS', 'MCM', 'SMS'], {'S': STEEL, 'M': item('steel_bearing'), 'C': item('circuit_board')})
LANG.update({'block.rotarycraft.power_bus': 'Power Bus', 'block.rotarycraft.bus_controller': 'Bus Controller',
             'gui.rotarycraft.bus.torque_mode': 'Torque mode: trades speed for torque',
             'gui.rotarycraft.bus.speed_mode': 'Speed mode: trades torque for speed',
             'message.rotarycraft.bus.status': 'Bus: %s blocks, %s output sides, %s N*m at %s rad/s, lubricant %s / %s mB'})

# ---- Engine Control Unit ----
ecu_top = side.copy()
g = ImageDraw.Draw(ecu_top)
g.rectangle([2, 2, 13, 13], fill=(40, 44, 52, 255), outline=(190, 194, 200, 255))
for k in range(4):
    g.line([(4, 4 + 3 * k), (11, 4 + 3 * k)], fill=(80, 200, 90, 255) if k != 1 else (230, 200, 60, 255))
ecu_top.save(BLOCK_TEX + '/ecu_top.png')
ecu_side = side.copy()
g = ImageDraw.Draw(ecu_side)
g.rectangle([3, 5, 12, 10], fill=(40, 44, 52, 255), outline=(190, 194, 200, 255))
g.line([(5, 8), (10, 8)], fill=(230, 200, 60, 255))
ecu_side.save(BLOCK_TEX + '/ecu_side.png')
w(A + '/models/block/engine_controller.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': 'rotarycraft:block/ecu_top', 'side': 'rotarycraft:block/ecu_side', 'bottom': 'rotarycraft:block/ecu_top', 'particle': 'rotarycraft:block/ecu_side'}})
w(A + '/blockstates/engine_controller.json', {'variants': {'': {'model': 'rotarycraft:block/engine_controller'}}})
w(A + '/models/item/engine_controller.json', {'parent': 'rotarycraft:block/engine_controller'})
w(D + '/loot_table/blocks/engine_controller.json', {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:engine_controller'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
PICKAXE.append('rotarycraft:engine_controller')
shaped('engine_controller', ['IPI', 'IGI', 'IRI'], {'I': STEEL, 'G': {'item': 'minecraft:gold_ingot'}, 'P': item('circuit_board'), 'R': {'item': 'minecraft:redstone'}})
LANG.update({'block.rotarycraft.engine_controller': 'Engine Control Unit',
             'message.rotarycraft.ecu.setting.shutdown': 'Shutdown', 'message.rotarycraft.ecu.setting.standby': 'Standby',
             'message.rotarycraft.ecu.setting.low': 'Low', 'message.rotarycraft.ecu.setting.medium': 'Medium', 'message.rotarycraft.ecu.setting.full': 'Full',
             'message.rotarycraft.ecu.set': 'ECU set to %s (%s%% speed).',
             'message.rotarycraft.ecu.redstone': 'ECU is now redstone-operated.',
             'message.rotarycraft.ecu.manual': 'ECU is now manually controlled.',
             'message.rotarycraft.ecu.status': 'ECU: %s (%s%% speed), fuel %s / %s mB'})

# ---- Belt, Chain and Split Belt pulleys ----
texture('Transmission/belttex.png', 'belt_hub')
MODELS.append('ModelBelt:belt_hub')
for name, color in (('belt', (192, 120, 70)), ('chain', (80, 80, 80)), ('split_belt', (48, 96, 64))):
    Image.new('RGBA', (16, 16), color + (255,)).save(T + '/machine/belt_band_%s.png' % name)
for block in ('belt_hub', 'chain_drive', 'split_belt'):
    rendered(block)
shaped('belt_hub', ['sBs', ' G ', 'sBs'], {'s': STEEL, 'B': item('base_panel'), 'G': item('hub')}, 2)
shaped('chain_drive', ['sBs', ' G ', 'sBs'], {'s': STEEL, 'B': item('base_panel'), 'G': item('steel_gear')}, 2)
shaped('split_belt', [' B ', 'SgS', ' B '], {'S': item('shaft_core'), 'B': item('belt_hub'), 'g': item('steel_gear')}, 2)
LANG.update({'block.rotarycraft.belt_hub': 'Belt Pulley', 'block.rotarycraft.chain_drive': 'Chain Drive', 'block.rotarycraft.split_belt': 'Split Belt Pulley',
             'message.rotarycraft.belt.first': 'First pulley chosen: use the belt on the other one.',
             'message.rotarycraft.belt.lost': 'The first pulley is gone.',
             'message.rotarycraft.belt.short': 'Not enough belt: it needs %s.',
             'message.rotarycraft.belt.invalid': 'The pulleys cannot be joined like that.',
             'message.rotarycraft.belt.receiving': 'This pulley now receives the belt.',
             'message.rotarycraft.belt.driving': 'This pulley now drives the belt.'})

# ---- Advanced gears: the worm gear ----
MODELS.append('ModelWorm:worm_gear')
rendered('worm_drive')
shaped('worm_drive', ['SW ', ' GS', ' M '], {'S': item('shaft_core'), 'W': item('worm_gear'), 'G': item('steel_gear'), 'M': item('mount')})
LANG.update({'block.rotarycraft.worm_drive': 'Worm Gear'})

# ---- the CVT ----
texture('Transmission/cvttex.png', 'cvt')
MODELS.append('ModelCVT:cvt')
rendered('cvt')
gui('cvtgui.png', 'cvt')
gui('cvtgui2.png', 'cvt_redstone')
shaped('cvt', ['BSB', 'BSB', 'sMc'], {'B': item('diamond_bearing'), 'S': item('bedrock_shaft_core'), 's': item('circuit_board'), 'M': item('mount'), 'c': item('screen')})
LANG.update({'block.rotarycraft.cvt': 'CVT'})

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
