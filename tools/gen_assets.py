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
FLYWHEELS = {'wood': (150, 108, 60), 'stone': (128, 128, 128), 'iron': (200, 200, 205), 'gold': (230, 190, 50), 'bedrock': (60, 60, 60)}
MACHINES = ['dc_engine', 'wind_engine', 'steam_engine', 'generator', 'electric_motor', 'dynamometer', 'clutch', 'grinder', 'extractor', 'blast_furnace', 'friction_heater', 'fermenter', 'centrifuge', 'rock_melter', 'gas_engine', 'performance_engine', 'ac_engine', 'magnetizer', 'fractionator', 'microturbine'] + ['gearbox_%dx' % r for r in (2, 4, 8, 16)] + ['flywheel_' + f for f in FLYWHEELS]
BLOCKS = ['shaft_' + m for m in MATS] + MACHINES + ['bevel_gear', 'splitter']

# blockstates ---------------------------------------------------------------------------------------------------------
for b in BLOCKS:
    if b in ('bevel_gear', 'splitter'):
        # input and output can be any two sides, so the gear casing looks the same from every side
        w('%s/blockstates/%s.json' % (A, b), {'variants': {'': {'model': 'rotarycraft:block/' + b}}})
        continue
    variants = {}
    for facing, r in ROT.items():
        v = {'model': 'rotarycraft:block/' + b}
        v.update(r)
        variants['facing=' + facing] = v
    w('%s/blockstates/%s.json' % (A, b), {'variants': variants})
w('%s/models/block/bevel_gear.json' % A, {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'rotarycraft:block/bevel_gear'}})
w('%s/models/block/splitter.json' % A, {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'rotarycraft:block/splitter'}})

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
SIMPLE_ITEMS = ['screwdriver', 'angular_transducer', 'hsla_steel_ingot', 'sawdust', 'flour', 'netherrack_dust', 'tar']
for it in SIMPLE_ITEMS:
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

im, g = panel()
for a in range(0, 360, 90):
    import math
    x = 8 + 6 * math.cos(math.radians(a + 20))
    y = 8 + 6 * math.sin(math.radians(a + 20))
    g.line([(8, 8), (x, y)], fill=(220, 220, 225, 255), width=2)
g.ellipse([6, 6, 9, 9], fill=(90, 90, 100, 255))
im.save(T + '/block/wind_engine_front.png')

im, g = panel()
g.rectangle([2, 4, 13, 11], fill=(130, 70, 40, 255), outline=(80, 40, 20, 255))
g.ellipse([9, 1, 14, 6], fill=(230, 230, 220, 255), outline=(40, 42, 48, 255))
g.line([(11, 4), (13, 2)], fill=(200, 30, 30, 255))
g.rectangle([4, 12, 6, 15], fill=(170, 180, 192, 255))
im.save(T + '/block/steam_engine_front.png')

im, g = panel()
g.ellipse([3, 3, 12, 12], fill=(110, 110, 120, 255), outline=(60, 60, 66, 255))
g.rectangle([7, 2, 8, 13], fill=(200, 30, 30, 255))
im.save(T + '/block/clutch_front.png')

im, g = panel((90, 92, 100))
for dx, dy in [(0, 0), (12, 0), (0, 12), (12, 12)]:
    g.rectangle([1 + dx, 1 + dy, 2 + dx, 2 + dy], fill=(150, 150, 160, 255))
g.line([(4, 8), (11, 8)], fill=(170, 180, 192, 255), width=2)
g.line([(8, 4), (8, 11)], fill=(170, 180, 192, 255), width=2)
g.ellipse([6, 6, 9, 9], fill=(60, 60, 66, 255))
im.save(T + '/block/bevel_gear.png')

im, g = panel((90, 92, 100))
g.line([(2, 8), (13, 8)], fill=(170, 180, 192, 255), width=2)
g.line([(8, 8), (8, 2)], fill=(170, 180, 192, 255), width=2)
g.ellipse([6, 6, 9, 9], fill=(60, 60, 66, 255))
im.save(T + '/block/splitter.png')

im, g = panel()
g.rectangle([2, 4, 13, 11], fill=(50, 52, 58, 255), outline=(30, 32, 36, 255))
for x in range(3, 13, 2):
    g.line([(x, 5), (x + 1, 10)], fill=(190, 195, 205, 255))
im.save(T + '/block/grinder_front.png')

im, g = panel()
for i, c in enumerate([(120, 120, 130), (150, 120, 90), (110, 150, 190), (200, 200, 210)]):
    g.rectangle([2 + i * 3, 4, 3 + i * 3, 11], fill=c + (255,))
g.line([(2, 12), (13, 12)], fill=(30, 32, 36, 255))
im.save(T + '/block/extractor_front.png')


def powder(name, c):
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 9.5
            if dx * dx / 36 + dy * dy / 16 <= 1:
                p[x, y] = shade(c, rnd.randint(-25, 25))
    im.save('%s/item/%s.png' % (T, name))


powder('sawdust', (196, 160, 110))
powder('flour', (240, 236, 225))
powder('netherrack_dust', (140, 50, 50))
powder('tar', (30, 26, 24))

for f, c in FLYWHEELS.items():
    im, g = panel()
    g.ellipse([1, 1, 14, 14], fill=shade(c, 0), outline=shade(c, -50))
    g.ellipse([4, 4, 11, 11], outline=shade(c, -30))
    g.ellipse([6, 6, 9, 9], fill=(60, 60, 66, 255))
    im.save('%s/block/flywheel_%s_front.png' % (T, f))

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


for a, b in [(2, 4), (4, 8), (8, 16)]:
    w('%s/recipe/gearbox_%dx.json' % (D, b), {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
        'ingredients': [{'item': 'rotarycraft:gearbox_%dx' % a}, {'item': 'rotarycraft:gearbox_%dx' % a}],
        'result': {'id': 'rotarycraft:gearbox_%dx' % b, 'count': 1}})


def grind(name, ingredient, result, count=1):
    w('%s/recipe/grinding/%s.json' % (D, name), {'type': 'rotarycraft:grinding', 'ingredient': ingredient,
                                                'result': {'id': result, 'count': count}})


def item(i):
    return {'item': i}


def tag(t):
    return {'tag': t}


for name, ing, res, n in [
    ('stone', item('minecraft:stone'), 'minecraft:cobblestone', 1),
    ('cobblestone', item('minecraft:cobblestone'), 'minecraft:gravel', 1),
    ('gravel', item('minecraft:gravel'), 'minecraft:sand', 1),
    ('glass', item('minecraft:glass'), 'minecraft:sand', 1),
    ('sandstone', item('minecraft:sandstone'), 'minecraft:sand', 1),
    ('sandstone_stairs', item('minecraft:sandstone_stairs'), 'minecraft:sand', 6),
    ('glowstone', item('minecraft:glowstone'), 'minecraft:glowstone_dust', 4),
    ('stone_bricks', item('minecraft:stone_bricks'), 'minecraft:cobblestone', 1),
    ('furnace', item('minecraft:furnace'), 'minecraft:cobblestone', 8),
    ('bricks', item('minecraft:bricks'), 'minecraft:clay_ball', 4),
    ('clay', item('minecraft:clay'), 'minecraft:clay_ball', 4),
    ('brick_stairs', item('minecraft:brick_stairs'), 'minecraft:clay_ball', 6),
    ('brick', item('minecraft:brick'), 'minecraft:clay_ball', 1),
    ('stone_stairs', item('minecraft:stone_stairs'), 'minecraft:gravel', 2),
    ('stone_brick_stairs', item('minecraft:stone_brick_stairs'), 'minecraft:cobblestone', 2),
    ('stone_pressure_plate', item('minecraft:stone_pressure_plate'), 'minecraft:cobblestone', 2),
    ('netherrack', item('minecraft:netherrack'), 'rotarycraft:netherrack_dust', 1),
    ('soul_sand', item('minecraft:soul_sand'), 'rotarycraft:tar', 1),
    ('wheat', item('minecraft:wheat'), 'rotarycraft:flour', 3),
    ('sugar_cane', item('minecraft:sugar_cane'), 'minecraft:sugar', 3),
    ('bone', item('minecraft:bone'), 'minecraft:bone_meal', 9),
    ('blaze_rod', item('minecraft:blaze_rod'), 'minecraft:blaze_powder', 6),
    ('ice', item('minecraft:ice'), 'minecraft:snowball', 4),
    ('logs', tag('minecraft:logs'), 'rotarycraft:sawdust', 16),
    ('planks', tag('minecraft:planks'), 'rotarycraft:sawdust', 4),
    ('note_block', item('minecraft:note_block'), 'rotarycraft:sawdust', 32),
    ('jukebox', item('minecraft:jukebox'), 'rotarycraft:sawdust', 32),
    ('wooden_fences', tag('minecraft:wooden_fences'), 'rotarycraft:sawdust', 4),
    ('wooden_stairs', tag('minecraft:wooden_stairs'), 'rotarycraft:sawdust', 6),
    ('chest', item('minecraft:chest'), 'rotarycraft:sawdust', 32),
    ('crafting_table', item('minecraft:crafting_table'), 'rotarycraft:sawdust', 16),
    ('ladder', item('minecraft:ladder'), 'rotarycraft:sawdust', 4),
    ('wooden_pressure_plates', tag('minecraft:wooden_pressure_plates'), 'rotarycraft:sawdust', 8),
    ('bowl', item('minecraft:bowl'), 'rotarycraft:sawdust', 12),
    ('wooden_doors', tag('minecraft:wooden_doors'), 'rotarycraft:sawdust', 24),
    ('signs', tag('minecraft:signs'), 'rotarycraft:sawdust', 24),
    ('stick', item('minecraft:stick'), 'rotarycraft:sawdust', 2),
    ('wooden_trapdoors', tag('minecraft:wooden_trapdoors'), 'rotarycraft:sawdust', 24),
    ('fence_gates', tag('minecraft:fence_gates'), 'rotarycraft:sawdust', 16),
    ('coal', item('minecraft:coal'), 'rotarycraft:coal_dust', 1),
] + [('flower_' + f, item('minecraft:' + f), 'minecraft:%s_dye' % d, 6) for f, d in [
    # the original's flower dyes: six dye per flower
    ('dandelion', 'yellow'), ('poppy', 'red'), ('blue_orchid', 'light_blue'), ('allium', 'magenta'), ('azure_bluet', 'light_gray'),
    ('red_tulip', 'red'), ('orange_tulip', 'orange'), ('white_tulip', 'light_gray'), ('pink_tulip', 'pink'),
    ('oxeye_daisy', 'light_gray'), ('lilac', 'magenta'), ('rose_bush', 'red'), ('peony', 'pink')]]:
    grind(name, ing, res, n)

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
lang.update({
    'block.rotarycraft.wind_engine': 'Wind Engine',
    'block.rotarycraft.grinder': 'Grinder',
    'item.rotarycraft.sawdust': 'Sawdust',
    'item.rotarycraft.flour': 'Flour',
    'item.rotarycraft.netherrack_dust': 'Netherrack Dust',
    'item.rotarycraft.tar': 'Tar',
    'gui.rotarycraft.power': '%s (%s N*m, %s rad/s)',
    'gui.rotarycraft.needs': 'Needs %s N*m and %s',
    'gui.rotarycraft.low_power': '%s / needs %s',
    'block.rotarycraft.steam_engine': 'Steam Engine',
    'block.rotarycraft.clutch': 'Clutch',
    'block.rotarycraft.bevel_gear': 'Bevel Gears',
    'block.rotarycraft.splitter': 'Shaft Junction',
    'message.rotarycraft.splitter.merge': 'Merge: back and branch inputs join into the front',
    'message.rotarycraft.splitter.even': 'Split: torque divided evenly between front and branch',
    'message.rotarycraft.splitter.favor_straight': 'Split: %s/%s of the torque to the front',
    'message.rotarycraft.splitter.favor_bent': 'Split: %s/%s of the torque to the branch',
    'message.rotarycraft.clutch.powered': 'Clutch engages while powered',
    'message.rotarycraft.clutch.unpowered': 'Clutch engages while unpowered',
})
for f in FLYWHEELS:
    lang['block.rotarycraft.flywheel_' + f] = f.capitalize() + ' Flywheel'
for m in MATS:
    lang['block.rotarycraft.shaft_' + m] = m.capitalize() + ' Shaft'
for r in (2, 4, 8, 16):
    lang['block.rotarycraft.gearbox_%dx' % r] = '%d:1 Gearbox' % r
w(A + '/lang/en_us.json', lang)
print('assets ok,', len(BLOCKS), 'blocks')
import runpy
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_extractor.py'))
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_blast.py'))
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_fermenter.py'))
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_centrifuge.py'))
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_melter.py'))
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_engines.py'))
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_fractionator.py'))
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_canola.py'))
runpy.run_path(os.path.join(os.path.dirname(__file__), 'gen_parts.py'))
