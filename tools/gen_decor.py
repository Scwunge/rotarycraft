"""World and decoration machines and the extra cannons (run by gen_assets.py): the original's model textures, GUI pictures, blockstates, loot, recipes
and lang. Written with the helpers in machinegen.py."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from machinegen import *  # noqa: E402,F401,F403
import machinegen as mg  # noqa: E402

# ---- Obsidian Maker ----
rendered_machine('obsidian_maker', model='ModelObsidian', texture='obsidiantex.png')
gui('obsidiangui.png', 'obsidian_maker')
shaped('obsidian_maker', ['SpS', 'PMP', 'BBB'], {'S': STEEL, 'p': vanilla('glass_pane'), 'P': item('pipe'), 'M': item('mixer'), 'B': item('base_panel')})
names({'block.rotarycraft.obsidian_maker': 'Obsidian Maker'})

# ---- Line Builder ----
rendered_machine('line_builder', model='ModelRam', texture='ramtex.png')
gui('basicstorage.png', 'line_builder')
shaped('line_builder', ['sbs', 'sps', 'PgP'], {'s': STEEL, 'b': BEDROCK_INGOT, 'p': vanilla('piston'), 'g': GEAR2, 'P': BASEPANEL})
names({'block.rotarycraft.line_builder': 'Line Builder'})

# ---- Block Filler and Spiller (plain steel cubes with their mark on the underside) ----


def filler_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.rectangle([3, 3, 12, 12], fill=(60, 62, 70, 255), outline=(150, 154, 162, 255))
    g.polygon([(4, 4), (11, 4), (9, 10), (6, 10)], fill=(196, 160, 90, 255))
    g.rectangle([7, 10, 8, 13], fill=(150, 154, 162, 255))


def spiller_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.ellipse([2, 2, 13, 13], fill=(40, 90, 170, 255), outline=(150, 154, 162, 255))
    g.polygon([(7, 4), (10, 9), (7, 11), (5, 9)], fill=(150, 200, 255, 255))


drawn_machine('block_filler', filler_mark)
gui_copy('one_slot', 'block_filler')
shaped('block_filler', ['sps', 'phP', 'sgs'], {'s': STEEL, 'p': PIPE, 'h': vanilla('hopper'), 'P': BASEPANEL, 'g': GEAR2})
drawn_machine('spiller', spiller_mark)
shaped('spiller', ['sSs', 'PpP', 'sgs'], {'s': STEEL, 'S': item('pipe'), 'P': BASEPANEL, 'p': vanilla('bucket'), 'g': GEAR2})
names({'block.rotarycraft.block_filler': 'Block Filler', 'block.rotarycraft.spiller': 'Spiller'})

mg.finish()
