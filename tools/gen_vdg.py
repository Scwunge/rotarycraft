"""Van de Graaff Generator assets and recipe (run by gen_assets.py). The model is the original's (reika_models/van_de_graff.json)."""
import json
import os
import shutil

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


shutil.copy('reference/RotaryCraft/Textures/TileEntityTex/vandegrafftex.png', T + '/machine/van_de_graff.png')
w(A + '/blockstates/van_de_graaff.json', {'variants': {'': {'model': 'rotarycraft:block/van_de_graaff'}}})
w(A + '/models/block/van_de_graaff.json', {'textures': {'particle': 'rotarycraft:block/shaft_steel'}})
item = json.load(open(A + '/models/item/railgun.json'))
w(A + '/models/item/van_de_graaff.json', item)
w(D + '/loot_table/blocks/van_de_graaff.json', {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:van_de_graaff'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
if 'rotarycraft:van_de_graaff' not in data['values']:
    data['values'].append('rotarycraft:van_de_graaff')
w(path, data)
w(D + '/recipe/van_de_graaff.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['shs', 'gbg', 'php'],
  'key': {'h': {'item': 'rotarycraft:hub'}, 'p': {'item': 'rotarycraft:base_panel'}, 'b': {'item': 'rotarycraft:belt'},
          'g': {'item': 'minecraft:glass_pane'}, 's': {'tag': 'c:ingots/steel'}},
  'result': {'id': 'rotarycraft:van_de_graaff', 'count': 1}})
lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang['block.rotarycraft.van_de_graaff'] = 'Van de Graaff Generator'
w(lang_path, lang)
print('van de graaff ok')
