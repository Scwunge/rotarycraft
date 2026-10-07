"""Automation and processing machines (run by gen_assets.py): the original's model textures, GUI pictures, blockstates, loot, recipes and lang. Written with the
helpers in machinegen.py."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from machinegen import *  # noqa: E402,F401,F403
import machinegen as mg  # noqa: E402

# ---- Player Detector ----
rendered_machine('player_detector', model='ModelDetector', texture='detectortex.png')
shaped('player_detector', ['LRL', 'OGO', 'OPO'], {'L': vanilla('lapis_lazuli'), 'R': item('radar_unit'), 'O': vanilla('obsidian'), 'P': BASEPANEL, 'G': vanilla('gold_ingot')})
names({'block.rotarycraft.player_detector': 'Player Detector'})

# ---- Smoke Detector ----
rendered_machine('smoke_detector', model='ModelSmokeDetector', texture='smokedetectortex.png')
gui_copy('one_slot', 'smoke_detector')
shaped('smoke_detector', [' S ', 'RRR', ' N '], {'S': vanilla('stone_slab'), 'R': vanilla('redstone'), 'N': vanilla('note_block')})
names({'block.rotarycraft.smoke_detector': 'Smoke Detector'})

# ---- Heater ----
rendered_machine('heater', model='ModelHeater', texture=None)
for _i, _t in enumerate(['heatertex.png', 'heatertex200C.png', 'heatertex400C.png', 'heatertex600C.png', 'heatertex800C.png', 'heatertex900C.png']):
    model_texture('Heater/' + _t, 'heater_%d' % _i)
gui('heatergui.png', 'heater')
shaped('heater', ['sBs', 'prp', 'scs'], {'s': STEEL, 'B': vanilla('iron_bars'), 'p': BASEPANEL, 'r': TUNGSTEN, 'c': item('combustor')})
names({'block.rotarycraft.heater': 'Heater', 'gui.rotarycraft.heater.temperature': 'Temperature Control:'})

# ---- Igniter (a plain cube) ----


def igniter_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.rectangle([3, 3, 12, 12], fill=(60, 30, 20, 255), outline=(150, 154, 162, 255))
    g.polygon([(8, 4), (11, 9), (9, 12), (6, 12), (5, 9)], fill=(255, 150, 40, 255))
    g.polygon([(8, 7), (9, 10), (8, 12), (7, 10)], fill=(255, 230, 120, 255))


drawn_machine('firestarter', igniter_mark, face='up')
gui('heatergui.png', 'firestarter')
shaped('firestarter', ['OGO', 'GCG', 'OGO'], {'O': vanilla('obsidian'), 'G': vanilla('gold_ingot'), 'C': item('combustor')})
names({'block.rotarycraft.firestarter': 'Igniter'})

# ---- Item Refresher (a plain cube) ----


def refresher_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.rectangle([3, 3, 12, 12], fill=(30, 40, 70, 255), outline=(150, 154, 162, 255))
    g.ellipse([5, 5, 10, 10], outline=(180, 120, 255, 255))
    g.point((8, 7), fill=(255, 255, 255, 255))


drawn_machine('refresher', refresher_mark, face='up')
shaped('refresher', ['ses', 'epe', 'ses'], {'s': STEEL, 'p': vanilla('ender_pearl'), 'e': vanilla('lapis_lazuli')})
names({'block.rotarycraft.refresher': 'Item Refresher'})

# ---- Wetter, and what it does to things ----
rendered_machine('wetter', model='ModelWetter', texture='wettertex.png')
gui_copy('one_slot', 'wetter')
shaped('wetter', ['S S', 'gmg', 'SPS'], {'S': STEEL, 'g': vanilla('glass_pane'), 'm': MIXER, 'P': BASEPANEL})
names({'block.rotarycraft.wetter': 'Wetter'})
w('%s/recipe/wetting/lubricant_sand.json' % D, {'type': 'rotarycraft:wetting', 'ingredient': {'item': 'minecraft:sand'}, 'fluid': {'fluid': 'rotarycraft:lubricant', 'amount': 500},
                                                'result': {'id': 'minecraft:soul_sand', 'count': 1}, 'duration': 200})
w('%s/recipe/wetting/oil_sand.json' % D, {'type': 'rotarycraft:wetting', 'ingredient': {'item': 'minecraft:sand'}, 'fluid': {'tag': 'c:crude_oil', 'amount': 125},
                                          'result': {'id': 'minecraft:soul_sand', 'count': 1}, 'duration': 50})
w('%s/recipe/wetting/jet_fuel_cobblestone.json' % D, {'type': 'rotarycraft:wetting', 'ingredient': {'item': 'minecraft:cobblestone'}, 'fluid': {'fluid': 'rotarycraft:jet_fuel', 'amount': 20},
                                                      'result': {'id': 'minecraft:netherrack', 'count': 1}, 'duration': 80})
w('%s/recipe/wetting/ender_cobblestone.json' % D, {'type': 'rotarycraft:wetting', 'ingredient': {'item': 'minecraft:cobblestone'}, 'fluid': {'tag': 'c:ender', 'amount': 50},
                                                   'result': {'id': 'minecraft:end_stone', 'count': 1}, 'duration': 80})

# ---- Aggregator ----
rendered_machine('aggregator', model='ModelAggregator', texture='aggregatortex.png')
shaped('aggregator', ['SPS', 'GCG', 'SsS'], {'S': STEEL, 'P': BASEPANEL, 'G': vanilla('glass_pane'), 'C': COMPRESSOR, 's': SHAFT})
names({'block.rotarycraft.aggregator': 'Aggregator'})

# ---- Grindstone ----
rendered_machine('grindstone', model='ModelGrindstone', texture='grindstonetex.png')
gui_copy('one_slot', 'grindstone')
shaped('grindstone', ['S S', 'sBs', 'ppp'], {'S': STEEL, 's': SHAFT, 'B': vanilla('stone'), 'p': BASEPANEL})
names({'block.rotarycraft.grindstone': 'Grindstone'})

# ---- Purifier (a plain cube) ----


def purifier_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.rectangle([3, 3, 12, 12], fill=(70, 70, 76, 255), outline=(150, 154, 162, 255))
    for x in (5, 7, 9, 11):
        g.line([(x, 4), (x, 11)], fill=(170, 175, 185, 255))


drawn_machine('purifier', purifier_mark, face='up')
gui('purifiergui.png', 'purifier')
shaped('purifier', ['sbs', 'prp', 'sps'], {'s': STEEL, 'b': vanilla('iron_bars'), 'p': BASEPANEL, 'r': vanilla('redstone')})
names({'block.rotarycraft.purifier': 'Purifier'})

# ---- Bucket Filler (a plain cube) ----


def bucket_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.rectangle([3, 3, 12, 12], fill=(50, 56, 70, 255), outline=(150, 154, 162, 255))
    g.polygon([(5, 5), (11, 5), (10, 11), (6, 11)], fill=(180, 184, 192, 255))
    g.rectangle([6, 5, 10, 6], fill=(60, 120, 220, 255))


drawn_machine('bucket_filler', bucket_mark, face='up')
gui('basicstorage.png', 'bucket_filler')
shaped('bucket_filler', ['SPS', 'PCP', 'SPS'], {'S': STEEL, 'P': PIPE, 'C': vanilla('chest')})
names({'block.rotarycraft.bucket_filler': 'Bucket Filler', 'gui.rotarycraft.bucket_filler.button0.0': 'Filling', 'gui.rotarycraft.bucket_filler.button0.1': 'Emptying'})

# ---- Filling Station ----
rendered_machine('filling_station', model='ModelFillingStation', texture='fillingtex.png')
gui('fillingstationgui.png', 'filling_station')
shaped('filling_station', ['ppS', ' iR', 'ppB'], {'p': PIPE, 'S': STEEL, 'i': IMPELLER, 'R': item('reservoir'), 'B': BASEPANEL})
names({'block.rotarycraft.filling_station': 'Filling Station'})

# ---- Spillway ----
rendered_machine('spillway', model='ModelSpillway', texture='spillwaytex.png')
shaped('spillway', ['S  ', 'PSP', 'PpP'], {'S': STEEL, 'P': BASEPANEL, 'p': PIPE})
names({'block.rotarycraft.spillway': 'Spillway'})

mg.finish()
