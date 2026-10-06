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


# Performance Engine: red cylinders and a blue water jacket
im, g = panel()
g.rectangle([1, 2, 14, 13], fill=(60, 90, 140, 255), outline=(30, 45, 80, 255))
for x0 in (3, 9):
    g.rectangle([x0, 4, x0 + 3, 11], fill=(190, 50, 50, 255), outline=(90, 20, 20, 255))
im.save(T + '/block/performance_engine_front.png')
shutil.copy('reference/RotaryCraft/Textures/GUI/perfgui.png', T + '/gui/performance_engine.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
for _ in range(7):
    x, y = rnd.randint(2, 11), rnd.randint(3, 11)
    g.polygon([(x, y), (x + rnd.randint(2, 4), y + rnd.randint(-1, 2)), (x + rnd.randint(0, 3), y + rnd.randint(2, 4))],
              fill=shade((150, 155, 165), rnd.randint(-30, 20)))
im.save(T + '/item/scrap.png')
w('%s/models/item/scrap.json' % A, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/scrap'}})


# Microturbine: an intake with turbine blades
im, g = panel()
g.ellipse([2, 2, 13, 13], fill=(40, 42, 48, 255), outline=(150, 155, 165, 255))
for i in range(8):
    import math
    a = i * math.pi / 4
    g.line([(7.5, 7.5), (7.5 + 5 * math.cos(a), 7.5 + 5 * math.sin(a))], fill=(170, 180, 192, 255))
im.save(T + '/block/microturbine_front.png')
shutil.copy('reference/RotaryCraft/Textures/GUI/jetgui.png', T + '/gui/turbine.png')

# Jet Engine: a dark intake ring around compressor blades
im, g = panel()
g.ellipse([1, 1, 14, 14], fill=(30, 30, 34, 255), outline=(170, 180, 192, 255))
for i in range(12):
    a = i * math.pi / 6
    g.line([(7.5, 7.5), (7.5 + 6 * math.cos(a), 7.5 + 6 * math.sin(a))], fill=(140, 145, 155, 255))
g.ellipse([5, 5, 10, 10], fill=(200, 60, 40, 255))
im.save(T + '/block/jet_engine_front.png')
im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.rectangle([2, 4, 13, 11], fill=(90, 95, 105, 255), outline=(40, 42, 48, 255))
g.polygon([(13, 5), (15, 7), (15, 8), (13, 10)], fill=(255, 140, 30, 255))
g.rectangle([4, 6, 10, 9], fill=(200, 60, 40, 255))
im.save(A + '/textures/item/afterburner_upgrade.png')
w('%s/models/item/afterburner_upgrade.json' % A, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/afterburner_upgrade'}})
w(D + '/damage_type/jet_ingest.json', {'message_id': 'rotarycraft.jet_ingest', 'scaling': 'never', 'exhaustion': 0.0})

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
    'block.rotarycraft.performance_engine': 'Performance Engine',
    'item.rotarycraft.scrap': 'Scrap',
    'gui.rotarycraft.additives': 'Additives: %s / %s',
    'gui.rotarycraft.performance_engine.limit': 'Explodes above %s C; water cools it',
    'block.rotarycraft.microturbine': 'Microturbine',
    'block.rotarycraft.jet_engine': 'Jet Engine',
    'item.rotarycraft.afterburner_upgrade': 'Afterburner Upgrade',
    'gui.rotarycraft.afterburner_on': 'AB on',
    'gui.rotarycraft.afterburner_off': 'AB off',
    'gui.rotarycraft.afterburner': 'Afterburner: twice the torque, 2.5x the fuel, more heat',
    'gui.rotarycraft.jet.fod': 'Damage: %s/8',
    'death.attack.rotarycraft.jet_ingest': '%1$s was sucked into a jet engine',
    'death.attack.rotarycraft.jet_ingest.player': '%1$s was sucked into a jet engine',
})
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('engines ok')
