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

# the bedrock set, made in the blast furnace: blast_crafting recipes whose results come enchanted as they cannot be unenchanted
BEDROCK = {'type': 'item', 'item': 'rotarycraft:bedrock_ingot'}
SHAFT = {'item': 'rotarycraft:shaft_core'}
INGOT = {'item': 'rotarycraft:bedrock_ingot'}
ENCHANTED = {
    'bedrock_pickaxe': {'minecraft:silk_touch': 1, 'minecraft:fortune': 5},
    'bedrock_sword': {'minecraft:sharpness': 5, 'minecraft:looting': 5},
    'bedrock_sickle': {'minecraft:fortune': 5},
    'bedrock_helmet': {'minecraft:projectile_protection': 4, 'minecraft:respiration': 3},
    'bedrock_chestplate': {'minecraft:blast_protection': 4},
    'bedrock_leggings': {'minecraft:fire_protection': 4},
    'bedrock_boots': {'minecraft:feather_falling': 4},
}
BEDROCK_TOOLS = [
    ('bedrock_pickaxe', 101, 'Bedrock Pickaxe', ['BBB', ' S ', ' S '], 'handheld', 1000),
    ('bedrock_axe', 100, 'Bedrock Axe', ['BB ', 'BS ', ' S '], 'handheld', 1000),
    ('bedrock_shovel', 102, 'Bedrock Shovel', ['B', 'S', 'S'], 'handheld', 1000),
    ('bedrock_hoe', 21, 'Bedrock Hoe', ['BB', ' S', ' S'], 'handheld', 1000),
    ('bedrock_sword', 23, 'Bedrock Sword', ['B', 'B', 'S'], 'handheld', 1000),
    ('bedrock_shears', 25, 'Bedrock Shears', [' B', 'B '], 'handheld', 1000),
    ('bedrock_sickle', 36, 'Bedrock Sickle', [' B ', '  B', 'SB '], 'handheld', 1000),
    ('bedrock_helmet', 7, 'Bedrock Helmet', ['BBB', 'B B'], 'generated', 1200),
    ('bedrock_chestplate', 9, 'Bedrock Chestplate', ['B B', 'BBB', 'BBB'], 'generated', 1200),
    ('bedrock_leggings', 10, 'Bedrock Leggings', ['BBB', 'B B', 'B B'], 'generated', 1200),
    ('bedrock_boots', 8, 'Bedrock Boots', ['B B', 'B B'], 'generated', 1200),
]
for name, index, title, pattern, parent, temperature in BEDROCK_TOOLS:
    sprite(index, name)
    model(name, 'minecraft:item/' + parent)
    key = {'B': INGOT}
    if any('S' in row for row in pattern):
        key['S'] = SHAFT
    result = {'id': 'rotarycraft:' + name, 'count': 1}
    if name in ENCHANTED:
        result['components'] = {'minecraft:enchantments': {'levels': ENCHANTED[name]}}
    w('%s/recipe/blast_crafting/%s.json' % (D, name), {'type': 'rotarycraft:blast_crafting', 'pattern': pattern, 'key': key, 'result': result,
                                                       'temperature': temperature, 'speed': 4})
    lang['item.rotarycraft.' + name] = title
w(D + '/tags/block/incorrect_for_bedrock_tool.json', {'values': []})
lang['message.rotarycraft.bedrock_broke'] = 'The dulled tool has broken.'

# the armour's worn look: the original's two layers
os.makedirs(A + '/textures/models/armor', exist_ok=True)
shutil.copy(REF + '/Misc/steel_1.png', A + '/textures/models/armor/steel_layer_1.png')
shutil.copy(REF + '/Misc/steel_2.png', A + '/textures/models/armor/steel_layer_2.png')
shutil.copy(REF + '/Misc/bedrock_1.png', A + '/textures/models/armor/bedrock_layer_1.png')
shutil.copy(REF + '/Misc/bedrock_2.png', A + '/textures/models/armor/bedrock_layer_2.png')
w(A + '/lang/en_us.json', lang)
