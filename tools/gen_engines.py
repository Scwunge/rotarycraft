"""Fuel-burning engines: textures, recipes and lang (run by gen_assets.py)."""
import json
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'
rnd = random.Random(61)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def shade(c, d):
    return tuple(max(0, min(255, x + d)) for x in c) + (255,)


def panel():
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
    g = ImageDraw.Draw(im)
    g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
    return im, g


# Gas Engine: two cylinders and an exhaust
im, g = panel()
for x0 in (2, 9):
    g.rectangle([x0, 3, x0 + 4, 12], fill=(150, 155, 165, 255), outline=(50, 52, 60, 255))
    g.line([(x0 + 1, 5), (x0 + 3, 5)], fill=(90, 92, 100, 255))
g.rectangle([6, 13, 9, 14], fill=(92, 197, 178, 255))
im.save(T + '/block/gas_engine_front.png')

w(D + '/recipe/gas_engine.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['CgC', 'FGs', 'IHI'],
    'key': {'C': {'item': 'minecraft:piston'}, 'g': {'tag': 'c:ingots/gold'}, 'F': {'item': 'minecraft:flint_and_steel'},
            'G': {'item': 'rotarycraft:gearbox_2x'}, 's': {'item': 'rotarycraft:shaft_steel'}, 'I': {'tag': 'c:ingots/steel'},
            'H': {'item': 'minecraft:hopper'}},
    'result': {'id': 'rotarycraft:gas_engine', 'count': 1}})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.gas_engine': 'Gas Engine',
    'gui.rotarycraft.fuel': 'Fuel: %s / %s mB',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('engines ok')
