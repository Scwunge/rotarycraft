"""Blast Furnace and Friction Heater data, textures and lang (run by gen_assets.py)."""
import json
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
rnd = random.Random(23)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def add(ing, chance, count=1):
    """Additive: chance in percent per item made, as written in the original."""
    return {'ingredient': ing, 'chance': chance / 100.0, 'count': count}


IRON = {'item': 'minecraft:iron_ingot'}
GUNPOWDER = {'item': 'minecraft:gunpowder'}
SAND = {'tag': 'c:sands'}
COKE = {'item': 'rotarycraft:coke'}

# the original's recipes (RecipesBlastFurnace), 600 C for steel
BLAST = {
    'hsla_steel_from_coal': {'center': add({'item': 'minecraft:coal'}, 100), 'lower': add(GUNPOWDER, 3.6), 'upper': add(SAND, 0.2),
                             'main': IRON, 'result': {'id': 'rotarycraft:hsla_steel_ingot', 'count': 1}, 'temperature': 600, 'xp': 0.6},
    'hsla_steel_from_charcoal': {'center': add({'item': 'minecraft:charcoal'}, 100), 'lower': add(GUNPOWDER, 3.2), 'upper': add(SAND, 0.2),
                                 'main': IRON, 'result': {'id': 'rotarycraft:hsla_steel_ingot', 'count': 1}, 'temperature': 600, 'xp': 0.6},
    'hsla_steel_from_coke': {'center': add(COKE, 100), 'lower': add(GUNPOWDER, 1.8), 'upper': add(SAND, 0.1),
                             'main': IRON, 'result': {'id': 'rotarycraft:hsla_steel_ingot', 'count': 1}, 'temperature': 600, 'xp': 0.6,
                             'bonus_yield': 1.0},
    'coke': {'main': {'item': 'minecraft:coal'}, 'result': {'id': 'rotarycraft:coke', 'count': 1}, 'temperature': 400},
    'spring_steel': {'center': add(COKE, 75), 'lower': add({'item': 'minecraft:redstone'}, 40),
                     'main': {'tag': 'c:ingots/steel'}, 'result': {'id': 'rotarycraft:spring_steel_ingot', 'count': 1},
                     'temperature': 1000},
    # nine scrap (from exploded machines) melt back into a steel ingot at the steel temperature
    'steel_from_scrap': {'main': {'item': 'rotarycraft:scrap'}, 'main_per_result': 9,
                         'result': {'id': 'rotarycraft:hsla_steel_ingot', 'count': 1}, 'temperature': 600},
}
for name, r in BLAST.items():
    recipe = {'type': 'rotarycraft:blast_furnace'}
    recipe.update(r)
    w('%s/recipe/blast_furnace/%s.json' % (D, name), recipe)

# steel only from the Blast Furnace now (the temporary vanilla blasting recipe goes away)
old = D + '/recipe/hsla_steel_ingot_blasting.json'
if os.path.exists(old):
    os.remove(old)

T = A + '/textures'


def shade(c, d):
    return tuple(max(0, min(255, v + d)) for v in c) + (255,)


def panel(base=(72, 76, 84)):
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            p[x, y] = shade(base, rnd.randint(-6, 6))
    g = ImageDraw.Draw(im)
    g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
    return im, g


# blast furnace: brick body with a glowing mouth
im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        mortar = y % 4 == 3 or (x + (4 if (y // 4) % 2 else 0)) % 8 == 7
        p[x, y] = (150, 150, 150, 255) if mortar else shade((140, 60, 45), rnd.randint(-12, 12))
im.save(T + '/block/blast_furnace_side.png')
im2 = im.copy()
g = ImageDraw.Draw(im2)
g.rectangle([4, 7, 11, 13], fill=(40, 20, 15, 255))
g.rectangle([5, 9, 10, 13], fill=(255, 150, 40, 255))
g.rectangle([6, 11, 9, 13], fill=(255, 230, 120, 255))
im2.save(T + '/block/blast_furnace_front.png')

im, g = panel()
g.ellipse([3, 3, 12, 12], fill=(170, 60, 30, 255), outline=(90, 30, 15, 255))
g.ellipse([6, 6, 9, 9], fill=(255, 200, 80, 255))
im.save(T + '/block/friction_heater_front.png')

# items
im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        dx, dy = x - 7.5, y - 8
        if dx * dx / 30 + dy * dy / 22 <= 1:
            p[x, y] = shade((55, 55, 60), rnd.randint(-15, 15))
im.save(T + '/item/coke.png')
im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
col = (120, 135, 160)
g.polygon([(2, 10), (6, 6), (14, 6), (10, 10)], fill=col + (255,))
g.polygon([(2, 10), (10, 10), (10, 12), (2, 12)], fill=tuple(c - 40 for c in col) + (255,))
g.polygon([(10, 10), (14, 6), (14, 8), (10, 12)], fill=tuple(c - 60 for c in col) + (255,))
g.line([(5, 8), (12, 8)], fill=(190, 205, 230, 255))
im.save(T + '/item/spring_steel_ingot.png')
for it in ('coke', 'spring_steel_ingot'):
    w('%s/models/item/%s.json' % (A, it), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + it}})

# blast furnace model: brick all round, glowing front
w(A + '/models/block/blast_furnace.json', {'parent': 'minecraft:block/cube', 'textures': {
    'north': 'rotarycraft:block/blast_furnace_front', 'south': 'rotarycraft:block/blast_furnace_side', 'east': 'rotarycraft:block/blast_furnace_side',
    'west': 'rotarycraft:block/blast_furnace_side', 'up': 'rotarycraft:block/blast_furnace_side', 'down': 'rotarycraft:block/blast_furnace_side',
    'particle': 'rotarycraft:block/blast_furnace_side'}})

# crafting: the blast furnace needs no steel (it is how steel is made)

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.blast_furnace': 'Blast Furnace',
    'block.rotarycraft.friction_heater': 'Friction Heater',
    'item.rotarycraft.coke': 'Coke',
    'item.rotarycraft.spring_steel_ingot': 'Spring Steel Ingot',
    'gui.rotarycraft.temperature': 'Temperature: %s C',
    'gui.rotarycraft.blast_furnace.hot': 'Hot enough for steel (%s C)',
    'gui.rotarycraft.blast_furnace.cold': 'Steel needs %s C: heat with lava, fire or a Friction Heater',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('blast furnace data:', len(BLAST), 'recipes')
