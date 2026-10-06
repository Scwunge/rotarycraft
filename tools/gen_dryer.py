"""Dryer (the original's Drying Bed) assets, recipes and lang (run by gen_assets.py). Drying recipes are the original's
(RecipesDryingBed) where the fluid exists in this pack."""
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


def dry(name, fluid, amount, result, count=1):
    w('%s/recipe/drying/%s.json' % (D, name), {'type': 'rotarycraft:drying',
      'fluid': {'id': fluid, 'amount': amount}, 'result': {'id': result, 'count': count}})


dry('salt', 'minecraft:water', 250, 'rotarycraft:salt')
dry('gold_nugget', 'minecraft:lava', 1000, 'minecraft:gold_nugget')

shutil.copy('reference/RotaryCraft/Textures/GUI/drygui.png', T + '/gui/dryer.png')
shutil.copy('reference/RotaryCraft/Textures/TileEntityTex/dryingbedtex.png', T + '/machine/dryer.png')
w(A + '/blockstates/dryer.json', {'variants': {'': {'model': 'rotarycraft:block/dryer'}}})
w(A + '/models/block/dryer.json', {'textures': {'particle': 'rotarycraft:block/shaft_steel'}})
w(A + '/models/item/dryer.json', json.load(open(A + '/models/item/railgun.json')))
w(D + '/loot_table/blocks/dryer.json', {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:dryer'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
if 'rotarycraft:dryer' not in data['values']:
    data['values'].append('rotarycraft:dryer')
w(path, data)
w(D + '/recipe/dryer.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['S S', 'SPS', 'S S'],
  'key': {'S': {'tag': 'c:ingots/steel'}, 'P': {'item': 'rotarycraft:base_panel'}}, 'result': {'id': 'rotarycraft:dryer', 'count': 1}})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang['block.rotarycraft.dryer'] = 'Dryer'
w(lang_path, lang)
print('dryer ok')
