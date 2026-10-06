"""Rock Melter and fluid data, textures and lang (run by gen_assets.py). Melting values are the original's (RecipesLavaMaker)."""
import json
import os
import random
import shutil

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
rnd = random.Random(53)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def melt(name, ingredient, fluid, amount, temperature, energy):
    w('%s/recipe/melting/%s.json' % (D, name), {'type': 'rotarycraft:melting', 'ingredient': ingredient,
      'result': {'id': fluid, 'amount': amount}, 'temperature': temperature, 'energy': energy})


ROCK_MELT_ENERGY = 5_200_000
melt('stone', {'tag': 'c:stones'}, 'minecraft:lava', 1000, 1000, ROCK_MELT_ENERGY)
melt('cobblestone', {'tag': 'c:cobblestones'}, 'minecraft:lava', 500, 1000, 3_120_000)
melt('netherrack', {'tag': 'c:netherracks'}, 'minecraft:lava', 2000, 600, 480_000)
melt('stone_bricks', {'item': 'minecraft:stone_bricks'}, 'minecraft:lava', 1000, 1200, 4_160_000)
melt('ethanol_crystals', {'item': 'rotarycraft:ethanol_crystals'}, 'rotarycraft:ethanol', 1000, 180, 6000)
melt('clean_sludge', {'item': 'rotarycraft:clean_sludge'}, 'rotarycraft:ethanol', 1000, 180, 9000)

# fluids: block states, models, buckets, tags ----------------------------------------------------------------------------
FLUIDS = {'ethanol': 'Ethanol', 'jet_fuel': 'Jet Fuel', 'lubricant': 'Lubricant', 'liquid_nitrogen': 'Liquid Nitrogen'}
for f in FLUIDS:
    w('%s/blockstates/%s.json' % (A, f), {'variants': {'': {'model': 'rotarycraft:block/' + f}}})
    w('%s/models/block/%s.json' % (A, f), {'textures': {'particle': 'minecraft:block/water_still'}})
    w('%s/models/item/%s_bucket.json' % (A, f), {'parent': 'neoforge:item/bucket', 'loader': 'neoforge:fluid_container',
                                                 'fluid': 'rotarycraft:' + f})
    w('%s/tags/fluid/%s.json' % (D.replace('/rotarycraft', '/c'), f), {'values': ['rotarycraft:' + f, 'rotarycraft:flowing_' + f]})

# textures -------------------------------------------------------------------------------------------------------------
T = A + '/textures'
shutil.copy('reference/RotaryCraft/Textures/GUI/lavamakergui.png', T + '/gui/rock_melter.png')


def shade(c, d):
    return tuple(max(0, min(255, x + d)) for x in c) + (255,)


im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.rectangle([3, 3, 12, 12], fill=(60, 30, 20, 255), outline=(30, 20, 15, 255))
for _ in range(14):
    x, y = rnd.randint(4, 11), rnd.randint(4, 11)
    g.point((x, y), fill=shade((235, 110, 20), rnd.randint(-30, 20)))
im.save(T + '/block/rock_melter_front.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
for cx, cy, r in ((6, 9, 3), (10, 7, 3), (8, 12, 2), (11, 11, 2)):
    g.polygon([(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)], fill=shade((92, 197, 178), rnd.randint(-15, 15)), outline=(40, 110, 100, 255))
im.save(T + '/item/ethanol_crystals.png')
w('%s/models/item/ethanol_crystals.json' % A, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/ethanol_crystals'}})


lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.rock_melter': 'Rock Melter',
    'item.rotarycraft.ethanol_crystals': 'Ethanol Crystals',
    'gui.rotarycraft.temperature_short': '%s C',
})
for f, name in FLUIDS.items():
    lang['fluid_type.rotarycraft.' + f] = name
    lang['block.rotarycraft.' + f] = name
    lang['item.rotarycraft.%s_bucket' % f] = name + ' Bucket'
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('rock melter and fluids ok')
