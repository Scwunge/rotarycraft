"""Gearboxes in six materials: block states, models, front textures, loot (keeping wear, lubricant and bearing), tags and
names (run by gen_assets.py, before gen_parts.py writes their recipes)."""
import json
import math
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures/block'
rnd = random.Random(83)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def shade(c, d=0):
    return tuple(max(0, min(255, x + d)) for x in c[:3]) + (255,)


MATS = {'wood': (150, 108, 60), 'stone': (128, 128, 128), 'steel': (170, 180, 192), 'tungsten': (90, 95, 105),
        'diamond': (90, 220, 215), 'bedrock': (60, 60, 60)}
NAMES = {'wood': 'Wooden', 'stone': 'Stone', 'steel': 'Steel', 'tungsten': 'Tungsten', 'diamond': 'Diamond', 'bedrock': 'Bedrock'}
RATIOS = (2, 4, 8, 16)
ROT = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}

# remove the old single-material gearbox files
for r in RATIOS:
    for path in ('%s/blockstates/gearbox_%dx.json' % (A, r), '%s/models/block/gearbox_%dx.json' % (A, r), '%s/models/item/gearbox_%dx.json' % (A, r),
                 '%s/loot_table/blocks/gearbox_%dx.json' % (D, r), '%s/recipe/gearbox_%dx.json' % (D, r)):
        if os.path.exists(path):
            os.remove(path)

ids = []
for m, c in MATS.items():
    # a casing of the material with two meshing gears
    im = Image.new('RGBA', (16, 16))
    p = im.load()
    for x in range(16):
        for y in range(16):
            p[x, y] = shade(c, rnd.randint(-8, 8))
    g = ImageDraw.Draw(im)
    g.rectangle([0, 0, 15, 15], outline=shade(c, -50))
    for cx, cy in ((5.5, 6.5), (10.5, 9.5)):
        for i in range(6):
            a = i * math.pi / 3
            x, y = cx + 4 * math.cos(a), cy + 4 * math.sin(a)
            g.rectangle([x - 0.5, y - 0.5, x + 0.5, y + 0.5], fill=shade(c, 40))
        g.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=shade(c, 25), outline=shade(c, -60))
        g.point((round(cx), round(cy)), fill=(30, 30, 30, 255))
    im.save('%s/gearbox_%s_front.png' % (T, m))
    for r in RATIOS:
        b = 'gearbox_%s_%dx' % (m, r)
        ids.append(b)
        variants = {}
        for facing, rot in ROT.items():
            v = {'model': 'rotarycraft:block/' + b}
            v.update(rot)
            variants['facing=' + facing] = v
        w('%s/blockstates/%s.json' % (A, b), {'variants': variants})
        side = 'rotarycraft:block/machine_side'
        w('%s/models/block/%s.json' % (A, b), {'parent': 'minecraft:block/cube', 'textures': {
            'north': 'rotarycraft:block/gearbox_%s_front' % m, 'south': 'rotarycraft:block/machine_back', 'east': side, 'west': side,
            'up': side, 'down': side, 'particle': 'rotarycraft:block/gearbox_%s_front' % m}})
        w('%s/models/item/%s.json' % (A, b), {'parent': 'rotarycraft:block/' + b})
        w('%s/loot_table/blocks/%s.json' % (D, b), {'type': 'minecraft:block', 'pools': [{
            'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + b, 'functions': [
                {'function': 'minecraft:copy_components', 'source': 'block_entity', 'include': ['rotarycraft:gearbox_state']}]}],
            'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def add_tag(path, values):
    data = json.load(open(path)) if os.path.exists(path) else {'values': []}
    data['values'] = [v for v in data['values'] if 'rotarycraft:gearbox_' not in v or v in values] + [v for v in values if v not in data['values']]
    w(path, data)


add_tag(R + '/data/minecraft/tags/block/mineable/pickaxe.json', ['rotarycraft:' + b for b in ids if '_wood_' not in b])
add_tag(R + '/data/minecraft/tags/block/mineable/axe.json', ['rotarycraft:' + b for b in ids if '_wood_' in b])
add_tag(R + '/data/minecraft/tags/block/needs_diamond_tool.json', ['rotarycraft:' + b for b in ids if '_bedrock_' in b])

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
for k in [k for k in lang if k.startswith('block.rotarycraft.gearbox_') and k[len('block.rotarycraft.gearbox_')].isdigit()]:
    del lang[k]
for m in MATS:
    for r in RATIOS:
        lang['block.rotarycraft.gearbox_%s_%dx' % (m, r)] = '%s %d:1 Gearbox' % (NAMES[m], r)
lang.update({
    'message.rotarycraft.gearbox.status': 'wear %s%%, lubricant %s/%s mB',
    'message.rotarycraft.gearbox.status_dry': 'wear %s%%',
    'item.rotarycraft.gearbox.state': 'Wear %s%%, lubricant %s mB',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('gearboxes ok')
