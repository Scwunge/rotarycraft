"""Fermenter data, textures and lang (run by gen_assets.py)."""
import json
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
rnd = random.Random(31)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


# plant matter for sludge, values from the original (PlantMaterials / MulchMaterials)
MULCH = {
    1: ['minecraft:sugar_cane', 'minecraft:lily_pad', '#minecraft:saplings', '#minecraft:small_flowers', 'minecraft:potato',
        'rotarycraft:sawdust', 'minecraft:fern'],
    2: ['minecraft:short_grass', 'minecraft:tall_grass', 'minecraft:large_fern', 'minecraft:vine', '#minecraft:leaves'],
    4: ['#minecraft:tall_flowers'],
    8: [],
}
for v, values in MULCH.items():
    w('%s/tags/item/mulch/%d.json' % (D, v), {'values': values})

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


blob(T + '/item/yeast.png', (220, 205, 160))
blob(T + '/item/sludge.png', (95, 110, 55), 7, 5.5)
for it in ('yeast', 'sludge'):
    w('%s/models/item/%s.json' % (A, it), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + it}})

im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.rectangle([3, 3, 12, 12], fill=(60, 90, 50, 255), outline=(30, 45, 25, 255))
for _ in range(6):
    x, y = rnd.randint(4, 11), rnd.randint(4, 11)
    g.point((x, y), fill=(200, 220, 150, 255))
im.save(T + '/block/fermenter_front.png')


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.fermenter': 'Fermenter',
    'item.rotarycraft.yeast': 'Yeast',
    'item.rotarycraft.sludge': 'Sludge',
    'gui.rotarycraft.fermenter.best': 'Best: 25 C for yeast, 35 C for sludge; yeast dies at 60 C',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('fermenter data ok')
