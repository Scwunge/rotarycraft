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

mg.finish()
