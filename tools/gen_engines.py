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

# AC Engine: copper coils around a core; Magnetizer: a core between two coils
im, g = panel()
g.ellipse([3, 3, 12, 12], fill=(184, 115, 51, 255), outline=(110, 60, 25, 255))
g.ellipse([6, 6, 9, 9], fill=(170, 180, 192, 255))
im.save(T + '/block/ac_engine_front.png')
im, g = panel()
for x0 in (2, 10):
    g.rectangle([x0, 3, x0 + 3, 12], fill=(184, 115, 51, 255), outline=(110, 60, 25, 255))
g.rectangle([6, 6, 9, 9], fill=(170, 180, 192, 255), outline=(60, 62, 70, 255))
im.save(T + '/block/magnetizer_front.png')

for name, c in (('shaft_core', (170, 180, 192)), ('tungsten_shaft_core', (90, 95, 105))):
    im = Image.new('RGBA', (16, 16))
    g = ImageDraw.Draw(im)
    for i in range(10):
        g.rectangle([3 + i, 11 - i, 4 + i, 12 - i], fill=shade(c, rnd.randint(-12, 12)))
    g.ellipse([2, 10, 6, 14], fill=(110, 60, 25, 255))
    g.ellipse([10, 2, 14, 6], fill=(110, 60, 25, 255))
    im.save(T + '/item/%s.png' % name)
    w('%s/models/item/%s.json' % (A, name), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + name}})

import shutil
shutil.copy('reference/RotaryCraft/Textures/GUI/basic_gui_oneslot.png', T + '/gui/one_slot.png')

def shaped(name, pattern, key):
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': pattern, 'key': key,
      'result': {'id': 'rotarycraft:' + name, 'count': 1}})

shaped('shaft_core', ['  s', ' S ', 's  '], {'S': {'tag': 'c:ingots/steel'}, 's': {'item': 'rotarycraft:shaft_steel'}})
shaped('tungsten_shaft_core', ['  s', ' S ', 's  '], {'S': {'item': 'rotarycraft:tungsten_ingot'}, 's': {'item': 'rotarycraft:shaft_steel'}})
shaped('ac_engine', ['GGG', 'GCs', 'PRP'], {'G': {'tag': 'c:ingots/gold'}, 'C': {'tag': 'c:storage_blocks/copper'},
       's': {'item': 'rotarycraft:shaft_steel'}, 'P': {'tag': 'c:ingots/steel'}, 'R': {'tag': 'c:dusts/redstone'}})
shaped('magnetizer', ['P P', 'CMC', 'PRP'], {'P': {'tag': 'c:ingots/steel'}, 'C': {'tag': 'c:storage_blocks/copper'},
       'M': {'item': 'rotarycraft:shaft_steel'}, 'R': {'tag': 'c:dusts/redstone'}})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.gas_engine': 'Gas Engine',
    'gui.rotarycraft.fuel': 'Fuel: %s / %s mB',
    'block.rotarycraft.ac_engine': 'AC Electric Engine',
    'block.rotarycraft.magnetizer': 'Magnetizer',
    'item.rotarycraft.shaft_core': 'Shaft Core',
    'item.rotarycraft.tungsten_shaft_core': 'Tungsten Shaft Core',
    'item.rotarycraft.shaft_core.magnetized': 'Magnetized: %s uT',
    'item.rotarycraft.shaft_core.unmagnetized': 'Not magnetized',
    'gui.rotarycraft.magnetization': 'Core: %s uT',
    'gui.rotarycraft.ac_on': 'AC signal',
    'gui.rotarycraft.ac_off': 'No AC signal',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('engines ok')
