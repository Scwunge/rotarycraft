"""Crafting components and the original machine recipes (run last by gen_assets.py, so its machine recipes replace any
stand-ins written earlier). Recipes follow RotaryRecipes.addCraftItems / addMachines / addMultiTypes at medium difficulty
(3 parts per craft, 8 belts, 2 condensers)."""
import json
import math
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures/item'
rnd = random.Random(71)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def shade(c, d=0):
    return tuple(max(0, min(255, x + d)) for x in c[:3]) + (255,)


MATS = {'wood': (150, 108, 60), 'stone': (128, 128, 128), 'steel': (170, 180, 192), 'tungsten': (90, 95, 105),
        'diamond': (90, 220, 215), 'bedrock': (60, 60, 60)}
NAMES = {'wood': 'Wooden', 'stone': 'Stone', 'steel': 'Steel', 'tungsten': 'Tungsten', 'diamond': 'Diamond', 'bedrock': 'Bedrock'}
RATIOS = (2, 4, 8, 16)


# textures -------------------------------------------------------------------------------------------------------------
def canvas():
    im = Image.new('RGBA', (16, 16))
    return im, ImageDraw.Draw(im)


def gear_shape(g, cx, cy, r, c, teeth=8):
    for i in range(teeth):
        a = i * 2 * math.pi / teeth
        x, y = cx + (r + 1) * math.cos(a), cy + (r + 1) * math.sin(a)
        g.rectangle([x - 1, y - 1, x + 1, y + 1], fill=shade(c, -20))
    g.ellipse([cx - r, cy - r, cx + r, cy + r], fill=shade(c), outline=shade(c, -45))
    g.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(30, 30, 30, 255))


def rod(g, c):
    for i in range(11):
        g.rectangle([2 + i, 12 - i, 3 + i, 13 - i], fill=shade(c, rnd.randint(-15, 15)))


def save(im, name):
    im.save('%s/%s.png' % (T, name))
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name}})


for m, c in MATS.items():
    if m != 'wood':
        im, g = canvas()
        rod(g, c)
        save(im, m + '_rod')
    im, g = canvas()
    gear_shape(g, 7.5, 7.5, 5, c)
    save(im, m + '_gear')
    for r in RATIOS:
        im, g = canvas()
        gear_shape(g, 5.5, 6.5, 3.5, c, 6)
        gear_shape(g, 10.5, 9.5, 3.5, c, 6)
        for i in range(int(math.log2(r))):
            g.point((1 + 2 * i, 14), fill=(250, 250, 250, 255))
        save(im, '%s_gear_unit_%d' % (m, r))
    im, g = canvas()
    g.ellipse([2, 2, 13, 13], fill=shade(c), outline=shade(c, -50))
    g.ellipse([5, 5, 10, 10], fill=(0, 0, 0, 0), outline=shade(c, -50))
    for i in range(8):
        a = i * math.pi / 4
        x, y = 7.5 + 4 * math.cos(a), 7.5 + 4 * math.sin(a)
        g.point((round(x), round(y)), fill=(235, 235, 240, 255))
    save(im, m + '_bearing')
    if m not in ('steel', 'tungsten'):  # those two cores are made with the AC engine textures
        im, g = canvas()
        rod(g, c)
        g.ellipse([2, 10, 6, 14], fill=(110, 60, 25, 255))
        g.ellipse([10, 2, 14, 6], fill=(110, 60, 25, 255))
        save(im, m + '_shaft_core')

STEEL = MATS['steel']
GOLD = (230, 190, 50)
COPPER = (184, 115, 51)


def plate(c, d=0):
    im, g = canvas()
    g.rectangle([1, 4, 14, 11], fill=shade(c, d), outline=shade(c, -50))
    return im, g


simple = {}
im, g = plate(STEEL); g.line([(1, 7), (14, 7)], fill=shade(STEEL, -30)); simple['base_panel'] = im
im, g = canvas(); g.rectangle([2, 9, 13, 13], fill=shade(STEEL), outline=shade(STEEL, -50)); g.rectangle([2, 4, 4, 9], fill=shade(STEEL)); g.rectangle([11, 4, 13, 9], fill=shade(STEEL)); simple['mount'] = im
im, g = canvas(); g.ellipse([5, 5, 10, 10], fill=(220, 225, 230, 255), outline=shade(STEEL, -60)); simple['ball_bearing'] = im
im, g = canvas(); gear_shape(g, 7.5, 7.5, 3, STEEL, 6); g.line([(1, 14), (14, 1)], fill=shade(STEEL, -20), width=2); simple['worm_gear'] = im
im, g = canvas()
for i in range(4):
    g.ellipse([1 + 3 * i, 1 + 3 * i, 5 + 3 * i, 5 + 3 * i], outline=shade(STEEL, -30))
simple['chain'] = im
im, g = canvas(); g.rectangle([1, 6, 14, 9], fill=(110, 70, 40, 255), outline=(60, 35, 20, 255)); simple['belt'] = im
im, g = canvas(); gear_shape(g, 7.5, 7.5, 5, STEEL); g.rectangle([1, 6, 3, 9], fill=(150, 40, 40, 255)); g.rectangle([12, 6, 14, 9], fill=(150, 40, 40, 255)); simple['brake'] = im


def blades(c, n, r=6):
    im, g = canvas()
    g.ellipse([7.5 - r, 7.5 - r, 7.5 + r, 7.5 + r], fill=shade(c, -40))
    for i in range(n):
        a = i * 2 * math.pi / n
        g.line([(7.5, 7.5), (7.5 + r * math.cos(a), 7.5 + r * math.sin(a))], fill=shade(c, 30), width=2)
    g.ellipse([6, 6, 9, 9], fill=shade(c, -60))
    return im


simple['impeller'] = blades(STEEL, 4)
simple['compressor'] = blades(STEEL, 8)
simple['turbine'] = blades((200, 200, 210), 12)
simple['compound_turbine'] = blades(MATS['tungsten'], 12)
simple['compound_compressor'] = blades(MATS['tungsten'], 8)
im, g = canvas(); g.polygon([(2, 5), (13, 2), (13, 13), (2, 10)], fill=shade(STEEL), outline=shade(STEEL, -50)); simple['diffuser'] = im
im, g = plate(STEEL); g.rectangle([5, 5, 10, 10], fill=(240, 120, 30, 255)); simple['combustor'] = im
im, g = plate((200, 90, 70)); g.rectangle([5, 5, 10, 10], fill=(255, 200, 60, 255)); simple['high_combustor'] = im
for name, c in (('cylinder', STEEL), ('silumin_cylinder', (205, 210, 220))):
    im, g = canvas(); g.rectangle([4, 1, 11, 14], fill=shade(c), outline=shade(c, -60)); g.line([(4, 4), (11, 4)], fill=shade(c, -40)); simple[name] = im
im, g = plate(STEEL)
for x in range(2, 14, 2):
    g.line([(x, 4), (x, 11)], fill=GOLD + (255,))
simple['radiator'] = im
im, g = plate(STEEL)
for y in (5, 8, 10):
    g.line([(2, y), (13, y)], fill=(120, 160, 220, 255))
simple['condenser'] = im
im, g = canvas(); g.ellipse([2, 2, 13, 13], fill=GOLD + (255,), outline=(150, 110, 20, 255)); g.ellipse([6, 6, 9, 9], fill=shade(STEEL)); simple['gold_coil'] = im
im, g = plate(STEEL); g.line([(4, 3), (4, 0)], fill=GOLD + (255,)); g.line([(11, 3), (11, 0)], fill=GOLD + (255,)); g.point((7, 7), fill=(255, 40, 40, 255)); simple['igniter'] = im
im, g = plate((60, 90, 150)); simple['water_plate'] = im
im, g = canvas(); g.polygon([(7, 1), (9, 1), (9, 14), (7, 14)], fill=shade(STEEL)); g.polygon([(5, 2), (7, 1), (7, 7)], fill=shade(STEEL, -30)); simple['propeller'] = im
im, g = canvas(); gear_shape(g, 7.5, 7.5, 4, STEEL); g.ellipse([5, 5, 10, 10], fill=shade(STEEL, -20)); simple['hub'] = im
im, g = canvas(); g.rectangle([1, 3, 14, 12], fill=(200, 230, 245, 255), outline=(120, 120, 130, 255)); g.line([(3, 10), (9, 4)], fill=(255, 255, 255, 255)); simple['mirror'] = im
im, g = canvas(); rod(g, STEEL); g.ellipse([4, 4, 11, 11], outline=GOLD + (255,), width=2); simple['generator_unit'] = im
im, g = plate(STEEL); g.rectangle([2, 4, 5, 11], fill=GOLD + (255,)); g.rectangle([10, 4, 13, 11], fill=GOLD + (255,)); simple['linear_induction_motor'] = im
im, g = plate((40, 40, 50)); g.point((5, 7), fill=(255, 40, 40, 255)); g.point((10, 7), fill=(60, 220, 120, 255)); simple['power_module'] = im
im, g = plate((30, 110, 50)); g.line([(2, 6), (13, 6)], fill=GOLD + (255,)); g.line([(2, 9), (13, 9)], fill=GOLD + (255,)); simple['circuit_board'] = im
im, g = plate((40, 40, 45)); g.rectangle([3, 5, 12, 10], fill=(60, 200, 120, 255)); simple['screen'] = im
im, g = canvas(); g.polygon([(3, 2), (12, 2), (7, 14)], fill=shade(STEEL), outline=shade(STEEL, -60)); simple['drill'] = im
im, g = canvas(); gear_shape(g, 7.5, 7.5, 5, (200, 200, 205), 14); simple['saw'] = im
im, g = canvas(); g.ellipse([2, 2, 13, 13], outline=shade(STEEL), width=2); g.line([(4, 4), (11, 11)], fill=shade(STEEL)); g.line([(11, 4), (4, 11)], fill=shade(STEEL)); simple['mixer'] = im
im, g = canvas(); g.arc([2, 2, 13, 13], 200, 340, fill=(60, 220, 120, 255), width=2); g.rectangle([6, 9, 9, 14], fill=shade(STEEL)); simple['radar_unit'] = im
im, g = canvas(); g.arc([3, 3, 12, 12], 0, 360, fill=(80, 140, 230, 255)); g.arc([6, 6, 9, 9], 0, 360, fill=(80, 140, 230, 255)); simple['sonar_unit'] = im


def ingot(c):
    im, g = canvas()
    g.polygon([(2, 9), (5, 5), (14, 5), (11, 9)], fill=shade(c, 20))
    g.rectangle([2, 9, 11, 12], fill=shade(c), outline=shade(c, -50))
    g.polygon([(11, 9), (14, 5), (14, 8), (11, 12)], fill=shade(c, -25))
    return im


def dust(c):
    im, g = canvas()
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 9.5
            if dx * dx / 36 + dy * dy / 16 <= 1:
                im.putpixel((x, y), shade(c, rnd.randint(-25, 25)))
    return im


simple['silicon'] = ingot((70, 70, 90))
simple['aluminum_powder'] = dust((210, 215, 222))
simple['aluminum_ingot'] = ingot((210, 215, 222))
simple['silumin_ingot'] = ingot((190, 200, 215))
simple['red_gold_dust'] = dust((220, 80, 40))
simple['red_gold_ingot'] = ingot((220, 90, 50))
simple['spring_tungsten_ingot'] = ingot((100, 105, 125))
simple['bedrock_dust'] = dust((70, 70, 70))
simple['bedrock_ingot'] = ingot((60, 60, 60))
for name, im in simple.items():
    save(im, name)

for f, c in (('wood', MATS['wood']), ('stone', MATS['stone']), ('iron', (200, 200, 205)), ('gold', GOLD), ('bedrock', MATS['bedrock'])):
    im, g = canvas()
    g.ellipse([1, 1, 14, 14], fill=shade(c), outline=shade(c, -50))
    gear_shape(g, 7.5, 7.5, 2, STEEL, 6)
    save(im, f + '_flywheel_core')

# tags -----------------------------------------------------------------------------------------------------------------
C = R + '/data/c/tags/item'
w(C + '/ingots/aluminum.json', {'values': ['rotarycraft:aluminum_ingot']})
w(C + '/dusts/aluminum.json', {'values': ['rotarycraft:aluminum_powder']})
w(C + '/silicon.json', {'values': ['rotarycraft:silicon']})


# recipes --------------------------------------------------------------------------------------------------------------
def item(i):
    return {'item': i if ':' in i else 'rotarycraft:' + i}


def tag(t):
    return {'tag': t}


STEEL_I = tag('c:ingots/steel')
GOLD_I = tag('c:ingots/gold')
REDSTONE = tag('c:dusts/redstone')
PANEL = item('base_panel')
MOUNT = item('mount')
ROD = item('steel_rod')
GEAR = item('steel_gear')
SPRING = item('spring_steel_ingot')
SPRING_TUNG = item('spring_tungsten_ingot')


def shaped(name, pattern, key, count=1, result=None, suffix=''):
    w('%s/recipe/%s%s.json' % (D, name, suffix), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
      'result': {'id': result or 'rotarycraft:' + name, 'count': count}})


def shapeless(name, ingredients, count=1, suffix=''):
    w('%s/recipe/%s%s.json' % (D, name, suffix), {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': ingredients,
      'result': {'id': 'rotarycraft:' + name, 'count': count}})


def blast_craft(name, pattern, key, temperature, count=1, speed=1):
    w('%s/recipe/blast_crafting/%s.json' % (D, name), {'type': 'rotarycraft:blast_crafting', 'pattern': pattern, 'key': key,
      'result': {'id': 'rotarycraft:' + name, 'count': count}, 'temperature': temperature, 'speed': speed})


def smelt(name, ingredient, result, xp=0.4):
    w('%s/recipe/smelting/%s.json' % (D, name), {'type': 'minecraft:smelting', 'category': 'misc', 'ingredient': ingredient,
      'result': {'id': result, 'count': 1}, 'experience': xp, 'cookingtime': 200})


# engine parts
shaped('impeller', [' S ', 'SGS', ' S '], {'S': STEEL_I, 'G': GEAR})
shaped('impeller', [' S ', 'SGS', ' S '], {'S': tag('c:ingots/tin'), 'G': GEAR}, suffix='_from_tin')
shaped('compressor', ['SSS', 'SGS', 'SSS'], {'S': STEEL_I, 'G': GEAR})
shaped('turbine', ['sss', 'sGs', 'sss'], {'s': item('propeller'), 'G': item('compressor')})
shaped('diffuser', [' SS', 'S  ', ' SS'], {'S': STEEL_I})
shaped('radiator', ['GGG', 'PPP', 'SSS'], {'G': GOLD_I, 'P': tag('c:ingots/copper'), 'S': STEEL_I})  # pipes until the Pipe is ported
shaped('condenser', ['SPS', 'PSP', 'SPS'], {'S': STEEL_I, 'P': tag('c:ingots/copper')}, count=2)  # pipes until the Pipe is ported
shaped('gold_coil', ['GGG', 'GSG', 'GGG'], {'S': STEEL_I, 'G': GOLD_I})
shaped('gold_coil', ['GGG', 'GSG', 'GGG'], {'S': STEEL_I, 'G': tag('c:ingots/electrum')}, suffix='_from_electrum')
shaped('combustor', ['SSS', 'SRS', 'SGS'], {'S': STEEL_I, 'G': item('igniter'), 'R': REDSTONE})
blast_craft('high_combustor', ['SiS', 'iRi', 'SGS'], {'i': item('red_gold_ingot'), 'S': STEEL_I, 'G': item('igniter'), 'R': REDSTONE}, 1100)
shaped('cylinder', ['SSS', 'S S', 'SSS'], {'S': STEEL_I}, count=2)
shaped('silumin_cylinder', ['SSS', 'S S', 'SSS'], {'S': item('silumin_ingot')}, count=2)
shaped('compound_turbine', [' tS', 'tst', 'St '], {'S': item('turbine'), 's': item('tungsten_shaft_core'), 't': item('tungsten_ingot')})
shaped('compound_compressor', [' tS', 'tst', 'St '], {'S': item('compressor'), 's': item('tungsten_shaft_core'), 't': item('tungsten_ingot')})
shaped('igniter', ['G G', 'SRS', 'SSS'], {'S': STEEL_I, 'R': REDSTONE, 'G': GOLD_I})
shaped('water_plate', ['PPP', 'PPP', 'SSS'], {'P': PANEL, 'S': SPRING})
# misc parts
shaped('propeller', [' S ', ' I ', ' P '], {'P': PANEL, 'S': ROD, 'I': STEEL_I})
shaped('hub', ['  B', ' C ', 'G  '], {'G': GEAR, 'B': item('steel_bearing'), 'C': item('shaft_core')})
shaped('mirror', ['GGG', 'III'], {'G': tag('c:glass_blocks'), 'I': tag('c:ingots/iron')})
shaped('mirror', ['GGG', 'III'], {'G': tag('c:glass_blocks'), 'I': tag('c:ingots/silver')}, suffix='_from_silver')
shaped('base_panel', ['SSS'], {'S': STEEL_I}, count=3)
shaped('mount', ['S S', 'SBS'], {'B': PANEL, 'S': STEEL_I})
shaped('mount', ['S S', 'SBS'], {'B': PANEL, 'S': tag('c:ingots/tin')}, suffix='_from_tin')
shaped('drill', ['SSS', 'SSS', ' S '], {'S': STEEL_I})
shaped('screen', ['SGS', 'SCS'], {'S': STEEL_I, 'C': item('circuit_board'), 'G': tag('c:glass_panes')})
shaped('mixer', [' S ', 'SIS', ' S '], {'S': STEEL_I, 'I': item('impeller')})
shaped('saw', ['S S', ' C ', 'S S'], {'S': STEEL_I, 'C': GEAR})
shaped('circuit_board', ['PGP', 'RER', 'GPG'], {'P': STEEL_I, 'G': GOLD_I, 'R': REDSTONE, 'E': tag('c:ender_pearls')}, count=2)
shaped('circuit_board', ['PGP', 'RER', 'GPG'], {'P': STEEL_I, 'G': GOLD_I, 'R': REDSTONE, 'E': item('silicon')}, count=3, suffix='_from_silicon')
shaped('sonar_unit', [' S ', 'SNS', 'RCR'], {'S': STEEL_I, 'R': REDSTONE, 'N': item('minecraft:note_block'), 'C': item('circuit_board')})
shaped('radar_unit', ['SSS', ' G ', 'RMR'], {'S': STEEL_I, 'R': REDSTONE, 'G': GOLD_I, 'M': item('dc_engine')})
shaped('belt', ['LLL', 'LSL', 'LLL'], {'L': tag('c:leathers'), 'S': STEEL_I}, count=8)
shapeless('ball_bearing', [STEEL_I], count=4)
shaped('ball_bearing', ['SS', 'SS'], {'S': STEEL_I}, count=16, suffix='_x16')
shaped('brake', [' g ', 'SBS', ' G '], {'g': item('steel_gear_unit_2'), 'G': GEAR, 'S': ROD, 'B': item('steel_bearing')})
shaped('worm_gear', ['S  ', ' G ', '  S'], {'S': ROD, 'G': GEAR})
shaped('linear_induction_motor', ['WRW', 'NNN'], {'W': item('gold_coil'), 'N': STEEL_I, 'R': REDSTONE})
shaped('generator_unit', ['  G', ' C ', 'G  '], {'G': item('gold_coil'), 'C': item('shaft_core')})
shaped('chain', ['s s', ' s ', 's s'], {'s': STEEL_I}, count=4)
shaped('power_module', ['RER', 'GGG', 'SSS'], {'R': REDSTONE, 'G': GOLD_I, 'E': item('minecraft:ender_eye'), 'S': STEEL_I}, count=2)
shaped('power_module', ['RER', 'GGG', 'SSS'], {'R': REDSTONE, 'G': GOLD_I, 'E': item('silicon'), 'S': STEEL_I}, count=3, suffix='_from_silicon')

# gear materials: base item (cores, gears), rod (shaft unit)
BASE = {'wood': tag('minecraft:planks'), 'stone': item('minecraft:stone'), 'steel': STEEL_I, 'tungsten': SPRING_TUNG,
        'diamond': tag('c:gems/diamond'), 'bedrock': item('bedrock_ingot')}
UNIT = {m: (item('minecraft:stick') if m == 'wood' else item(m + '_rod')) for m in MATS}
for m in MATS:
    core = 'shaft_core' if m == 'steel' else m + '_shaft_core'
    shaped(core, ['  s', ' S ', 's  '], {'S': BASE[m], 's': UNIT[m]})
    shaped('%s_gear_unit_2' % m, [' GB', 'BG '], {'B': UNIT[m], 'G': item(m + '_gear')})
    shaped('%s_gear_unit_4' % m, [' GB', 'BG '], {'B': UNIT[m], 'G': item(m + '_gear_unit_2')})
    shaped('%s_gear_unit_8' % m, [' gB', 'BG '], {'B': UNIT[m], 'G': item(m + '_gear_unit_4'), 'g': item(m + '_gear_unit_2')})
    shaped('%s_gear_unit_8' % m, [' gB', 'BG '], {'B': UNIT[m], 'G': item(m + '_gear_unit_2'), 'g': item(m + '_gear_unit_4')}, suffix='_b')
    shaped('%s_gear_unit_16' % m, [' gB', 'BG '], {'B': UNIT[m], 'G': item(m + '_gear_unit_8'), 'g': item(m + '_gear_unit_2')})
    shaped('%s_gear_unit_16' % m, [' gB', 'BG '], {'B': UNIT[m], 'G': item(m + '_gear_unit_2'), 'g': item(m + '_gear_unit_8')}, suffix='_b')
    shaped('%s_gear_unit_16' % m, [' GB', 'BG '], {'B': UNIT[m], 'G': item(m + '_gear_unit_4')}, suffix='_c')
shaped('wood_gear', [' W ', 'WWW', ' W '], {'W': tag('minecraft:planks')})
shaped('stone_gear', [' W ', 'WWW', ' W '], {'W': item('minecraft:stone')}, count=2)
shaped('steel_gear', [' B ', 'BBB', ' B '], {'B': STEEL_I}, count=3)
shaped('tungsten_gear', [' W ', 'WWW', ' W '], {'W': SPRING_TUNG}, count=5)
blast_craft('diamond_gear', [' W ', 'WGW', ' W '], {'W': tag('c:gems/diamond'), 'G': item('tungsten_gear')}, 1400, count=6)
blast_craft('bedrock_gear', ['bWb', 'WWW', 'bWb'], {'b': item('bedrock_dust'), 'W': STEEL_I}, 1450, count=8, speed=2)
shaped('stone_rod', ['  B', ' B ', 'B  '], {'B': item('minecraft:stone')}, count=2)
shaped('steel_rod', ['  B', ' B ', 'B  '], {'B': STEEL_I}, count=3)
shaped('tungsten_rod', ['  B', ' B ', 'B  '], {'B': SPRING_TUNG}, count=3)
shaped('diamond_rod', ['  B', ' B ', 'B  '], {'B': tag('c:gems/diamond')}, count=3)
blast_craft('bedrock_rod', [' D ', 'DSD', ' D '], {'D': item('bedrock_dust'), 'S': ROD}, 1450, count=4, speed=2)
for m, mid in (('stone', item('stone_gear')), ('steel', STEEL_I), ('tungsten', SPRING_TUNG), ('diamond', item('diamond_gear'))):
    shaped(m + '_bearing', ['LLL', 'LSL', 'LLL'], {'L': item('ball_bearing'), 'S': mid})
blast_craft('bedrock_bearing', ['LLL', 'LSL', 'LLL'], {'L': item('bedrock_dust'), 'S': item('steel_bearing')}, 1450, speed=2)

# alloys
shapeless('red_gold_dust', [REDSTONE, tag('c:dusts/gold')], count=2)
w('%s/recipe/red_gold_dust_from_flakes.json' % D, {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [REDSTONE,
  {'type': 'neoforge:components', 'items': 'rotarycraft:ore_flakes', 'components': {'rotarycraft:ore_product': {'type': 'gold', 'color': 16576075}}}],
  'result': {'id': 'rotarycraft:red_gold_dust', 'count': 2}})
smelt('red_gold_ingot', item('red_gold_dust'), 'rotarycraft:red_gold_ingot')
smelt('aluminum_ingot', item('aluminum_powder'), 'rotarycraft:aluminum_ingot')
smelt('silicon', item('silicon_dust'), 'rotarycraft:silicon')
w('%s/recipe/grinding/aluminum_ingot.json' % D, {'type': 'rotarycraft:grinding', 'ingredient': tag('c:ingots/aluminum'),
  'result': {'id': 'rotarycraft:aluminum_powder', 'count': 1}})
w('%s/recipe/grinding/bedrock_ingot.json' % D, {'type': 'rotarycraft:grinding', 'ingredient': item('bedrock_ingot'),
  'result': {'id': 'rotarycraft:bedrock_dust', 'count': 4}})


def blast(name, main, result, temperature, center=None, lower=None, upper=None, count=1, bonus=0.0):
    r = {'type': 'rotarycraft:blast_furnace', 'main': main, 'result': {'id': result, 'count': count}, 'temperature': temperature}
    for k, v in (('center', center), ('lower', lower), ('upper', upper)):
        if v:
            r[k] = {'ingredient': v[0], 'chance': v[1] / 100.0, 'count': v[2] if len(v) > 2 else 1}
    if bonus:
        r['bonus_yield'] = bonus
    w('%s/recipe/blast_furnace/%s.json' % (D, name), r)


blast('silicon_dust', tag('c:sands'), 'rotarycraft:silicon_dust', 700, center=(tag('c:dusts/aluminum'), 25), lower=(item('minecraft:blaze_powder'), 2.5), bonus=0.8)
blast('silumin_ingot', tag('c:ingots/aluminum'), 'rotarycraft:silumin_ingot', 900, center=(item('silicon_dust'), 20))
blast('spring_tungsten_ingot', SPRING, 'rotarycraft:spring_tungsten_ingot', 1100,
      center=({'type': 'neoforge:components', 'items': 'rotarycraft:ore_flakes', 'components': {'rotarycraft:ore_product': {'type': 'tungsten', 'color': 0x5A6470}}}, 5),
      upper=(item('minecraft:obsidian'), 20))
blast('bedrock_ingot', STEEL_I, 'rotarycraft:bedrock_ingot', 1450, center=(item('bedrock_dust'), 100, 4))

# flywheel cores
for f, raw in (('wood', tag('minecraft:planks')), ('stone', item('minecraft:stone')), ('iron', tag('c:ingots/iron')), ('gold', GOLD_I),
               ('bedrock', item('bedrock_ingot'))):
    shaped(f + '_flywheel_core', ['WWW', 'WGW', 'WWW'], {'W': raw, 'G': GEAR})

# the original machine recipes -------------------------------------------------------------------------------------------
M = 'rotarycraft:'
shaped('dc_engine', ['SSS', 'SRs', 'PRP'], {'S': STEEL_I, 'R': REDSTONE, 'P': PANEL, 's': ROD})
shaped('wind_engine', ['SSS', 'SHS', 'SSS'], {'S': item('propeller'), 'H': item('hub')}, count=2)
shaped('steam_engine', ['ccc', 'CIs', 'PGP'], {'c': item('minecraft:cobblestone'), 'I': item('impeller'), 'P': PANEL, 's': ROD, 'G': GOLD_I, 'C': item('condenser')})
shaped('gas_engine', ['CgC', 'SGs', 'PIP'], {'g': GOLD_I, 'S': item('igniter'), 'I': item('impeller'), 'P': PANEL, 's': ROD, 'G': item('steel_gear_unit_2'), 'C': item('cylinder')})
shaped('ac_engine', ['SSS', 'SGs', 'PRP'], {'S': GOLD_I, 'R': REDSTONE, 'P': PANEL, 's': ROD, 'G': item('gold_coil')})
shaped('performance_engine', ['CrC', 'SGs', 'PIP'], {'C': item('silumin_cylinder'), 'S': item('igniter'), 'I': item('impeller'), 'P': PANEL, 's': ROD,
       'r': item('radiator'), 'G': item('steel_gear_unit_2')})
shaped('jet_engine', ['DCS', 'ScS', 'PTs'], {'S': item('silumin_ingot'), 'D': item('diffuser'), 'C': item('compound_compressor'),
       'c': item('high_combustor'), 'T': item('compound_turbine'), 'P': PANEL, 's': ROD})
shaped('afterburner_upgrade', ['SEI', 'ERE', 'SEI'], {'R': item('compound_turbine'), 'S': item('high_combustor'), 'I': item('igniter'), 'E': item('bedrock_dust')})
shaped('microturbine', ['CSS', 'cTs', 'PPP'], {'S': item('silumin_ingot'), 'C': item('compressor'), 'c': item('high_combustor'), 'T': item('turbine'), 'P': PANEL, 's': ROD})
shaped('bevel_gear', ['ISB', 'SGB', 'BBB'], {'B': PANEL, 'I': STEEL_I, 'S': ROD, 'G': GEAR}, count=4)
shaped('splitter', ['ISP', 'SGP', 'ISP'], {'P': PANEL, 'I': STEEL_I, 'S': ROD, 'G': GEAR}, count=2)
shaped('clutch', ['S', 'M', 'R'], {'M': MOUNT, 'S': ROD, 'R': REDSTONE})
shaped('dynamometer', [' S ', ' E ', ' Ms'], {'s': item('screen'), 'M': MOUNT, 'S': ROD, 'E': tag('c:ender_pearls')}, count=2)
shaped('dynamometer', [' S ', ' E ', ' Ms'], {'s': item('screen'), 'M': MOUNT, 'S': ROD, 'E': item('silicon')}, count=4, suffix='_from_silicon')
shaped('grinder', ['B B', 'SGS', 'PPP'], {'B': STEEL_I, 'G': GEAR, 'P': PANEL, 'S': item('saw')})
shaped('extractor', ['SWS', 'siD', 'PIN'], {'D': item('drill'), 'P': PANEL, 'S': STEEL_I, 'I': ROD, 's': item('minecraft:stone'), 'i': item('impeller'),
       'N': item('minecraft:netherrack'), 'W': tag('minecraft:planks')})
shaped('blast_furnace', ['BBB', 'BrB', 'BBB'], {'B': item('minecraft:stone_bricks'), 'r': REDSTONE})
shaped('friction_heater', ['S  ', 'Sss', 'SPP'], {'P': PANEL, 'S': STEEL_I, 's': ROD})
shaped('fermenter', ['BPB', 'PIP', 'BPB'], {'B': STEEL_I, 'I': item('impeller'), 'P': PANEL})
shaped('centrifuge', ['SGS', 'S S', 'PgP'], {'P': PANEL, 'g': item('steel_gear_unit_4'), 'S': STEEL_I, 'G': tag('c:glass_panes')})
shaped('rock_melter', ['SRS', 'PGP', 'SsS'], {'s': ROD, 'S': STEEL_I, 'R': item('minecraft:cauldron'), 'P': PANEL, 'G': GEAR})  # cauldron until the Reservoir is ported
shaped('fractionator', ['GFG', 'GIG', 'GPG'], {'P': PANEL, 'I': item('mixer'), 'G': GOLD_I, 'F': item('minecraft:bucket')})  # bucket until the Fuel Line is ported
shaped('magnetizer', ['p p', 'gmg', 'prp'], {'r': REDSTONE, 'p': PANEL, 'm': MOUNT, 'g': item('gold_coil')})
shaped('generator', ['gpS', 'iGs', 'psp'], {'S': STEEL_I, 'p': PANEL, 'g': GOLD_I, 'G': item('generator_unit'), 'i': item('impeller'), 's': item('shaft_core')})
shaped('electric_motor', ['cGS', 'BCB', 'SGS'], {'c': STEEL_I, 'G': item('gold_coil'), 'S': STEEL_I, 'B': PANEL, 'C': item('diamond_shaft_core')})
shaped('screwdriver', ['I  ', ' S ', '  W'], {'S': item('minecraft:stick'), 'I': tag('c:ingots/iron'), 'W': tag('minecraft:planks')})
shaped('angular_transducer', [' W ', 'WEW', 'SSS'], {'S': STEEL_I, 'E': REDSTONE, 'W': tag('minecraft:planks')})
# shafts: 8 from a rod on a mount (wood: a stick framed by planks; stone: a stone rod in stone slabs)
for m in ('steel', 'diamond', 'bedrock'):
    shaped('shaft_' + m, ['S', 'M'], {'M': MOUNT, 'S': item(m + '_rod')}, count=8)
shaped('shaft_wood', ['BSB', 'BBB'], {'B': tag('minecraft:planks'), 'S': item('minecraft:stick')}, count=8)
shaped('shaft_stone', ['BSB', 'BBB'], {'B': item('minecraft:smooth_stone_slab'), 'S': item('stone_rod')}, count=8)
# gearboxes: a gear unit on its material's mount (planks for wood, smooth stone slabs for stone, a mount otherwise)
for m in MATS:
    for r in RATIOS:
        unit = item('%s_gear_unit_%d' % (m, r))
        if m == 'wood':
            shaped('gearbox_wood_%dx' % r, ['MGM', 'MMM'], {'M': tag('minecraft:planks'), 'G': unit})
        elif m == 'stone':
            shaped('gearbox_stone_%dx' % r, ['MGM', 'MMM'], {'M': item('minecraft:smooth_stone_slab'), 'G': unit})
        else:
            shaped('gearbox_%s_%dx' % (m, r), ['G', 'M'], {'M': MOUNT, 'G': unit})
for f in ('wood', 'stone', 'iron', 'gold', 'bedrock'):
    shaped('flywheel_' + f, ['W', 'M'], {'W': item(f + '_flywheel_core'), 'M': MOUNT})

# lang -----------------------------------------------------------------------------------------------------------------
lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
for m in MATS:
    n = NAMES[m]
    if m != 'wood':
        lang['item.rotarycraft.%s_rod' % m] = '%s Rod' % n
    lang['item.rotarycraft.%s_gear' % m] = '%s Gear' % n
    for r in RATIOS:
        lang['item.rotarycraft.%s_gear_unit_%d' % (m, r)] = '%s Gear Unit (%d:1)' % (n, r)
    lang['item.rotarycraft.%s_bearing' % m] = '%s Bearing' % n
    if m not in ('steel', 'tungsten'):
        lang['item.rotarycraft.%s_shaft_core' % m] = '%s Shaft Core' % n
lang.update({
    'item.rotarycraft.base_panel': 'Base Panel', 'item.rotarycraft.mount': 'Mount', 'item.rotarycraft.ball_bearing': 'Ball Bearing',
    'item.rotarycraft.worm_gear': 'Worm Gear', 'item.rotarycraft.chain': 'Chain', 'item.rotarycraft.belt': 'Belt', 'item.rotarycraft.brake': 'Brake',
    'item.rotarycraft.impeller': 'Impeller', 'item.rotarycraft.compressor': 'Compressor', 'item.rotarycraft.turbine': 'Turbine',
    'item.rotarycraft.diffuser': 'Diffuser', 'item.rotarycraft.combustor': 'Combustor', 'item.rotarycraft.high_combustor': 'High-Temperature Combustor',
    'item.rotarycraft.cylinder': 'Cylinder', 'item.rotarycraft.silumin_cylinder': 'Silumin Cylinder', 'item.rotarycraft.radiator': 'Radiator',
    'item.rotarycraft.condenser': 'Condenser', 'item.rotarycraft.gold_coil': 'Gold Coil', 'item.rotarycraft.igniter': 'Igniter',
    'item.rotarycraft.water_plate': 'Water Plate', 'item.rotarycraft.compound_turbine': 'Compound Turbine',
    'item.rotarycraft.compound_compressor': 'Compound Compressor', 'item.rotarycraft.propeller': 'Propeller Blade', 'item.rotarycraft.hub': 'Hub',
    'item.rotarycraft.mirror': 'Mirror', 'item.rotarycraft.generator_unit': 'Generator Unit',
    'item.rotarycraft.linear_induction_motor': 'Linear Induction Motor', 'item.rotarycraft.power_module': 'Power Module',
    'item.rotarycraft.circuit_board': 'Circuit Board', 'item.rotarycraft.screen': 'Screen', 'item.rotarycraft.drill': 'Drill',
    'item.rotarycraft.saw': 'Saw', 'item.rotarycraft.mixer': 'Mixer', 'item.rotarycraft.radar_unit': 'Radar Unit', 'item.rotarycraft.sonar_unit': 'Sonar Unit',
    'item.rotarycraft.silicon': 'Silicon', 'item.rotarycraft.aluminum_powder': 'Aluminum Powder', 'item.rotarycraft.aluminum_ingot': 'Aluminum Ingot',
    'item.rotarycraft.silumin_ingot': 'Silumin Ingot', 'item.rotarycraft.red_gold_dust': 'Red Gold Dust', 'item.rotarycraft.red_gold_ingot': 'Red Gold Ingot',
    'item.rotarycraft.spring_tungsten_ingot': 'Spring Tungsten Ingot', 'item.rotarycraft.bedrock_dust': 'Bedrock Dust',
    'item.rotarycraft.bedrock_ingot': 'Bedrock Alloy Ingot',
    'item.rotarycraft.wood_flywheel_core': 'Wooden Flywheel Core', 'item.rotarycraft.stone_flywheel_core': 'Stone Flywheel Core',
    'item.rotarycraft.iron_flywheel_core': 'Iron Flywheel Core', 'item.rotarycraft.gold_flywheel_core': 'Gold Flywheel Core',
    'item.rotarycraft.bedrock_flywheel_core': 'Bedrock Flywheel Core',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('parts ok')
