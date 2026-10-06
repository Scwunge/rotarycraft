"""Generates the Extractor data: ore types (rotarycraft:extraction), flake smelting (rotarycraft:tag_smelting),
product textures, the extractor block assets and lang. Run from the repository root after gen_assets.py:
python tools/gen_extractor.py
"""
import json
import os
import random

from PIL import Image, ImageDraw

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
D = R + '/data/rotarycraft'
rnd = random.Random(11)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(obj, f, indent=2)
        f.write('\n')


def flakes(ore_type, color, count=1):
    return {'id': 'rotarycraft:ore_flakes', 'count': count,
            'components': {'rotarycraft:ore_product': {'type': ore_type, 'color': color}}}


# ore type: (ore tag, colour, rarity, result tag or item, result count, display name, bonus, bonus chance)
# Rarity and bonuses follow the original (ReikaOreHelper / ExtractorBonus); vanilla emerald is the only rare ore.
TUNGSTEN = 0x5A6470
SILVER = 0xC7D7E0
GOLD = 0xFCEE4B
NICKEL = 0xDADCA8
PLATINUM = 0x9FE0F5
CERTUS = 0xC8E6F0
ALUMINUM_POWDER = {'id': 'rotarycraft:aluminum_powder', 'count': 1}
ORES = {
    'coal':     ('c:ores/coal', 0x3A3A3A, 'common', 'item:minecraft:coal', 1, 'Coal', {'id': 'minecraft:gunpowder', 'count': 1}, 0.0625),
    'iron':     ('c:ores/iron', 0xD8AF93, 'common', 'c:ingots/iron', 1, 'Iron', flakes('tungsten', TUNGSTEN), 0.025),
    'gold':     ('c:ores/gold', GOLD, 'common', 'c:ingots/gold', 1, 'Gold', flakes('silver', SILVER), 0.125),
    'copper':   ('c:ores/copper', 0xE77C56, 'common', 'c:ingots/copper', 1, 'Copper', flakes('gold', GOLD), 0.25),
    'redstone': ('c:ores/redstone', 0xAA0F01, 'common', 'item:minecraft:redstone', 4, 'Redstone', ALUMINUM_POWDER, 0.25),
    'lapis':    ('c:ores/lapis', 0x1F4FB0, 'common', 'item:minecraft:lapis_lazuli', 4, 'Lapis', ALUMINUM_POWDER, 0.125),
    'diamond':  ('c:ores/diamond', 0x5DECF5, 'common', 'item:minecraft:diamond', 1, 'Diamond', None, 0),
    'emerald':  ('c:ores/emerald', 0x17DD62, 'rare', 'item:minecraft:emerald', 1, 'Emerald', None, 0),
    'quartz':   ('c:ores/quartz', 0xE7E0D5, 'nether', 'item:minecraft:quartz', 1, 'Nether Quartz', flakes('certus_quartz', CERTUS), 0.0625),
    'netherite_scrap': ('c:ores/netherite_scrap', 0x5C4038, 'nether', 'item:minecraft:netherite_scrap', 1, 'Netherite Scrap', None, 0),
    # other mods' ores: only active when the pack has them
    'tin':      ('c:ores/tin', 0xD7E2E8, 'common', 'c:ingots/tin', 1, 'Tin', None, 0),
    'lead':     ('c:ores/lead', 0x5D6280, 'common', 'c:ingots/lead', 1, 'Lead', flakes('nickel', NICKEL), 0.25),
    'silver':   ('c:ores/silver', SILVER, 'common', 'c:ingots/silver', 1, 'Silver', None, 0),
    'nickel':   ('c:ores/nickel', NICKEL, 'common', 'c:ingots/nickel', 1, 'Nickel', flakes('platinum', PLATINUM), 0.5),
    'aluminum': ('c:ores/aluminum', 0xE1E6E8, 'common', 'c:ingots/aluminum', 1, 'Aluminum', None, 0),
    'uranium':  ('c:ores/uranium', 0x5E8A3C, 'common', 'c:ingots/uranium', 1, 'Uranium', None, 0),
    'osmium':   ('c:ores/osmium', 0x9CB6D0, 'common', 'c:ingots/osmium', 1, 'Osmium', flakes('iron', 0xD8AF93), 0.125),
    'zinc':     ('c:ores/zinc', 0xBAC4C8, 'common', 'c:ingots/zinc', 1, 'Zinc', None, 0),
    'platinum': ('c:ores/platinum', 0x9FE0F5, 'common', 'c:ingots/platinum', 1, 'Platinum', None, 0),
    'tungsten': ('c:ores/tungsten', TUNGSTEN, 'common', 'c:ingots/tungsten', 1, 'Tungsten', flakes('iron', 0xD8AF93), 0.75),
    'thorium':  ('c:ores/thorium', 0x3D3D3D, 'common', 'c:ingots/thorium', 1, 'Thorium', None, 0),
    'certus_quartz': ('c:ores/certus_quartz', CERTUS, 'common', 'c:gems/certus_quartz', 1, 'Certus Quartz', flakes('quartz', 0xE7E0D5), 0.5),
}

for t, (tag, color, rarity, result, count, name, bonus, chance) in ORES.items():
    recipe = {'type': 'rotarycraft:extraction', 'ore': {'tag': tag}, 'ore_type': t, 'color': color, 'rarity': rarity}
    if bonus:
        recipe['bonus'] = bonus
        recipe['bonus_chance'] = chance
    w('%s/recipe/extraction/%s.json' % (D, t), recipe)

    ingredient = {'type': 'neoforge:components', 'items': 'rotarycraft:ore_flakes',
                  'components': {'rotarycraft:ore_product': {'type': t, 'color': color}}}
    if result.startswith('item:'):
        smelt = {'type': 'minecraft:smelting', 'category': 'misc', 'ingredient': ingredient,
                 'result': {'id': result[5:], 'count': count}, 'experience': 0.7, 'cookingtime': 200}
    else:
        smelt = {'type': 'rotarycraft:tag_smelting', 'ingredient': ingredient, 'result_tag': result, 'count': count,
                 'experience': 0.7, 'cookingtime': 200}
    w('%s/recipe/flakes_smelting/%s.json' % (D, t), smelt)

# the two rare metals the bonuses produce; this mod supplies them so the bonus is always usable
for metal in ('silver', 'tungsten'):
    w('%s/data/c/tags/item/ingots/%s.json' % (R, metal), {'values': ['rotarycraft:%s_ingot' % metal]})
w(R + '/data/c/tags/item/ingots.json', {'values': ['rotarycraft:hsla_steel_ingot', 'rotarycraft:silver_ingot', 'rotarycraft:tungsten_ingot']})

# textures: grey shapes the ore colour tints ------------------------------------------------------------------------
T = A + '/textures/item'


def grey(c):
    v = max(0, min(255, c))
    return (v, v, v, 255)


im = Image.new('RGBA', (16, 16))
p = im.load()
for x in range(16):
    for y in range(16):
        dx, dy = x - 7.5, y - 10
        if dx * dx / 42 + dy * dy / 18 <= 1:
            p[x, y] = grey(200 + rnd.randint(-40, 40))
im.save(T + '/ore_dust.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.polygon([(3, 6), (12, 5), (14, 11), (8, 14), (2, 12)], fill=grey(190))
for _ in range(14):
    x, y = rnd.randint(4, 11), rnd.randint(7, 12)
    g.point((x, y), fill=grey(235))
im.save(T + '/ore_slurry.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
g.rectangle([6, 1, 9, 4], fill=(170, 170, 170, 255))
g.ellipse([3, 4, 12, 15], fill=grey(215), outline=(120, 120, 120, 255))
g.rectangle([5, 6, 7, 8], fill=grey(250))
im.save(T + '/ore_solution.png')

im = Image.new('RGBA', (16, 16))
g = ImageDraw.Draw(im)
for _ in range(7):
    x, y = rnd.randint(2, 11), rnd.randint(3, 11)
    g.polygon([(x, y), (x + 3, y + 1), (x + 2, y + 4), (x - 1, y + 3)], fill=grey(rnd.randint(170, 240)))
im.save(T + '/ore_flakes.png')

for metal, col in (('silver', (205, 215, 225)), ('tungsten', (95, 105, 115))):
    im = Image.new('RGBA', (16, 16))
    g = ImageDraw.Draw(im)
    g.polygon([(2, 10), (6, 6), (14, 6), (10, 10)], fill=col + (255,))
    g.polygon([(2, 10), (10, 10), (10, 12), (2, 12)], fill=tuple(max(0, c - 40) for c in col) + (255,))
    g.polygon([(10, 10), (14, 6), (14, 8), (10, 12)], fill=tuple(max(0, c - 60) for c in col) + (255,))
    im.save('%s/%s_ingot.png' % (T, metal))

for it in ('ore_dust', 'ore_slurry', 'ore_solution', 'ore_flakes', 'silver_ingot', 'tungsten_ingot'):
    w('%s/models/item/%s.json' % (A, it), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rotarycraft:item/' + it}})

# lang additions ----------------------------------------------------------------------------------------------------
lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
lang.update({
    'block.rotarycraft.extractor': 'Extractor',
    'item.rotarycraft.ore_dust': 'Ore Dust',
    'item.rotarycraft.ore_slurry': 'Ore Slurry',
    'item.rotarycraft.ore_solution': 'Ore Solution',
    'item.rotarycraft.ore_flakes': 'Ore Flakes',
    'item.rotarycraft.ore_dust.named': '%s Dust',
    'item.rotarycraft.ore_slurry.named': '%s Slurry',
    'item.rotarycraft.ore_solution.named': '%s Solution',
    'item.rotarycraft.ore_flakes.named': '%s Flakes',
    'item.rotarycraft.silver_ingot': 'Silver Ingot',
    'item.rotarycraft.tungsten_ingot': 'Tungsten Ingot',
    'gui.rotarycraft.extractor.status': '%s, %s/4 stages, %sB water',
    'gui.rotarycraft.extractor.stage1': 'Ore to dust: %s N*m, %s rad/s, %s',
    'gui.rotarycraft.extractor.stage2': 'Dust to slurry: %s N*m, %s rad/s, %s',
    'gui.rotarycraft.extractor.stage3': 'Slurry to solution: %s N*m, %s rad/s, %s',
    'gui.rotarycraft.extractor.stage4': 'Solution to flakes: %s N*m, %s rad/s, %s',
    'gui.rotarycraft.water': 'Water: %s / %s mB',
})
for t, v in ORES.items():
    lang['ore_type.rotarycraft.' + t] = v[5]
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('extractor data:', len(ORES), 'ore types')
