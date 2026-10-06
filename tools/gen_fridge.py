"""Refrigerator and salt data, textures and lang (run by gen_assets.py)."""
import json
import os
import random
import shutil

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'
rnd = random.Random(83)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def shade(c, d):
    return tuple(max(0, min(255, x + d)) for x in c) + (255,)


shutil.copy('reference/RotaryCraft/Textures/GUI/fridgegui.png', T + '/gui/refrigerator.png')

im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.rectangle([3, 2, 12, 13], fill=(200, 215, 230, 255), outline=(60, 80, 100, 255))
g.line([(3, 7), (12, 7)], fill=(60, 80, 100, 255))
g.rectangle([10, 4, 11, 5], fill=(60, 80, 100, 255))
g.rectangle([10, 9, 11, 11], fill=(60, 80, 100, 255))
im.save(T + '/block/refrigerator_front.png')

# salt: a pile of white crystals
im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
for cx, cy, r in ((7, 10, 4), (11, 11, 3), (5, 8, 2)):
    g.ellipse([cx - r, cy - r + 1, cx + r, cy + r], fill=shade((235, 235, 235), rnd.randint(-10, 8)), outline=(170, 170, 180, 255))
for _ in range(8):
    g.point((rnd.randint(3, 12), rnd.randint(6, 13)), fill=(255, 255, 255, 255))
im.save(T + '/item/salt.png')
w(A + '/models/item/salt.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/salt'}})
w(R + '/data/c/tags/item/dusts/salt.json', {'values': ['rotarycraft:salt']})

w(D + '/recipe/refrigerator.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SPS', 'CcD', 'pPp'],
  'key': {'S': {'tag': 'c:ingots/steel'}, 'P': {'item': 'rotarycraft:pipe'}, 'C': {'item': 'rotarycraft:compressor'},
          'c': {'item': 'rotarycraft:condenser'}, 'D': {'item': 'rotarycraft:diffuser'}, 'p': {'item': 'rotarycraft:base_panel'}},
  'result': {'id': 'rotarycraft:refrigerator', 'count': 1}})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.refrigerator': 'Refrigerator',
    'item.rotarycraft.salt': 'Salt',
    'gui.rotarycraft.refrigerator.needs': 'Needs 2048 N*m (any speed) and 32 kW; one ice block makes liquid nitrogen each cycle',
})
w(lang_path, lang)
print('refrigerator ok')
