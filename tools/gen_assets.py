"""Generates blockstates, models, placeholder textures, loot tables, tags, recipes and lang for the RotaryCraft port.
Run from the repository root: python tools/gen_assets.py
"""
import json
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


ROT = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
MATS = {'wood': (150, 108, 60), 'stone': (128, 128, 128), 'steel': (170, 180, 192), 'diamond': (90, 220, 215), 'bedrock': (60, 60, 60)}
MACHINES = ['dc_engine', 'generator', 'electric_motor', 'dynamometer'] + ['gearbox_%dx' % r for r in (2, 4, 8, 16)]
BLOCKS = ['shaft_' + m for m in MATS] + MACHINES

# blockstates ---------------------------------------------------------------------------------------------------------
for b in BLOCKS:
    variants = {}
    for facing, r in ROT.items():
        v = {'model': 'rotarycraft:block/' + b}
        v.update(r)
        variants['facing=' + facing] = v
    w('%s/blockstates/%s.json' % (A, b), {'variants': variants})

# models --------------------------------------------------------------------------------------------------------------
for m in MATS:
    tex = 'rotarycraft:block/shaft_' + m
    faces = {
        'north': {'texture': '#rod', 'uv': [6, 6, 10, 10]},
        'south': {'texture': '#rod', 'uv': [6, 6, 10, 10]},
        'east': {'texture': '#rod', 'uv': [0, 6, 16, 10]},
        'west': {'texture': '#rod', 'uv': [0, 6, 16, 10]},
        'up': {'texture': '#rod', 'uv': [6, 0, 10, 16], 'rotation': 90},
        'down': {'texture': '#rod', 'uv': [6, 0, 10, 16], 'rotation': 90},
    }
    w('%s/models/block/shaft_%s.json' % (A, m), {
        'parent': 'minecraft:block/block',
        'textures': {'rod': tex, 'particle': tex},
        'elements': [{'from': [6, 6, 0], 'to': [10, 10, 16], 'faces': faces}],
    })
for b in MACHINES:
    front = 'rotarycraft:block/gearbox_front' if b.startswith('gearbox') else 'rotarycraft:block/%s_front' % b
    side = 'rotarycraft:block/machine_side'
    w('%s/models/block/%s.json' % (A, b), {'parent': 'minecraft:block/cube', 'textures': {
        'north': front, 'south': 'rotarycraft:block/machine_back', 'east': side, 'west': side, 'up': side, 'down': side, 'particle': side}})
for b in BLOCKS:
    w('%s/models/item/%s.json' % (A, b), {'parent': 'rotarycraft:block/' + b})
for it in ['screwdriver', 'angular_transducer', 'hsla_steel_ingot']:
    w('%s/models/item/%s.json' % (A, it), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + it}})

# textures (placeholders until the original machine renderers are ported) -------------------------------------------
T = A + '/textures'
os.makedirs(T + '/block', exist_ok=True)
os.makedirs(T + '/item', exist_ok=True)
rnd = random.Random(7)


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c) + (255,)


for m, c in MATS.items():
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            d = rnd.randint(-12, 12)
            if m == 'wood' and (y + x // 5) % 4 == 0:
                d -= 18
            if m in ('steel', 'diamond'):
                d += 25 if y in (6, 7) else (-25 if y in (9, 10) else 0)
            p[x, y] = shade(c, d)
    im.save('%s/block/shaft_%s.png' % (T, m))


def panel(base=(72, 76, 84)):
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            p[x, y] = shade(base, rnd.randint(-6, 6))
    g = ImageDraw.Draw(im)
    g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
    for x, y in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        p[x, y] = (160, 165, 175, 255)
    return im, g


im, g = panel()
im.save(T + '/block/machine_side.png')
im, g = panel()
g.ellipse([4, 4, 11, 11], fill=(40, 42, 48, 255), outline=(150, 150, 160, 255))
g.rectangle([7, 7, 8, 8], fill=(170, 180, 192, 255))
im.save(T + '/block/machine_back.png')

im, g = panel()
g.ellipse([3, 3, 12, 12], fill=(184, 115, 51, 255), outline=(110, 60, 25, 255))
g.ellipse([6, 6, 9, 9], fill=(170, 180, 192, 255))
g.rectangle([1, 14, 4, 15], fill=(200, 30, 30, 255))
im.save(T + '/block/dc_engine_front.png')

im, g = panel()
for dx, dy in [(0, 0), (0, 12), (-6, 6), (6, 6), (-4, 2), (4, 2), (-4, 10), (4, 10)]:
    g.rectangle([7 + dx, 1 + dy, 8 + dx, 2 + dy], fill=(150, 150, 160, 255))
g.ellipse([3, 3, 12, 12], fill=(140, 140, 150, 255), outline=(90, 90, 100, 255))
g.ellipse([6, 6, 9, 9], fill=(60, 60, 66, 255))
im.save(T + '/block/gearbox_front.png')

im, g = panel()
g.rectangle([3, 3, 12, 12], fill=(184, 115, 51, 255), outline=(110, 60, 25, 255))
g.line([(9, 4), (6, 8), (9, 8), (6, 12)], fill=(255, 230, 60, 255), width=1)
im.save(T + '/block/generator_front.png')

im, g = panel()
g.ellipse([3, 3, 12, 12], fill=(60, 90, 170, 255), outline=(30, 45, 90, 255))
g.ellipse([6, 6, 9, 9], fill=(170, 180, 192, 255))
g.rectangle([12, 1, 14, 2], fill=(255, 230, 60, 255))
im.save(T + '/block/electric_motor_front.png')

im, g = panel()
g.ellipse([2, 2, 13, 13], fill=(230, 230, 220, 255), outline=(40, 42, 48, 255))
g.line([(8, 8), (11, 4)], fill=(200, 30, 30, 255), width=1)
for x, y in [(4, 8), (5, 5), (8, 4), (11, 5), (12, 8)]:
    g.point((x, y), fill=(40, 42, 48, 255))
im.save(T + '/block/dynamometer_front.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.line([(3, 12), (11, 4)], fill=(190, 195, 205, 255), width=1)
g.line([(2, 13), (5, 10)], fill=(200, 40, 40, 255), width=2)
im.save(T + '/item/screwdriver.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.rounded_rectangle([3, 1, 12, 14], radius=2, fill=(70, 74, 82, 255), outline=(30, 32, 36, 255))
g.rectangle([5, 3, 10, 8], fill=(30, 60, 40, 255))
g.line([(6, 7), (8, 5), (9, 6)], fill=(90, 255, 120, 255))
g.ellipse([6, 10, 9, 13], fill=(184, 115, 51, 255))
im.save(T + '/item/angular_transducer.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.polygon([(2, 10), (6, 6), (14, 6), (10, 10)], fill=(150, 160, 175, 255))
g.polygon([(2, 10), (10, 10), (10, 12), (2, 12)], fill=(110, 118, 130, 255))
g.polygon([(10, 10), (14, 6), (14, 8), (10, 12)], fill=(90, 96, 108, 255))
g.line([(6, 7), (12, 7)], fill=(210, 215, 225, 255))
im.save(T + '/item/hsla_steel_ingot.png')

# loot tables and tags ------------------------------------------------------------------------------------------------
for b in BLOCKS:
    w('%s/loot_table/blocks/%s.json' % (D, b), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + b}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
w(R + '/data/minecraft/tags/block/mineable/pickaxe.json', {'values': ['rotarycraft:' + b for b in BLOCKS if b != 'shaft_wood']})
w(R + '/data/minecraft/tags/block/mineable/axe.json', {'values': ['rotarycraft:shaft_wood']})
w(R + '/data/minecraft/tags/block/needs_diamond_tool.json', {'values': ['rotarycraft:shaft_bedrock']})
w(R + '/data/c/tags/item/ingots/steel.json', {'values': ['rotarycraft:hsla_steel_ingot']})
w(R + '/data/c/tags/item/ingots.json', {'values': ['rotarycraft:hsla_steel_ingot']})

# recipes -------------------------------------------------------------------------------------------------------------
STEEL = {'tag': 'c:ingots/steel'}
IRON = {'tag': 'c:ingots/iron'}
COPPER = {'tag': 'c:ingots/copper'}
STEEL_SHAFT = {'item': 'rotarycraft:shaft_steel'}


def shaped(name, pattern, key, count=1):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern,
                                       'key': key, 'result': {'id': 'rotarycraft:' + name, 'count': count}})


shaped('shaft_wood', ['X', 'S', 'X'], {'X': {'tag': 'minecraft:planks'}, 'S': {'item': 'minecraft:stick'}}, 2)
shaped('shaft_stone', ['X', 'S', 'X'], {'X': {'item': 'minecraft:stone'}, 'S': {'item': 'minecraft:stick'}}, 2)
shaped('shaft_steel', ['X', 'S', 'X'], {'X': STEEL, 'S': {'item': 'minecraft:stick'}}, 2)
shaped('shaft_diamond', ['X', 'S', 'X'], {'X': {'tag': 'c:gems/diamond'}, 'S': STEEL_SHAFT}, 2)
shaped('dc_engine', ['IRI', 'ICI', 'ISI'], {'I': IRON, 'R': {'item': 'minecraft:redstone'}, 'C': COPPER, 'S': STEEL_SHAFT})
shaped('gearbox_2x', ['ISI', 'S S', 'ISI'], {'I': STEEL, 'S': STEEL_SHAFT})
for a, b in [(2, 4), (4, 8), (8, 16)]:
    w('%s/recipe/gearbox_%dx.json' % (D, b), {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'item': 'rotarycraft:gearbox_%dx' % a}, {'item': 'rotarycraft:gearbox_%dx' % a}],
        'result': {'id': 'rotarycraft:gearbox_%dx' % b, 'count': 1}})
shaped('dynamometer', ['I I', 'SCS', 'III'], {'I': STEEL, 'S': STEEL_SHAFT, 'C': {'item': 'minecraft:clock'}})
shaped('generator', ['ICI', 'SRS', 'ICI'], {'I': STEEL, 'C': COPPER, 'S': STEEL_SHAFT, 'R': {'item': 'minecraft:redstone_block'}})
shaped('electric_motor', ['ICI', 'RSR', 'ICI'], {'I': STEEL, 'C': COPPER, 'S': STEEL_SHAFT, 'R': {'item': 'minecraft:redstone'}})
shaped('screwdriver', ['  I', ' I ', 'S  '], {'I': IRON, 'S': {'item': 'minecraft:stick'}})
shaped('angular_transducer', [' R ', 'ICI', 'III'], {'I': IRON, 'R': {'item': 'minecraft:redstone'}, 'C': {'item': 'minecraft:compass'}})
w(D + '/recipe/hsla_steel_ingot_blasting.json', {'type': 'minecraft:blasting', 'category': 'misc', 'ingredient': IRON,
    'result': {'id': 'rotarycraft:hsla_steel_ingot', 'count': 1}, 'experience': 0.4, 'cookingtime': 400})

# lang ----------------------------------------------------------------------------------------------------------------
lang = {
    'itemGroup.rotarycraft': 'RotaryCraft',
    'block.rotarycraft.dc_engine': 'DC Electric Engine',
    'block.rotarycraft.generator': 'Generator',
    'block.rotarycraft.electric_motor': 'Electric Motor',
    'block.rotarycraft.dynamometer': 'Dynamometer',
    'item.rotarycraft.screwdriver': 'Screwdriver',
    'item.rotarycraft.angular_transducer': 'Angular Transducer',
    'item.rotarycraft.hsla_steel_ingot': 'HSLA Steel Ingot',
    'message.rotarycraft.meter.reading': 'Torque: %s N*m, Speed: %s rad/s, Power: %s',
    'message.rotarycraft.meter.shaft_limits': 'Shaft limits: %s N*m, %s rad/s',
    'message.rotarycraft.meter.generator': 'Making %s FE/t, stored %s / %s FE',
    'message.rotarycraft.meter.motor': 'Uses %s FE/t while running, stored %s / %s FE',
    'message.rotarycraft.gearbox.reduction': 'Reduction: speed /%1$s, torque x%1$s',
    'message.rotarycraft.gearbox.acceleration': 'Acceleration: speed x%1$s, torque /%1$s',
}
for m in MATS:
    lang['block.rotarycraft.shaft_' + m] = m.capitalize() + ' Shaft'
for r in (2, 4, 8, 16):
    lang['block.rotarycraft.gearbox_%dx' % r] = '%d:1 Gearbox' % r
w(A + '/lang/en_us.json', lang)
print('assets ok,', len(BLOCKS), 'blocks')
