"""Centrifuge data, textures and lang (run by gen_assets.py). Recipes and chances are the original's (RecipesCentrifuge)."""
import json
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
rnd = random.Random(47)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def out(item, chance, count=1, components=None):
    stack = {'id': item, 'count': count}
    if components:
        stack['components'] = components
    return {'item': stack, 'chance': chance}


def flakes(ore, color):
    return {'rotarycraft:ore_product': {'type': ore, 'color': color}}


def spin(name, ingredient, outputs, per_operation=1, fluid=None, fluid_chance=100, conditions=None):
    r = {'type': 'rotarycraft:centrifuge', 'ingredient': ingredient, 'outputs': outputs}
    if per_operation != 1:
        r['per_operation'] = per_operation
    if fluid:
        r['fluid'] = fluid
        if fluid_chance != 100:
            r['fluid_chance'] = fluid_chance
    if conditions:
        r['neoforge:conditions'] = conditions
    w('%s/recipe/centrifuge/%s.json' % (D, name), r)


MEK = {'type': 'neoforge:mod_loaded', 'modid': 'mekanism'}
NO_MEK = {'type': 'neoforge:not', 'value': MEK}

spin('magma_cream', {'item': 'minecraft:magma_cream'}, [out('minecraft:slime_ball', 100), out('minecraft:blaze_powder', 100)])
spin('melon', {'item': 'minecraft:melon_slice'}, [out('minecraft:melon_seeds', 100, 4)])
spin('pumpkin', {'item': 'minecraft:pumpkin'}, [out('minecraft:pumpkin_seeds', 100, 12)])
spin('wheat', {'item': 'minecraft:wheat'}, [out('minecraft:wheat_seeds', 100, 4)])
spin('gravel', {'tag': 'c:gravels'}, [out('minecraft:flint', 50), out('minecraft:sand', 75)])
spin('dirt', {'item': 'minecraft:dirt'}, [out('minecraft:sand', 80), out('minecraft:clay', 10),
     out('minecraft:wheat_seeds', 2), out('minecraft:pumpkin_seeds', 0.125), out('minecraft:melon_seeds', 0.125),
     out('minecraft:oak_sapling', 0.03125), out('minecraft:short_grass', 0.0625)])
spin('sludge', {'item': 'rotarycraft:sludge'}, [out('rotarycraft:clean_sludge', 80), out('rotarycraft:clean_sludge', 20), out('rotarycraft:compost', 25)],
     per_operation=2)
spin('clay', {'item': 'minecraft:clay'}, [out('minecraft:dirt', 100), out('rotarycraft:silicon_dust', 75),
     out('rotarycraft:ore_flakes', 0.5, components=flakes('iron', 14200723)), out('rotarycraft:ore_flakes', 0.2, components=flakes('gold', 16576075)),
     out('minecraft:short_grass', 2.5)], fluid={'id': 'minecraft:water', 'amount': 20}, fluid_chance=40)
for g in ('short_grass', 'fern'):
    # the original spins grass into the seeds tall grass drops; in 1.21 that is wheat seeds
    spin(g, {'item': 'minecraft:' + g}, [out('minecraft:wheat_seeds', 100)])

# sulfur is an ore-dictionary product in the original; it comes out as Mekanism's sulfur dust when Mekanism is present
for name, ingredient, base, sulfur in (
        ('netherrack_dust', {'item': 'rotarycraft:netherrack_dust'}, [out('minecraft:glowstone_dust', 25), out('minecraft:gunpowder', 80)], 40),
        ('blaze_powder', {'item': 'minecraft:blaze_powder'}, [out('minecraft:gunpowder', 100)], 75)):
    spin(name, ingredient, base, conditions=[NO_MEK])
    spin(name + '_mekanism', ingredient, base + [out('mekanism:dust_sulfur', sulfur)], conditions=[MEK])

# items and the machine's face -----------------------------------------------------------------------------------------
T = A + '/textures'


def shade(c, d):
    return tuple(max(0, min(255, x + d)) for x in c) + (255,)


def blob(path, c, rx=6.5, ry=5.0, cy=9.0):
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - cy
            if dx * dx / (rx * rx) + dy * dy / (ry * ry) <= 1:
                p[x, y] = shade(c, rnd.randint(-18, 18))
    im.save(path)


blob(T + '/item/clean_sludge.png', (150, 160, 95), 7, 5.5)
blob(T + '/item/compost.png', (85, 62, 40), 7, 5.0)
blob(T + '/item/silicon_dust.png', (120, 120, 130), 6, 4.0, 10)
for it in ('clean_sludge', 'compost', 'silicon_dust'):
    w('%s/models/item/%s.json' % (A, it), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + it}})

im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.ellipse([2, 2, 13, 13], fill=(150, 160, 175, 255), outline=(50, 52, 60, 255))
g.ellipse([6, 6, 9, 9], fill=(60, 62, 70, 255))
for a in range(4):
    x, y = [(7, 3), (12, 7), (8, 12), (3, 8)][a]
    g.point((x, y), fill=(230, 230, 235, 255))
im.save(T + '/block/centrifuge_front.png')


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.centrifuge': 'Centrifuge',
    'item.rotarycraft.clean_sludge': 'Clean Sludge',
    'item.rotarycraft.compost': 'Compost',
    'item.rotarycraft.silicon_dust': 'Silicon Dust',
    'gui.rotarycraft.tank': '%s: %s / %s mB',
    'gui.rotarycraft.empty': 'Empty',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('centrifuge data ok')
