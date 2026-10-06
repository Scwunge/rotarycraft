"""Composter assets and recipes (run by gen_assets.py). Values are the original's (TileEntityComposter.CompostMatter)."""
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


def compost(name, value, *ingredients):
    w('%s/recipe/composting/%s.json' % (D, name), {'type': 'rotarycraft:composting', 'value': value,
      'ingredient': ingredients[0] if len(ingredients) == 1 else list(ingredients)})


def i(item):
    return {'item': item if ':' in item else 'minecraft:' + item}


def t(tag):
    return {'tag': tag}


compost('crap', 1, i('egg'), i('cookie'), i('wheat'), i('rotarycraft:canola_seeds'), i('rotarycraft:canola_husks'))
compost('sugarcane', 2, i('sugar_cane'))
compost('plants', 1, t('minecraft:saplings'), i('lily_pad'), t('minecraft:small_flowers'), i('brown_mushroom'), i('red_mushroom'))
compost('leaves', 2, t('minecraft:leaves'), i('grass_block'), i('vine'), i('short_grass'), i('tall_grass'), i('fern'), i('large_fern'))
compost('meat', 4, i('beef'), i('cooked_beef'), i('cooked_porkchop'), i('porkchop'), i('cooked_chicken'), i('chicken'))
compost('fish', 3, i('cooked_cod'), i('cod'), i('cooked_salmon'), i('salmon'))
compost('veggies', 2, i('potato'), i('carrot'), i('baked_potato'), i('poisonous_potato'), i('bread'), i('apple'), i('melon_slice'))
compost('mobs', 3, i('rotten_flesh'), i('spider_eye'))

shutil.copy('reference/RotaryCraft/Textures/GUI/compostergui.png', T + '/gui/composter.png')
shutil.copy('reference/RotaryCraft/Textures/TileEntityTex/compostertex.png', T + '/machine/composter.png')
w(A + '/blockstates/composter.json', {'variants': {'': {'model': 'rotarycraft:block/composter'}}})
w(A + '/models/block/composter.json', {'textures': {'particle': 'rotarycraft:block/shaft_steel'}})
w(A + '/models/item/composter.json', json.load(open(A + '/models/item/railgun.json')))
w(D + '/loot_table/blocks/composter.json', {'type': 'minecraft:block', 'pools': [{
    'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:composter'}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
if 'rotarycraft:composter' not in data['values']:
    data['values'].append('rotarycraft:composter')
w(path, data)
w(D + '/recipe/composter.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' S ', 'S S', 'BBB'],
  'key': {'S': {'tag': 'c:ingots/steel'}, 'B': {'item': 'rotarycraft:base_panel'}}, 'result': {'id': 'rotarycraft:composter', 'count': 1}})
w(D + '/recipe/composter_from_tin.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' S ', 'S S', 'BBB'],
  'key': {'S': {'tag': 'c:ingots/tin'}, 'B': {'item': 'rotarycraft:base_panel'}}, 'result': {'id': 'rotarycraft:composter', 'count': 1}})

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({'block.rotarycraft.composter': 'Composter', 'gui.rotarycraft.composter.temperature': '%s C (works from 40 to 70 C, with yeast)'})
w(lang_path, lang)
print('composter ok')
