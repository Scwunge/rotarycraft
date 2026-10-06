"""Canola crop, seeds, husks and lubricant data (run by gen_assets.py)."""
import json
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'
rnd = random.Random(79)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


GROWN = 9
# crop stages: stalks rise with age, buds then yellow flowers on the last stages (canola blooms yellow)
variants = {}
for age in range(GROWN + 1):
    im = Image.new('RGBA', (16, 16))
    g = ImageDraw.Draw(im)
    top = 15 - max(2, round(14 * (age + 1) / (GROWN + 1)))
    for x in (2, 5, 8, 11, 14):
        h = top + rnd.randint(0, 2)
        g.line([(x, 15), (x + rnd.choice((-1, 0, 1)), h)], fill=(70 + rnd.randint(-10, 10), 140 + rnd.randint(-15, 15), 50, 255))
        if age >= 4:
            g.point((x - 1, h + 3), fill=(80, 160, 60, 255))
        if age >= 7:
            c = (240, 220, 40, 255) if age >= 8 else (170, 200, 70, 255)
            g.rectangle([x - 1, h - 1, x, h], fill=c)
    name = 'canola_stage%d' % age
    im.save('%s/block/%s.png' % (T, name))
    w('%s/models/block/%s.json' % (A, name), {'parent': 'minecraft:block/crop', 'textures': {'crop': 'rotarycraft:block/' + name},
                                              'render_type': 'minecraft:cutout'})
    variants['age=%d' % age] = {'model': 'rotarycraft:block/' + name}
w('%s/blockstates/canola.json' % A, {'variants': variants})

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
for _ in range(9):
    x, y = rnd.randint(4, 11), rnd.randint(5, 12)
    g.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(40 + rnd.randint(0, 20), 30, 25, 255))
im.save(T + '/item/canola_seeds.png')
im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
for _ in range(10):
    x, y = rnd.randint(3, 12), rnd.randint(4, 12)
    g.line([(x, y), (x + rnd.randint(-2, 2), y + rnd.randint(1, 3))], fill=(200 + rnd.randint(-20, 20), 180, 110, 255), width=2)
im.save(T + '/item/canola_husks.png')
for it in ('canola_seeds', 'canola_husks'):
    w('%s/models/item/%s.json' % (A, it), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + it}})

# grass drops canola seeds: the original adds them to the grass seed pool at weight 2 against wheat's 10, so about one
# seed drop in six, and grass drops a seed one time in eight -> about 1 in 48
w(D + '/loot_modifiers/canola_from_grass.json', {
    'type': 'rotarycraft:add_item',
    'conditions': [
        {'condition': 'minecraft:any_of', 'terms': [
            {'condition': 'neoforge:loot_table_id', 'loot_table_id': 'minecraft:blocks/short_grass'},
            {'condition': 'neoforge:loot_table_id', 'loot_table_id': 'minecraft:blocks/tall_grass'}]},
        {'condition': 'minecraft:inverted', 'term': {'condition': 'minecraft:match_tool', 'predicate': {'items': 'minecraft:shears'}}},
        {'condition': 'minecraft:random_chance', 'chance': 1 / 48},
    ],
    'item': {'id': 'rotarycraft:canola_seeds', 'count': 1}})
glm = R + '/data/neoforge/loot_modifiers/global_loot_modifiers.json'
w(glm, {'replace': False, 'entries': ['rotarycraft:canola_from_grass']})

w(D + '/recipe/grinding/canola_seeds.json', {'type': 'rotarycraft:grinding', 'ingredient': {'item': 'rotarycraft:canola_seeds'},
                                              'result': {'id': 'rotarycraft:canola_husks', 'count': 1}})
# husks -> lubricant: 0.75 x the average medium-difficulty canola amount (112), rounded up to 10 = 90 mB
w(D + '/recipe/centrifuge/canola_husks.json', {'type': 'rotarycraft:centrifuge', 'ingredient': {'item': 'rotarycraft:canola_husks'},
                                                'outputs': [], 'fluid': {'id': 'rotarycraft:lubricant', 'amount': 90}})
w(R + '/data/minecraft/tags/block/crops.json', {'replace': False, 'values': ['rotarycraft:canola']})
w(R + '/data/minecraft/tags/block/maintains_farmland.json', {'replace': False, 'values': ['rotarycraft:canola']})
w(R + '/data/c/tags/item/seeds.json', {'replace': False, 'values': ['rotarycraft:canola_seeds']})
w(R + '/data/minecraft/tags/item/villager_plantable_seeds.json', {'replace': False, 'values': ['rotarycraft:canola_seeds']})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.canola': 'Canola',
    'item.rotarycraft.canola_seeds': 'Canola Seeds',
    'item.rotarycraft.canola_husks': 'Canola Husks',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('canola ok')
