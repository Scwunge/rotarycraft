"""The HSLA steel and bedrock tools and armour (run by gen_assets.py): sprites cut from the original's item sheet, models, armour layers, recipes and lang.
Add a tool by appending to TOOLS."""
import json
import os
import shutil

from PIL import Image

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
REF = 'reference/RotaryCraft/Textures'

SHEET = Image.open(REF + '/Items/items2.png').convert('RGBA')


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def sprite(index, name):
    cols = SHEET.size[0] // 16
    x, y = (index % cols) * 16, (index // cols) * 16
    os.makedirs(A + '/textures/item', exist_ok=True)
    SHEET.crop((x, y, x + 16, y + 16)).save('%s/textures/item/%s.png' % (A, name))


def model(name, parent='minecraft:item/handheld'):
    w('%s/models/item/%s.json' % (A, name), {'parent': parent, 'textures': {'layer0': 'rotarycraft:item/' + name}})


STEEL = {'tag': 'c:ingots/steel'}
STICK = {'item': 'minecraft:stick'}

# name, sprite index, display name, recipe pattern, key, parent
TOOLS = [
    ('steel_pickaxe', 13, 'HSLA Steel Pickaxe', ['BBB', ' S ', ' S '], 'handheld'),
    ('steel_axe', 14, 'HSLA Steel Axe', ['BB ', 'BS ', ' S '], 'handheld'),
    ('steel_shovel', 15, 'HSLA Steel Shovel', [' B ', ' S ', ' S '], 'handheld'),
    ('steel_hoe', 22, 'HSLA Steel Hoe', ['BB ', ' S ', ' S '], 'handheld'),
    ('steel_sword', 24, 'HSLA Steel Sword', ['B', 'B', 'S'], 'handheld'),
    ('steel_shears', 26, 'HSLA Steel Shears', [' B', 'B '], 'handheld'),
    ('steel_sickle', 35, 'HSLA Steel Sickle', [' B ', '  B', 'SB '], 'handheld'),
    ('steel_helmet', 17, 'HSLA Steel Helmet', ['BBB', 'B B'], 'generated'),
    ('steel_chestplate', 18, 'HSLA Steel Chestplate', ['B B', 'BBB', 'BBB'], 'generated'),
    ('steel_leggings', 19, 'HSLA Steel Leggings', ['BBB', 'B B', 'B B'], 'generated'),
    ('steel_boots', 20, 'HSLA Steel Boots', ['B B', 'B B'], 'generated'),
]

lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
for name, index, title, pattern, parent in TOOLS:
    sprite(index, name)
    model(name, 'minecraft:item/' + parent)
    key = {'B': STEEL}
    if any('S' in row for row in pattern):
        key['S'] = STICK
    w('%s/recipe/%s.json' % (D, name), {'type': 'minecraft:crafting_shaped', 'category': 'equipment', 'pattern': pattern, 'key': key,
                                       'result': {'id': 'rotarycraft:' + name, 'count': 1}})
    lang['item.rotarycraft.' + name] = title

# the armour's worn look: the original's two layers
os.makedirs(A + '/textures/models/armor', exist_ok=True)
shutil.copy(REF + '/Misc/steel_1.png', A + '/textures/models/armor/steel_layer_1.png')
shutil.copy(REF + '/Misc/steel_2.png', A + '/textures/models/armor/steel_layer_2.png')
w(A + '/lang/en_us.json', lang)
