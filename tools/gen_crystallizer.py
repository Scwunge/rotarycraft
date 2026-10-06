"""Crystallizer data, textures and lang (run by gen_assets.py). Recipes are the original's (RecipesCrystallizer)."""
import json
import os
import random
import shutil

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


def freeze(name, fluid, amount, result, count=1):
    w('%s/recipe/crystallizing/%s.json' % (D, name), {'type': 'rotarycraft:crystallizing',
      'fluid': {'id': fluid, 'amount': amount}, 'result': {'id': result, 'count': count}})


freeze('ice', 'minecraft:water', 1000, 'minecraft:ice')
freeze('snowball', 'minecraft:water', 200, 'minecraft:snowball')
freeze('stone', 'minecraft:lava', 1000, 'minecraft:stone')
freeze('ethanol_crystals', 'rotarycraft:ethanol', 1000, 'rotarycraft:ethanol_crystals')

# dry ice: the original had no machine recipe for it that survives in this port, so it is crafted from packed ice
w(D + '/recipe/dry_ice.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
  'ingredients': [{'item': 'minecraft:packed_ice'}, {'item': 'minecraft:packed_ice'}, {'item': 'minecraft:snowball'}],
  'result': {'id': 'rotarycraft:dry_ice', 'count': 2}})

shutil.copy('reference/RotaryCraft/Textures/GUI/crystalgui.png', T + '/gui/crystallizer.png')


def shade(c, d):
    return tuple(max(0, min(255, x + d)) for x in c) + (255,)


im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.rectangle([3, 3, 12, 12], fill=(150, 205, 235, 255), outline=(40, 80, 110, 255))
for x, y, h in ((5, 4, 5), (8, 4, 7), (11, 4, 4)):
    g.line([(x, y), (x, y + h)], fill=(235, 248, 255, 255))
im.save(T + '/block/crystallizer_front.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.polygon([(3, 8), (8, 3), (13, 8), (8, 13)], fill=(214, 232, 240, 255), outline=(120, 150, 170, 255))
for _ in range(10):
    x, y = rnd.randint(5, 10), rnd.randint(5, 10)
    g.point((x, y), fill=(255, 255, 255, 255))
im.save(T + '/item/dry_ice.png')
w(A + '/models/item/dry_ice.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/dry_ice'}})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.crystallizer': 'Crystallizer',
    'item.rotarycraft.dry_ice': 'Dry Ice',
    'gui.rotarycraft.crystallizer.now': '%s C',
    'gui.rotarycraft.crystallizer.freezes': 'Freezes at %s C',
    'gui.rotarycraft.crystallizer.needs': 'Needs 1 N*m, 1024 rad/s and 2 kW; colder than the fluid\'s freezing point',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('crystallizer data ok')
