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

mg.finish()
