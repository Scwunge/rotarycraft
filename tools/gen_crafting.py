"""Worktable, Auto-Crafter and Craft Pattern (run by gen_assets.py, after gen_decor.py): block textures drawn over the machine side, blockstates,
models, GUI textures from the original, the pattern's icon, loot, recipes and lang."""
import json
import os
import shutil

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
T = A + '/textures'
REF = 'reference/RotaryCraft'
BLOCK_TEX = T + '/block'


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def loot(name):
    w('%s/loot_table/blocks/%s.json' % (D, name), {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': 'rotarycraft:' + name}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


def item(i):
    return {'item': 'rotarycraft:' + i}


STEEL = {'tag': 'c:ingots/steel'}
side = Image.open(BLOCK_TEX + '/machine_side.png').convert('RGBA')
DARK = (52, 54, 60, 255)
LIGHT = (190, 194, 200, 255)
WOOD = (150, 108, 60, 255)


def grid(im, x0, y0, cell, gap, color):
    g = ImageDraw.Draw(im)
    for r in range(3):
        for c in range(3):
            x = x0 + c * (cell + gap)
            y = y0 + r * (cell + gap)
            g.rectangle([x, y, x + cell - 1, y + cell - 1], fill=color, outline=DARK)


# ---- Worktable: a plain cube, a crafting grid on top ----
top = side.copy()
grid(top, 2, 2, 4, 1, (110, 76, 40, 255))
top.save(BLOCK_TEX + '/worktable_top.png')
wside = side.copy()
g = ImageDraw.Draw(wside)
g.rectangle([0, 0, 15, 3], fill=WOOD, outline=DARK)
g.line([(2, 8), (13, 8)], fill=DARK)
g.rectangle([3, 10, 5, 13], outline=LIGHT)
g.rectangle([10, 10, 12, 13], outline=LIGHT)
wside.save(BLOCK_TEX + '/worktable_side.png')
w(A + '/models/block/worktable.json', {'parent': 'minecraft:block/cube_bottom_top', 'textures': {
    'top': 'rotarycraft:block/worktable_top', 'side': 'rotarycraft:block/worktable_side', 'bottom': 'rotarycraft:block/machine_side',
    'particle': 'rotarycraft:block/worktable_side'}})
w(A + '/blockstates/worktable.json', {'variants': {'': {'model': 'rotarycraft:block/worktable'}}})
w(A + '/models/item/worktable.json', {'parent': 'rotarycraft:block/worktable'})
loot('worktable')

# ---- Auto-Crafter: a machine with the recipe grid and an arrow on its front ----
front = side.copy()
grid(front, 2, 2, 3, 1, (110, 76, 40, 255))
g = ImageDraw.Draw(front)
g.line([(11, 6), (14, 6)], fill=(200, 60, 60, 255))
g.line([(12, 5), (14, 6), (12, 7)], fill=(200, 60, 60, 255))
g.rectangle([10, 10, 13, 13], fill=(90, 94, 102, 255), outline=LIGHT)
front.save(BLOCK_TEX + '/auto_crafter_front.png')
w(A + '/models/block/auto_crafter.json', {'parent': 'minecraft:block/cube', 'textures': {
    'north': 'rotarycraft:block/auto_crafter_front', 'south': 'rotarycraft:block/machine_back', 'east': 'rotarycraft:block/machine_side',
    'west': 'rotarycraft:block/machine_side', 'up': 'rotarycraft:block/machine_side', 'down': 'rotarycraft:block/machine_side',
    'particle': 'rotarycraft:block/machine_side'}})
rot = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270}, 'up': {'x': 270}, 'down': {'x': 90}}
w(A + '/blockstates/auto_crafter.json', {'variants': {'facing=' + f: dict({'model': 'rotarycraft:block/auto_crafter'}, **r) for f, r in rot.items()}})
w(A + '/models/item/auto_crafter.json', {'parent': 'rotarycraft:block/auto_crafter'})
loot('auto_crafter')

# ---- Craft Pattern: the original's sprite ----
sheet = Image.open(REF + '/Textures/Items/items.png').convert('RGBA')
index = 34
sheet.crop(((index % 16) * 16, (index // 16) * 16, (index % 16 + 1) * 16, (index // 16 + 1) * 16)).save(T + '/item/craft_pattern.png')
w(A + '/models/item/craft_pattern.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/craft_pattern'}})

# ---- screens ----
shutil.copy(REF + '/Textures/GUI/worktablegui2.png', T + '/gui/worktable.png')
shutil.copy(REF + '/Textures/GUI/craftergui2.png', T + '/gui/auto_crafter.png')
shutil.copy(REF + '/Textures/GUI/patterngui.png', T + '/gui/craft_pattern.png')

# ---- recipes ----
w(D + '/recipe/worktable.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['sCs', 'SBS', 'srs'], 'key': {
    's': {'item': 'minecraft:stone_slab'}, 'C': {'item': 'minecraft:crafting_table'}, 'S': STEEL, 'B': {'item': 'minecraft:bricks'},
    'r': {'item': 'minecraft:redstone'}}, 'result': {'id': 'rotarycraft:worktable', 'count': 1}})
w(D + '/recipe/auto_crafter.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['SCS', 'PcP', 'SPS'], 'key': {
    'S': STEEL, 'C': {'item': 'minecraft:crafting_table'}, 'P': item('base_panel'), 'c': item('circuit_board')},
    'result': {'id': 'rotarycraft:auto_crafter', 'count': 1}})
w(D + '/recipe/craft_pattern.json', {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': [' S ', ' B ', ' S '], 'key': {
    'S': STEEL, 'B': item('base_panel')}, 'result': {'id': 'rotarycraft:craft_pattern', 'count': 4}})
w(D + '/recipe/craft_pattern_clear.json', {'type': 'minecraft:crafting_shapeless', 'category': 'misc', 'ingredients': [item('craft_pattern')],
                                           'result': {'id': 'rotarycraft:craft_pattern', 'count': 1}})

# ---- lang and tags ----
lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.worktable': 'Worktable',
    'block.rotarycraft.auto_crafter': 'Auto-Crafter',
    'item.rotarycraft.craft_pattern': 'Craft Pattern',
    'item.rotarycraft.craft_pattern.crafts': 'Crafts %s %s',
    'item.rotarycraft.craft_pattern.no_output': 'Items, no output.',
    'item.rotarycraft.craft_pattern.empty': 'No crafting pattern.',
    'item.rotarycraft.craft_pattern.mode': 'Recipe mode: %s',
    'gui.rotarycraft.pattern_mode.crafting': 'Crafting Recipe',
    'gui.rotarycraft.pattern_mode.worktable': 'Worktable Recipe',
    'gui.rotarycraft.pattern_mode.blast_furnace': 'Blast Furnace Crafting',
    'gui.rotarycraft.pattern_limit': 'Input Limit',
    'gui.rotarycraft.crafter_mode.request': 'Request: crafts one cycle per request',
    'gui.rotarycraft.crafter_mode.continuous': 'Continuous: crafts as long as there are ingredients',
})
w(lang_path, lang)
path = R + '/data/minecraft/tags/block/mineable/pickaxe.json'
data = json.load(open(path))
data['values'] += [v for v in ('rotarycraft:worktable', 'rotarycraft:auto_crafter') if v not in data['values']]
w(path, data)
print('crafting ok')
