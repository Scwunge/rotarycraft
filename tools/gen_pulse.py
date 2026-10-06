"""Pulse Furnace, Blast Glass and the furnace's recipes (run by gen_assets.py). Recipes and temperatures are the original's
(RecipesPulseFurnace): metal items recycle at half their metal's melting point (clamped to 400-850 C)."""
import json
import os
import random
import shutil

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'
rnd = random.Random(71)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def pulse(name, item, result, count, temperature):
    r = {'type': 'rotarycraft:pulse_smelting', 'ingredient': {'item': item}, 'result': {'id': result, 'count': count}}
    if temperature != 600:
        r['temperature'] = temperature
    w('%s/recipe/pulse_smelting/%s.json' % (D, name), r)


IRON, GOLD, DIAMOND = 769, 532, 850

pulse('obsidian', 'minecraft:obsidian', 'rotarycraft:blast_glass', 1, 850)
pulse('iron_ingot', 'minecraft:iron_ingot', 'rotarycraft:hsla_steel_ingot', 2, 900)
pulse('red_gold_dust', 'rotarycraft:red_gold_dust', 'rotarycraft:red_gold_ingot', 1, 500)

# recycling: item, ingots back, metal temperature
ARMOUR = (('helmet', 5), ('chestplate', 8), ('leggings', 7), ('boots', 4))
TOOLS = (('hoe', 2), ('shovel', 1), ('axe', 3), ('pickaxe', 3), ('sword', 2))
for part, n in (('chainmail', None),):
    for piece, k in zip(('helmet', 'boots', 'leggings', 'chestplate'), (3, 2, 4, 5)):
        pulse('chainmail_' + piece, 'minecraft:chainmail_' + piece, 'minecraft:iron_ingot', k, IRON)
for piece, k in ARMOUR:
    pulse('iron_' + piece, 'minecraft:iron_' + piece, 'minecraft:iron_ingot', k, IRON)
    pulse('golden_' + piece, 'minecraft:golden_' + piece, 'minecraft:gold_ingot', k, GOLD)
    if piece == 'chestplate' or True:
        pulse('diamond_' + piece, 'minecraft:diamond_' + piece, 'minecraft:diamond', k, DIAMOND)
for tool, k in TOOLS:
    pulse('iron_' + tool, 'minecraft:iron_' + tool, 'minecraft:iron_ingot', k, IRON)
    pulse('golden_' + tool, 'minecraft:golden_' + tool, 'minecraft:gold_ingot', k, GOLD)
    pulse('diamond_' + tool, 'minecraft:diamond_' + tool, 'minecraft:diamond', k, DIAMOND)
for horse, ingot, t in (('iron', 'iron_ingot', IRON), ('diamond', 'diamond', DIAMOND), ('golden', 'gold_ingot', GOLD)):
    pulse(horse + '_horse_armor', 'minecraft:%s_horse_armor' % horse, 'minecraft:' + ingot, 7, t)
for name, k in (('flint_and_steel', 1), ('bucket', 3), ('water_bucket', 3), ('lava_bucket', 3), ('milk_bucket', 3), ('minecart', 5),
                ('iron_door', 6), ('cauldron', 7), ('hopper_minecart', 10)):
    pulse(name, 'minecraft:' + name, 'minecraft:iron_ingot', k, IRON)
pulse('detector_rail', 'minecraft:detector_rail', 'minecraft:iron_ingot', 1, IRON)
pulse('powered_rail', 'minecraft:powered_rail', 'minecraft:gold_ingot', 1, GOLD)

# crafting ("OCD", "PcO", "BBB"), the Pulse Furnace and its glass
w(D + '/recipe/pulse_furnace.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['OCD', 'PcO', 'BBB'],
  'key': {'O': {'item': 'minecraft:obsidian'}, 'C': {'item': 'rotarycraft:compressor'}, 'D': {'item': 'rotarycraft:diffuser'},
          'P': {'item': 'rotarycraft:pipe'}, 'c': {'item': 'rotarycraft:combustor'}, 'B': {'item': 'rotarycraft:base_panel'}},
  'result': {'id': 'rotarycraft:pulse_furnace', 'count': 1}})

# blast glass block ------------------------------------------------------------------------------------------------------
w(A + '/blockstates/blast_glass.json', {'variants': {'': {'model': 'rotarycraft:block/blast_glass'}}})
w(A + '/models/block/blast_glass.json', {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'rotarycraft:block/blast_glass'}})
w(A + '/models/item/blast_glass.json', {'parent': 'rotarycraft:block/blast_glass'})
w(D + '/loot_table/blocks/blast_glass.json', {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:blast_glass'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
for tagdir, tag in (('block', 'c:glass_blocks'), ('item', 'c:glass_blocks')):
    path = '%s/data/c/tags/%s/glass_blocks.json' % (R, tagdir)
    data = json.load(open(path)) if os.path.exists(path) else {'values': []}
    if 'rotarycraft:blast_glass' not in data['values']:
        data['values'].append('rotarycraft:blast_glass')
    w(path, data)
path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
if 'rotarycraft:blast_glass' not in data['values']:
    data['values'].append('rotarycraft:blast_glass')
w(path, data)


def shade(c, d):
    return tuple(max(0, min(255, x + d)) for x in c) + (255,)


im = Image.new('RGBA', (16, 16), (170, 215, 235, 90))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(60, 80, 100, 255))
g.rectangle([1, 1, 14, 14], outline=(120, 160, 185, 200))
for x, y in ((3, 3), (4, 3), (3, 4), (11, 10), (12, 11)):
    g.point((x, y), fill=(255, 255, 255, 220))
im.save(T + '/block/blast_glass.png')

# furnace --------------------------------------------------------------------------------------------------------------
shutil.copy('reference/RotaryCraft/Textures/GUI/pulsejetgui.png', T + '/gui/pulse_furnace.png')
im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        p[x, y] = shade((72, 76, 84), rnd.randint(-6, 6))
g = ImageDraw.Draw(im)
g.rectangle([0, 0, 15, 15], outline=(40, 42, 48, 255))
g.ellipse([3, 3, 12, 12], fill=(30, 30, 34, 255), outline=(150, 150, 160, 255))
g.ellipse([5, 5, 10, 10], fill=(240, 130, 40, 255))
g.point((7, 7), fill=(255, 235, 150, 255))
im.save(T + '/block/pulse_furnace_front.png')

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.pulse_furnace': 'Pulse Furnace',
    'block.rotarycraft.blast_glass': 'Blast Glass',
    'gui.rotarycraft.pulse_furnace.fuel': 'Jet fuel: %s / %s mB',
    'gui.rotarycraft.pulse_furnace.water': 'Water: %s / %s mB',
    'gui.rotarycraft.pulse_furnace.accelerant': 'Oxygen: %s / %s mB',
    'gui.rotarycraft.pulse_furnace.needs': 'Needs 1 N*m, 131072 rad/s and 1 W; smelts once hot enough',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('pulse furnace data ok')
