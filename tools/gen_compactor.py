"""Compactor, coal stages, storage blocks and the press head (run by gen_assets.py, before gen_parts.py)."""
import json
import os
import random
import shutil

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'
rnd = random.Random(97)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def shade(c, d=0):
    return tuple(max(0, min(255, x + d)) for x in c[:3]) + (255,)


shutil.copy('reference/RotaryCraft/Textures/GUI/compactorgui2.png', T + '/gui/compactor.png')


def gem(name, c, facets=True):
    im = Image.new('RGBA', (16, 16))
    g = ImageDraw.Draw(im)
    g.polygon([(8, 2), (13, 6), (11, 13), (5, 13), (3, 6)], fill=shade(c), outline=shade(c, -60))
    if facets:
        g.line([(8, 2), (8, 13)], fill=shade(c, 40))
        g.line([(3, 6), (13, 6)], fill=shade(c, 30))
    im.save('%s/item/%s.png' % (T, name))
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name}})


gem('anthracite', (35, 35, 40), facets=False)
gem('prismane', (120, 60, 140))
gem('lonsdaleite', (200, 230, 240))
im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.polygon([(2, 3), (13, 3), (11, 13), (4, 13)], fill=(70, 70, 75, 255), outline=(30, 30, 30, 255))
g.rectangle([5, 9, 10, 12], fill=(90, 220, 215, 255))
im.save(T + '/item/press_head.png')
w('%s/models/item/press_head.json' % A, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/press_head'}})

for block, c in (('anthracite_block', (30, 30, 35)), ('lonsdaleite_block', (200, 230, 240))):
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            p[x, y] = shade(c, rnd.randint(-14, 14))
    im.save('%s/block/%s.png' % (T, block))
    w('%s/blockstates/%s.json' % (A, block), {'variants': {'': {'model': 'rotarycraft:block/' + block}}})
    w('%s/models/block/%s.json' % (A, block), {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'rotarycraft:block/' + block}})
    w('%s/models/item/%s.json' % (A, block), {'parent': 'rotarycraft:block/' + block})
    w('%s/loot_table/blocks/%s.json' % (D, block), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + block}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})

# compactor front: a press
im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.rectangle([3, 2, 12, 6], fill=(150, 155, 165, 255), outline=(50, 52, 60, 255))
g.rectangle([3, 9, 12, 13], fill=(150, 155, 165, 255), outline=(50, 52, 60, 255))
g.rectangle([6, 7, 9, 8], fill=(35, 35, 40, 255))
im.save(T + '/block/compactor_front.png')

path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += [v for v in ['rotarycraft:anthracite_block', 'rotarycraft:lonsdaleite_block'] if v not in data['values']]
w(path, data)


# the original's compacting recipes (RecipesCompactor), 2 per step at medium difficulty; coal stages at 550 MPa and 800 C
def compact(name, ingredient, result, count, pressure, temperature, stage=1):
    w('%s/recipe/compacting/%s.json' % (D, name), {'type': 'rotarycraft:compacting', 'ingredient': ingredient,
      'result': {'id': result, 'count': count}, 'pressure': pressure, 'temperature': temperature, 'stage': stage})


RP, RT = 550_000, 800
compact('anthracite', {'item': 'minecraft:coal'}, 'rotarycraft:anthracite', 2, RP, RT, 1)
compact('anthracite_from_charcoal', {'item': 'minecraft:charcoal'}, 'rotarycraft:anthracite', 3, RP, RT, 1)
compact('prismane', {'item': 'rotarycraft:anthracite'}, 'rotarycraft:prismane', 2, RP, RT, 2)
compact('lonsdaleite', {'item': 'rotarycraft:prismane'}, 'rotarycraft:lonsdaleite', 2, RP, RT, 3)
compact('diamond', {'item': 'rotarycraft:lonsdaleite'}, 'minecraft:diamond', 2, RP, RT, 4)
compact('glowstone', {'item': 'minecraft:blaze_powder'}, 'minecraft:glowstone', 1, 2000, 600)
compact('packed_ice', {'item': 'minecraft:ice'}, 'minecraft:packed_ice', 2, 24000, -80)


def shaped(name, pattern, key, count=1, result=None):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
      'result': {'id': result or 'rotarycraft:' + name, 'count': count}})


shaped('anthracite_block', ['BBB', 'BBB', 'BBB'], {'B': {'item': 'rotarycraft:anthracite'}})
shaped('lonsdaleite_block', ['BBB', 'BBB', 'BBB'], {'B': {'item': 'rotarycraft:lonsdaleite'}})
w('%s/recipe/anthracite_from_block.json' % D, {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
  'ingredients': [{'item': 'rotarycraft:anthracite_block'}], 'result': {'id': 'rotarycraft:anthracite', 'count': 9}})
w('%s/recipe/lonsdaleite_from_block.json' % D, {'type': 'minecraft:crafting_shapeless', 'category': 'misc',
  'ingredients': [{'item': 'rotarycraft:lonsdaleite_block'}], 'result': {'id': 'rotarycraft:lonsdaleite', 'count': 9}})
shaped('press_head', ['SOD', 'ODB', 'DBB'], {'S': {'tag': 'c:ingots/steel'}, 'D': {'tag': 'c:gems/diamond'}, 'O': {'tag': 'c:obsidians'},
       'B': {'item': 'rotarycraft:bedrock_dust'}})
shaped('compactor', ['SPS', 'PGP', '#P#'], {'#': {'item': 'rotarycraft:base_panel'}, 'S': {'tag': 'c:ingots/steel'}, 'P': {'item': 'rotarycraft:press_head'},
       'G': {'item': 'rotarycraft:tungsten_gear_unit_16'}})
w(R + '/data/c/tags/item/storage_blocks/anthracite.json', {'values': ['rotarycraft:anthracite_block']})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.compactor': 'Compactor',
    'block.rotarycraft.anthracite_block': 'Block of Anthracite',
    'block.rotarycraft.lonsdaleite_block': 'Block of Lonsdaleite',
    'item.rotarycraft.anthracite': 'Anthracite',
    'item.rotarycraft.prismane': 'Prismane',
    'item.rotarycraft.lonsdaleite': 'Lonsdaleite',
    'item.rotarycraft.press_head': 'Press Head',
    'gui.rotarycraft.compactor.needs': 'Coal stages need 550 MPa and 800 C',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('compactor ok')
