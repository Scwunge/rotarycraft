"""Surveying and display machines (run by gen_assets.py after gen_weapons.py): the original's model textures, screens and icon sheets,
blockstates and models (the machines are drawn by their renderers), loot, recipes and lang."""
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
MACHINES = []


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def model_texture(src, name):
    os.makedirs(T + '/machine', exist_ok=True)
    shutil.copy('%s/Textures/TileEntityTex/%s' % (REF, src), '%s/machine/%s.png' % (T, name))


def gui(src, name):
    os.makedirs(T + '/gui', exist_ok=True)
    shutil.copy('%s/Textures/GUI/%s' % (REF, src), '%s/gui/%s.png' % (T, name))


BLOCK_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.4, 0.4, 0.4]},
}


def loot(name):
    w('%s/loot_table/blocks/%s.json' % (D, name), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def rendered_machine(name, facings=('up', 'down', 'north', 'south', 'east', 'west'), particle='rotarycraft:block/shaft_steel'):
    """A machine drawn entirely by its renderer: the block model only gives break particles; the item is drawn by the renderer too."""
    MACHINES.append(name)
    w('%s/models/block/%s.json' % (A, name), {'textures': {'particle': particle}})
    w('%s/blockstates/%s.json' % (A, name), {'variants': {'': {'model': 'rotarycraft:block/' + name}} if facings is None else {'facing=' + f: {'model': 'rotarycraft:block/' + name} for f in facings}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:builtin/entity', 'gui_light': 'side', 'textures': {'particle': particle},
                                             'display': BLOCK_DISPLAY})
    loot(name)


def item(i):
    return {'item': 'rotarycraft:' + i}


def tag(t):
    return {'tag': t}


STEEL = tag('c:ingots/steel')


def shaped(name, pattern, key, count=1):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
                                        'result': {'id': 'rotarycraft:' + name, 'count': count}})


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))

# ---- Mob Radar ----
model_texture('radartex.png', 'mob_radar')
gui('mobradargui.png', 'mob_radar')
gui('mobicons.png', 'mob_icons')
rendered_machine('mob_radar')
shaped('mob_radar', [' rs', ' g ', 'pcp'], {'r': item('radar_unit'), 's': item('screen'), 'c': item('circuit_board'), 'g': item('steel_gear_unit_2'),
                                           'p': item('base_panel')})

# ---- Ground-Penetrating Radar (the original's blocks are cut from its old terrain sheet: 81 sides, 82 top, 83 bottom) ----
sheet = Image.open(REF + '/Textures/Terrain/textures.png').convert('RGBA')
for tile, name in ((81, 'gpr_side'), (82, 'gpr_top'), (83, 'gpr_bottom')):
    sheet.crop(((tile % 16) * 16, (tile // 16) * 16, (tile % 16 + 1) * 16, (tile // 16 + 1) * 16)).save('%s/block/%s.png' % (T, name))
w(A + '/models/block/gpr.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': 'rotarycraft:block/gpr_top', 'bottom': 'rotarycraft:block/gpr_bottom', 'side': 'rotarycraft:block/gpr_side'}})
w(A + '/blockstates/gpr.json', {'variants': {'facing=' + f: {'model': 'rotarycraft:block/gpr'} for f in ('up', 'down', 'north', 'south', 'east', 'west')}})
w(A + '/models/item/gpr.json', {'parent': 'rotarycraft:block/gpr'})
loot('gpr')
MACHINES.append('gpr')
gui('gprgui.png', 'gpr')
shaped('gpr', ['SsS', 'PCP', 'SRS'], {'S': STEEL, 's': item('screen'), 'P': item('base_panel'), 'R': item('radar_unit'), 'C': item('circuit_board')})

# ---- Cave Scanner ----
model_texture('cavetex.png', 'cave_scanner')
rendered_machine('cave_scanner')
shaped('cave_scanner', ['sps', 'pcp', 'sns'], {'n': item('sonar_unit'), 's': STEEL, 'c': item('circuit_board'), 'p': item('base_panel')})

# ---- CCTV, Spy Cam and the CCTV Screen ----
model_texture('cctvtex.png', 'cctv')
model_texture('spycamtex.png', 'spy_cam')
model_texture('screentex.png', 'cctv_screen')
gui('cctvgui.png', 'cctv')
gui('spycamgui.png', 'spy_cam')
gui('screengui.png', 'cctv_screen')
rendered_machine('cctv', facings=None)
rendered_machine('spy_cam', facings=None)
rendered_machine('cctv_screen')
PANE = {'tag': 'c:glass_panes'}
shaped('cctv', [' g ', 'brs', ' p '], {'p': item('base_panel'), 's': STEEL, 'b': PANE, 'r': {'item': 'minecraft:redstone'}, 'g': {'tag': 'c:ingots/gold'}})
shaped('spy_cam', ['SCS', 'PRP', 'SGS'], {'P': item('base_panel'), 'S': STEEL, 'C': item('circuit_board'), 'G': PANE, 'R': {'item': 'minecraft:redstone'}})
shaped('cctv_screen', ['sss', 'mcs', 'ppp'], {'p': item('base_panel'), 's': STEEL, 'm': item('screen'), 'c': item('circuit_board')})

# ---- Display ----
model_texture('displaytex.png', 'display')
rendered_machine('display', facings=('north', 'south', 'east', 'west'))
shaped('display', ['SES', 'SCS', ' P '], {'P': item('base_panel'), 'E': item('silicon'), 'S': STEEL, 'C': item('circuit_board')})

# ---- Projector and Slides ----
model_texture('projtex.png', 'projector')
gui('projectorgui.png', 'projector')
os.makedirs(T + '/projector', exist_ok=True)
for i in range(24):
    shutil.copy('%s/Textures/Projector/image%d.png' % (REF, i), '%s/projector/image%d.png' % (T, i))
rendered_machine('projector', facings=('up', 'down', 'north', 'south', 'east', 'west'))
shaped('projector', ['sss', 'gcl', 'ppp'], {'c': item('circuit_board'), 's': STEEL, 'g': {'item': 'minecraft:glass'}, 'l': {'item': 'minecraft:glowstone'}, 'p': item('base_panel')})
sheet0 = Image.open(REF + '/Textures/Items/items.png').convert('RGBA')
slide_icon = sheet0.crop((32, 0, 48, 16))
for i in range(24):
    slide_icon.save('%s/item/slide_%d.png' % (T, i))
    w('%s/models/item/slide_%d.json' % (A, i), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/slide_%d' % i}})
    lang['item.rotarycraft.slide_%d' % i] = 'Slide (%d)' % i
shaped('slide_0', ['PPP', 'PGP', 'PPP'], {'P': {'item': 'minecraft:paper'}, 'G': {'tag': 'c:glass_panes'}}, count=4)
w(D + '/recipe/slide_dye.json', {'type': 'rotarycraft:slide_dye', 'category': 'misc'})

lang.update({
    'item.rotarycraft.slide': 'Slide (%s)',
    'item.rotarycraft.slide.tooltip': 'Goes in a Projector (a dye changes the picture)',
    'block.rotarycraft.projector': 'Projector',
    'block.rotarycraft.display': 'Display',
    'block.rotarycraft.cctv': 'CCTV',
    'block.rotarycraft.spy_cam': 'Spy Cam',
    'block.rotarycraft.cctv_screen': 'CCTV Screen',
    'gui.rotarycraft.camera_select': 'Camera Select',
    'gui.rotarycraft.spy_cam': 'Spy Cam',
    'block.rotarycraft.cave_scanner': 'Cave Scanner',
    'block.rotarycraft.mob_radar': 'Mob Radar',
    'block.rotarycraft.gpr': 'Ground-Penetrating Radar',
})
w(lang_path, lang)

path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += ['rotarycraft:' + m for m in MACHINES if 'rotarycraft:' + m not in data['values']]
w(path, data)

subprocess.run([sys.executable, 'tools/modelbase2json.py', 'ModelRadar:radar', 'ModelCave:cave', 'ModelCCTV:cctv', 'ModelSpyCam:spy_cam', 'ModelScreen:screen', 'ModelDisplay:display', 'ModelProjector:projector'], check=True)
print('survey ok,', len(MACHINES), 'machines')
