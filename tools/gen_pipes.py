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


add_tag(R + '/data/minecraft/tags/block/mineable/pickaxe.json', ['rotarycraft:pipe', 'rotarycraft:fuel_line', 'rotarycraft:bedrock_pipe'])
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

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
for name, (_, title) in PIPES.items():
    lang['block.rotarycraft.' + name] = title
lang['message.rotarycraft.meter.pipe'] = '%s: %s mB (%s kPa)'
lang['message.rotarycraft.meter.pipe_empty'] = 'Empty pipe'
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('pipes ok')
