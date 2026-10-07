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


def sprite(sheet, index, name):
    """An item icon cut from the original's sprite sheets (sheet 0 is items.png, sheet n is items<n+1>.png)."""
    from PIL import Image
    im = Image.open('%s/Textures/Items/items%s.png' % (REF, '' if sheet == 0 else sheet + 1)).convert('RGBA')
    im.crop(((index % 16) * 16, (index // 16) * 16, (index % 16 + 1) * 16, (index // 16 + 1) * 16)).save('%s/item/%s.png' % (T, name))
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name}})


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

# ---- Weather Controller (and the silver iodide it makes rain with) ----
model_texture('iotex.png', 'weather_controller')
rendered_machine('weather_controller')
MODELS.append('ModelIodide:iodide')
os.makedirs(T + '/gui', exist_ok=True)
shutil.copy(REF + '/Textures/GUI/basicstorage.png', T + '/gui/basic_storage.png')
sprite(0, 8 * 16 + 7, 'silver_iodide')
shaped('weather_controller', ['s s', 'sls', 'pcp'], {'s': STEEL, 'l': {'item': 'minecraft:daylight_detector'}, 'c': item('circuit_board'),
                                                     'p': item('base_panel')})
w(D + '/recipe/silver_iodide.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
                                     'ingredients': [item('salt'), tag('c:ingots/silver')], 'result': {'id': 'rotarycraft:silver_iodide', 'count': 1}})
w(R + '/data/c/tags/item/dusts/wood.json', {'values': ['rotarycraft:sawdust']})

# ---- Borer and the mining pipe it leaves behind ----
from PIL import Image, ImageDraw

BLOCK_TEX = T + '/block'
os.makedirs(BLOCK_TEX, exist_ok=True)
side = Image.open(BLOCK_TEX + '/machine_side.png').convert('RGBA')


def plain_machine(name, front, loot_name=None):
    """A full-block machine: its front (the face it works through) and the shared sides."""
    front.save('%s/%s_front.png' % (BLOCK_TEX, name))
    MACHINES.append(name)
    w('%s/models/block/%s.json' % (A, name), {'parent': 'minecraft:block/cube', 'textures': {
        'north': 'rotarycraft:block/%s_front' % name, 'south': 'rotarycraft:block/machine_back', 'east': 'rotarycraft:block/machine_side',
        'west': 'rotarycraft:block/machine_side', 'up': 'rotarycraft:block/machine_side', 'down': 'rotarycraft:block/machine_side',
        'particle': 'rotarycraft:block/machine_side'}})
    rot = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
    w('%s/blockstates/%s.json' % (A, name), {'variants': {'facing=' + f: dict({'model': 'rotarycraft:block/' + name}, **r) for f, r in rot.items()}})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'rotarycraft:block/' + name})
    w('%s/loot_table/blocks/%s.json' % (D, name), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def borer_front():
    im = side.copy()
    g = ImageDraw.Draw(im)
    g.ellipse([2, 2, 13, 13], fill=(30, 30, 34, 255), outline=(150, 154, 162, 255))
    g.ellipse([4, 4, 11, 11], fill=(70, 72, 78, 255))
    for a, b in (((7, 3), (9, 12)), ((5, 7), (12, 8)), ((4, 5), (11, 10))):
        g.line([a, b], fill=(190, 194, 200, 255))
    g.ellipse([7, 7, 8, 8], fill=(235, 235, 240, 255))
    return im


plain_machine('borer', borer_front())

pipe = Image.new('RGBA', (16, 16), (96, 100, 108, 255))
g = ImageDraw.Draw(pipe)
for x in (0, 7, 15):
    g.line([(x, 0), (x, 15)], fill=(60, 62, 68, 255))
for y in (3, 12):
    g.line([(0, y), (15, y)], fill=(130, 134, 142, 255))
g.rectangle([0, 0, 15, 15], outline=(52, 54, 60, 255))
pipe.save(BLOCK_TEX + '/mining_pipe.png')
for kind, box in (('x', [0, 5.28, 5.28, 16, 10.72, 10.72]), ('z', [5.28, 5.28, 0, 10.72, 10.72, 16])):
    w('%s/models/block/mining_pipe_%s.json' % (A, kind), {'textures': {'pipe': 'rotarycraft:block/mining_pipe', 'particle': 'rotarycraft:block/mining_pipe'},
        'elements': [{'from': box[:3], 'to': box[3:], 'faces': {f: {'texture': '#pipe', 'uv': [0, 5, 16, 11] if f in ('up', 'down', 'north', 'south', 'east', 'west') else [0, 0, 16, 16]}
                                                         for f in ('down', 'up', 'north', 'south', 'west', 'east')}}]})
w(A + '/models/block/mining_pipe_collar.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'rotarycraft:block/mining_pipe'}})
w(A + '/blockstates/mining_pipe.json', {'variants': {'kind=x': {'model': 'rotarycraft:block/mining_pipe_x'}, 'kind=z': {'model': 'rotarycraft:block/mining_pipe_z'},
                                                      'kind=collar': {'model': 'rotarycraft:block/mining_pipe_collar'}}})
shutil.copy(REF + '/Textures/GUI/borergui.png', T + '/gui/borer.png')
shutil.copy(REF + '/Textures/GUI/buttons.png', T + '/gui/borer_buttons.png')
shaped('borer', ['SSS', 'DGC', 'BBB'], {'S': STEEL, 'D': item('drill'), 'G': item('steel_gear_unit_2'), 'C': item('circuit_board'), 'B': item('base_panel')})

# ---- Bedrock Breaker and the bedrock slice it grinds ----
model_texture('bedrocktex.png', 'bedrock_breaker')
model_texture('bedrockvtex.png', 'bedrock_breaker_v')
rendered_machine('bedrock_breaker')
MODELS.append('ModelBedrockBreaker:bedrock_breaker')
MODELS.append('ModelBedrockBreakerV:bedrock_breaker_v')
shaped('bedrock_breaker', ['BDt', 'BSO', 'BDt'], {'t': item('tungsten_ingot'), 'S': STEEL, 'D': {'tag': 'c:gems/diamond'}, 'O': {'item': 'minecraft:obsidian'},
                                                  'B': item('base_panel')})
SLICE_DIRS = {'down': (1, 0), 'up': (1, 1), 'north': (2, 0), 'south': (2, 1), 'west': (0, 0), 'east': (0, 1)}
slice_variants = {}
for dname, (axis, positive) in SLICE_DIRS.items():
    for n in range(16):
        lo, hi = [0, 0, 0], [16, 16, 16]
        if positive:
            hi[axis] = 16 - n
        else:
            lo[axis] = n
        w('%s/models/block/bedrock_slice/%s_%d.json' % (A, dname, n), {'textures': {'all': 'minecraft:block/bedrock', 'particle': 'minecraft:block/bedrock'},
            'elements': [{'from': lo, 'to': hi, 'faces': {f: {'texture': '#all'} for f in ('down', 'up', 'north', 'south', 'west', 'east')}}]})
        slice_variants['facing=%s,progress=%d' % (dname, n)] = {'model': 'rotarycraft:block/bedrock_slice/%s_%d' % (dname, n)}
w(A + '/blockstates/bedrock_slice.json', {'variants': slice_variants})

# ---- Sonic Borer ----
model_texture('sonicborertex.png', 'sonic_borer')
rendered_machine('sonic_borer')
MODELS.append('ModelSonicBorer:sonic_borer')
shaped('sonic_borer', ['ss ', 'Icp', 'bbb'], {'s': STEEL, 'I': {'item': 'minecraft:iron_bars'}, 'c': item('compressor'), 'p': item('pipe'), 'b': item('base_panel')})

# ---- Terraformer ----
front = side.copy()
g = ImageDraw.Draw(front)
g.ellipse([2, 2, 13, 13], fill=(40, 90, 170, 255), outline=(150, 154, 162, 255))
g.polygon([(5, 5), (8, 4), (10, 6), (9, 9), (6, 10), (4, 8)], fill=(70, 150, 70, 255))
g.polygon([(9, 8), (12, 8), (11, 11), (8, 12)], fill=(190, 170, 100, 255))
g.arc([2, 2, 13, 13], 200, 340, fill=(220, 230, 255, 255))
plain_machine('terraformer', front)
shutil.copy(REF + '/Textures/GUI/terraformergui.png', T + '/gui/terraformer.png')
shutil.copy(REF + '/Textures/GUI/biomes.png', T + '/gui/biomes.png')
shaped('terraformer', ['SsS', 'ici', 'PiP'], {'S': STEEL, 's': item('screen'), 'i': item('impeller'), 'c': item('circuit_board'), 'P': item('base_panel')})

# ---- Solar Tower and Solar Mirror ----
model_texture('solartex.png', 'solar_tower')
model_texture('mirrortex.png', 'solar_mirror')
rendered_machine('solar_tower')
rendered_machine('solar_mirror')
MODELS.append('ModelSolar:solar_tower')
MODELS.append('ModelMirror:solar_mirror')
shaped('solar_tower', ['pPp', 'iPi', 'pPp'], {'p': item('base_panel'), 'P': item('pipe'), 'i': {'tag': 'c:dyes/black'}})
shaped('solar_mirror', ['bmb', ' g ', 'pcp'], {'b': item('blast_glass'), 'm': item('mirror'), 'g': item('steel_gear'), 'p': item('base_panel'), 'c': item('circuit_board')})
w(R + '/data/rotarycraft/tags/fluid/solar_sodium.json', {'replace': False, 'values': [
    {'id': 'reactorcraft:sodium', 'required': False}, {'id': 'mekanism:sodium', 'required': False}]})

lang.update({
    'block.rotarycraft.solar_tower': 'Solar Tower',
    'block.rotarycraft.solar_mirror': 'Solar Mirror',
    'block.rotarycraft.terraformer': 'Terraformer',
    'gui.rotarycraft.terraformer.radius': 'Area: %s',
    'gui.rotarycraft.terraformer.water': 'Water: %s mB',
    'gui.rotarycraft.terraformer.needs_signal': 'Needs a redstone signal',
    'gui.rotarycraft.terraformer.power': 'Needs %s',
    'gui.rotarycraft.terraformer.water_cost': 'Uses %s mB of water',
    'gui.rotarycraft.terraformer.item': 'Needs %s (used %s%% of the time)',
    'block.rotarycraft.sonic_borer': 'Sonic Borer',
    'block.rotarycraft.bedrock_breaker': 'Bedrock Breaker',
    'block.rotarycraft.bedrock_slice': 'Bedrock',
    'block.rotarycraft.borer': 'Borer',
    'block.rotarycraft.mining_pipe': 'Mining Pipe',
    'message.rotarycraft.borer.protected': 'Your borer has hit a protected area at %s, %s and has jammed.',
    'gui.rotarycraft.borer.reset': "Reset Pos'n",
    'gui.rotarycraft.borer.toggle_all': 'Toggle All',
    'gui.rotarycraft.borer.drops_on': 'Drops On',
    'gui.rotarycraft.borer.drops_off': 'Drops Off',
    'gui.rotarycraft.borer.jammed': 'Jammed',
    'gui.rotarycraft.borer.worn': 'Drill worn out',
    'gui.rotarycraft.borer.blocked': 'Blocked: this slice cannot be cut',
    'gui.rotarycraft.borer.required': 'Needs %s and %s N*m',
    'gui.rotarycraft.borer.supplied': 'Supplied: %s at %s N*m',
    'gui.rotarycraft.borer.slice': 'Slice %s',
    'block.rotarycraft.chunk_loader': 'Chunk Loader',
    'block.rotarycraft.weather_controller': 'Weather Controller',
    'item.rotarycraft.silver_iodide': 'Silver Iodide',
})
w(lang_path, lang)

path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += ['rotarycraft:' + m for m in MACHINES if 'rotarycraft:' + m not in data['values']]
w(path, data)

subprocess.run([sys.executable, 'tools/modelbase2json.py'] + MODELS, check=True)
