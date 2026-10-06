"""Pipes (hose, pipe, fuel line, bedrock pipe): multipart models (a core plus an arm per connected side), textures, loot,
tags, recipes and names (run by gen_assets.py before gen_parts.py)."""
import json
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures/block'
rnd = random.Random(89)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def shade(c, d=0):
    return tuple(max(0, min(255, x + d)) for x in c[:3]) + (255,)


# frame colour, the material it's made of, its name
PIPES = {
    'hose': ((150, 108, 60), 'Lubricant Hose'),
    'pipe': ((170, 180, 192), 'Fluid Pipe'),
    'fuel_line': ((30, 22, 40), 'Fuel Line'),
    'bedrock_pipe': ((60, 60, 60), 'Bedrock Pipe'),
    # fittings, framed like the blocks they're made from
    'valve': ((175, 30, 20), 'Valve'),
    'separator': ((35, 70, 160), 'Separator'),
    'bypass': ((215, 200, 150), 'Bypass'),
    'suction_pipe': ((70, 35, 40), 'Suction Pipe'),
}
ROT = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}

for name, (c, title) in PIPES.items():
    # frame round a glass window
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            edge = x < 2 or x > 13 or y < 2 or y > 13
            p[x, y] = shade(c, rnd.randint(-10, 10)) if edge else (200, 225, 235, 255)
    g = ImageDraw.Draw(im)
    g.line([(4, 11), (10, 5)], fill=(240, 250, 255, 255))
    im.save('%s/%s.png' % (T, name))
    tex = 'rotarycraft:block/' + name
    w('%s/models/block/%s_core.json' % (A, name), {'parent': 'minecraft:block/block', 'textures': {'all': tex, 'particle': tex},
        'elements': [{'from': [4, 4, 4], 'to': [12, 12, 12], 'faces': {f: {'texture': '#all', 'uv': [4, 4, 12, 12]}
                                                                         for f in ('north', 'south', 'east', 'west', 'up', 'down')}}]})
    w('%s/models/block/%s_arm.json' % (A, name), {'parent': 'minecraft:block/block', 'textures': {'all': tex, 'particle': tex},
        'elements': [{'from': [4, 4, 0], 'to': [12, 12, 4], 'faces': {
            'east': {'texture': '#all', 'uv': [0, 4, 4, 12]}, 'west': {'texture': '#all', 'uv': [0, 4, 4, 12]},
            'up': {'texture': '#all', 'uv': [4, 0, 12, 4]}, 'down': {'texture': '#all', 'uv': [4, 0, 12, 4]},
            'north': {'texture': '#all', 'uv': [4, 4, 12, 12]}}}]})
    parts = [{'apply': {'model': 'rotarycraft:block/%s_core' % name}}]
    for side, rot in ROT.items():
        a = {'model': 'rotarycraft:block/%s_arm' % name, 'uvlock': False}
        a.update(rot)
        parts.append({'when': {side: 'true'}, 'apply': a})
    w('%s/blockstates/%s.json' % (A, name), {'multipart': parts})
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:block/block', 'textures': {'all': tex, 'particle': tex},
        'elements': [{'from': [4, 4, 0], 'to': [12, 12, 16], 'faces': {f: {'texture': '#all'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}],
        'display': {'gui': {'rotation': [30, 45, 0], 'scale': [0.8, 0.8, 0.8]}}})
    w('%s/loot_table/blocks/%s.json' % (D, name), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + name}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def add_tag(path, values):
    data = json.load(open(path)) if os.path.exists(path) else {'values': []}
    data['values'] = data['values'] + [v for v in values if v not in data['values']]
    w(path, data)


add_tag(R + '/data/minecraft/tags/block/mineable/pickaxe.json', ['rotarycraft:pipe', 'rotarycraft:fuel_line', 'rotarycraft:bedrock_pipe',
                                                                 'rotarycraft:valve', 'rotarycraft:separator', 'rotarycraft:bypass', 'rotarycraft:suction_pipe'])
add_tag(R + '/data/minecraft/tags/block/mineable/axe.json', ['rotarycraft:hose'])
add_tag(R + '/data/minecraft/tags/block/needs_diamond_tool.json', ['rotarycraft:bedrock_pipe'])
# fluids that only fuel lines (and bedrock pipes) carry
w(D + '/tags/fluid/pipe_fuels.json', {'values': ['#c:ethanol', '#c:jet_fuel', {'id': '#c:biofuel', 'required': False},
                                                {'id': '#c:fuels', 'required': False}, {'id': '#c:diesel', 'required': False}]})


def shaped(name, pattern, key, count):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
      'result': {'id': 'rotarycraft:' + name, 'count': count}})


# the original's straight pipe recipes, 16 a craft at medium difficulty
GLASS = {'tag': 'c:glass_blocks'}
shaped('hose', ['WGW', 'WGW', 'WGW'], {'W': {'tag': 'minecraft:planks'}, 'G': GLASS}, 16)
shaped('pipe', ['SGS', 'SGS', 'SGS'], {'S': {'tag': 'c:ingots/steel'}, 'G': GLASS}, 16)
shaped('fuel_line', ['OGO', 'OGO', 'OGO'], {'O': {'tag': 'c:obsidians'}, 'G': GLASS}, 16)
shaped('bedrock_pipe', ['BGB', 'BGB', 'BGB'], {'B': {'item': 'rotarycraft:bedrock_ingot'}, 'G': {'item': 'minecraft:tinted_glass'}}, 16)  # tinted glass until Blast Glass
STEEL_ING = {'tag': 'c:ingots/steel'}
shaped('valve', ['sGs', 'OGO', 'sGs'], {'s': STEEL_ING, 'G': GLASS, 'O': {'item': 'minecraft:redstone_block'}}, 4)
shaped('separator', ['sGs', 'OGO', 'sGs'], {'s': STEEL_ING, 'G': GLASS, 'O': {'item': 'minecraft:lapis_block'}}, 4)
shaped('bypass', ['OGO', 'OGO', 'OGO'], {'O': {'item': 'minecraft:sandstone'}, 'G': GLASS}, 4)
shaped('suction_pipe', ['SGS', 'SGS', 'SGS'], {'S': {'item': 'minecraft:nether_bricks'}, 'G': GLASS}, 4)

# Reservoir: an open steel tank (covered: with a glass lid); Pump: an impeller housing
steel = (170, 180, 192)
im = Image.new('RGBA', (16, 16))
px = im.load()
for x in range(16):
    for y in range(16):
        px[x, y] = shade(steel, rnd.randint(-10, 10))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=shade(steel, -60))
im.save(T + '/reservoir.png')
im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], fill=(200, 225, 235, 120), outline=(120, 130, 140, 255))
g.line([(3, 12), (12, 3)], fill=(240, 250, 255, 200))
im.save(T + '/reservoir_lid.png')
tex = 'rotarycraft:block/reservoir'
walls = [([0, 0, 0], [16, 1, 16]), ([0, 1, 0], [16, 16, 1]), ([0, 1, 15], [16, 16, 16]), ([0, 1, 1], [1, 16, 15]), ([15, 1, 1], [16, 16, 15])]
elements = [{'from': f, 'to': t, 'faces': {d: {'texture': '#all'} for d in ('north', 'south', 'east', 'west', 'up', 'down')}} for f, t in walls]
w('%s/models/block/reservoir.json' % A, {'parent': 'minecraft:block/block', 'textures': {'all': tex, 'particle': tex}, 'elements': elements})
lid = elements + [{'from': [1, 15, 1], 'to': [15, 16, 15], 'faces': {'up': {'texture': '#lid'}, 'down': {'texture': '#lid'}}}]
w('%s/models/block/reservoir_covered.json' % A, {'parent': 'minecraft:block/block', 'render_type': 'minecraft:translucent',
    'textures': {'all': tex, 'lid': 'rotarycraft:block/reservoir_lid', 'particle': tex}, 'elements': lid})
w('%s/blockstates/reservoir.json' % A, {'variants': {'covered=false': {'model': 'rotarycraft:block/reservoir'},
                                                     'covered=true': {'model': 'rotarycraft:block/reservoir_covered'}}})
w('%s/models/item/reservoir.json' % A, {'parent': 'rotarycraft:block/reservoir'})
w('%s/loot_table/blocks/reservoir.json' % D, {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:reservoir', 'functions': [
        {'function': 'minecraft:copy_state', 'block': 'rotarycraft:reservoir', 'properties': ['covered']}]}],
    'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
im = Image.new('RGBA', (16, 16))
px = im.load()
for x in range(16):
    for y in range(16):
        px[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.ellipse([3, 3, 12, 12], fill=(60, 90, 150, 255), outline=(170, 180, 192, 255))
g.line([(5, 5), (10, 10)], fill=(170, 180, 192, 255))
g.line([(10, 5), (5, 10)], fill=(170, 180, 192, 255))
im.save(T + '/pump_front.png')
add_tag(R + '/data/minecraft/tags/block/mineable/pickaxe.json', ['rotarycraft:reservoir'])
STEEL = {'tag': 'c:ingots/steel'}
PANEL = {'item': 'rotarycraft:base_panel'}
shaped('reservoir', ['B B', 'B B', 'BBB'], {'B': PANEL}, 1)
w('%s/recipe/reservoir_covered.json' % D, {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['BPB', 'B B', 'BBB'],
  'key': {'B': PANEL, 'P': {'tag': 'c:glass_panes'}},
  'result': {'id': 'rotarycraft:reservoir', 'count': 1, 'components': {'minecraft:block_state': {'covered': 'true'}}}})
shaped('pump', ['SGS', 'pIp', 'PpP'], {'S': STEEL, 'G': {'tag': 'c:glass_panes'}, 'p': {'item': 'rotarycraft:pipe'},
       'I': {'item': 'rotarycraft:impeller'}, 'P': PANEL}, 1)

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
for name, (_, title) in PIPES.items():
    lang['block.rotarycraft.' + name] = title
lang['message.rotarycraft.meter.pipe'] = '%s: %s mB (%s kPa)'
lang['message.rotarycraft.meter.pipe_empty'] = 'Empty pipe'
lang['block.rotarycraft.reservoir'] = 'Reservoir'
lang['block.rotarycraft.pump'] = 'Pump'
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('pipes ok')
