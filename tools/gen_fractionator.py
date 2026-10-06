"""Fractionation Unit, coal dust and jet fuel data, textures and lang (run by gen_assets.py)."""
import json
import os
import random
import shutil

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'
rnd = random.Random(67)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def shade(c, d):
    return tuple(max(0, min(255, x + d)) for x in c) + (255,)


im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        dx, dy = x - 7.5, y - 9.5
        if dx * dx / 36 + dy * dy / 16 <= 1:
            p[x, y] = shade((40, 40, 44), rnd.randint(-15, 15))
im.save(T + '/item/coal_dust.png')
w('%s/models/item/coal_dust.json' % A, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/coal_dust'}})
w(R + '/data/c/tags/item/dusts/coal.json', {'values': ['rotarycraft:coal_dust']})

shutil.copy('reference/RotaryCraft/Textures/GUI/fractiongui3b.png', T + '/gui/fractionator.png')

im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.rectangle([3, 2, 7, 13], fill=(251, 92, 144, 255), outline=(120, 40, 70, 255))
g.rectangle([9, 2, 12, 13], fill=(92, 197, 178, 255), outline=(40, 110, 100, 255))
im.save(T + '/block/fractionator_front.png')

w(D + '/recipe/fractionator.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['GFG', 'GMG', 'GPG'],
    'key': {'G': {'tag': 'c:ingots/gold'}, 'F': {'item': 'minecraft:bucket'}, 'M': {'item': 'rotarycraft:gearbox_4x'},
            'P': {'tag': 'c:ingots/steel'}},
    'result': {'id': 'rotarycraft:fractionator', 'count': 1}})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'item.rotarycraft.coal_dust': 'Coal Dust',
    'block.rotarycraft.fractionator': 'Fractionation Unit',
    'gui.rotarycraft.jet_fuel': 'Jet fuel: %s / %s mB',
    'gui.rotarycraft.ethanol': 'Ethanol: %s / %s mB',
    'gui.rotarycraft.pressure': 'Pressure: %s kPa',
    'gui.rotarycraft.fractionator.yield': 'Yield: %s%% of the ethanol used',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('fractionator data ok')
