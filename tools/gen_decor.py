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

# ---- Particle Emitter (a plain cube: the original's picture is not in the reference, so the mark is drawn) ----


def particle_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.rectangle([3, 3, 12, 12], fill=(40, 44, 60, 255), outline=(150, 154, 162, 255))
    for x, y, c in ((5, 9, (255, 120, 60, 255)), (8, 6, (120, 220, 255, 255)), (10, 9, (170, 255, 120, 255)), (7, 10, (255, 240, 120, 255))):
        g.rectangle([x, y, x + 1, y + 1], fill=c)


drawn_machine('particle_emitter', particle_mark, face='up')
gui('particlegui.png', 'particle_emitter')
gui('particles.png', 'particle_icons')
shaped('particle_emitter', ['SDS', 'PCP', 'SIS'], {'S': STEEL, 'P': BASEPANEL, 'C': CIRCUIT, 'D': vanilla('dispenser'), 'I': IMPELLER}, count=4)
names({'block.rotarycraft.particle_emitter': 'Particle Emitter'})

# ---- Lamp ----


def lamp_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.ellipse([3, 3, 12, 12], fill=(255, 236, 140, 255), outline=(150, 154, 162, 255))
    g.ellipse([6, 5, 9, 8], fill=(255, 255, 220, 255))


drawn_machine('lamp', lamp_mark, face='up')
gui_copy('one_slot', 'lamp')
shaped('lamp', ['SGS', 'GgG', 'SGS'], {'S': STEEL, 'G': vanilla('glass'), 'g': vanilla('glowstone')})
names({'block.rotarycraft.lamp': 'Lamp'})

# ---- Flood Light (the lamp model, and the vertical lamp for up and down) ----
rendered_machine('floodlight', model='ModelLamp', texture='lamptex.png')
mg.MODELS.append('ModelVLamp:floodlight_v')
model_texture('LampVertical.png', 'floodlight_v')
shaped('floodlight', ['ISO', 'Ggd', 'I#O'], {'#': BASEPANEL, 'I': vanilla('iron_ingot'), 'd': vanilla('gold_ingot'), 'S': STEEL, 'G': vanilla('glass'),
                                              'g': vanilla('glowstone'), 'O': vanilla('obsidian')})
names({'block.rotarycraft.floodlight': 'Flood Light'})

# ---- Light Bridge, and the blocks of light the two make ----
rendered_machine('light_bridge', model='ModelBridge', texture='bridge.png')
shaped('light_bridge', ['GgG', 'BgS', 'BBD'], {'B': BASEPANEL, 'S': STEEL, 'D': vanilla('diamond'), 'G': vanilla('gold_ingot'), 'g': vanilla('glass')})
names({'block.rotarycraft.light_bridge': 'Light Bridge', 'block.rotarycraft.beam': 'Light Beam', 'block.rotarycraft.bridge': 'Light Bridge Beam'})


def light_textures():
    from PIL import Image, ImageDraw
    beam = Image.new('RGBA', (16, 16), (255, 255, 210, 64))
    beam.save('%s/block/beam.png' % T)
    plate = Image.new('RGBA', (16, 16), (110, 190, 255, 150))
    g = ImageDraw.Draw(plate)
    g.rectangle([0, 0, 15, 15], outline=(210, 240, 255, 230))
    g.line([(0, 8), (15, 8)], fill=(235, 250, 255, 230))
    g.line([(0, 4), (15, 4)], fill=(170, 220, 255, 190))
    g.line([(0, 12), (15, 12)], fill=(170, 220, 255, 190))
    plate.save('%s/block/bridge.png' % T)


light_textures()
w('%s/models/block/beam.json' % A, {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent', 'textures': {'all': 'rotarycraft:block/beam'}})
w('%s/blockstates/beam.json' % A, {'variants': {'': {'model': 'rotarycraft:block/beam'}}})
w('%s/models/block/bridge.json' % A, {'textures': {'plate': 'rotarycraft:block/bridge', 'particle': 'rotarycraft:block/bridge'}, 'render_type': 'minecraft:translucent',
                                      'elements': [{'from': [0, 1, 0], 'to': [16, 2.428, 16], 'faces': {
                                          'up': {'texture': '#plate', 'uv': [0, 0, 16, 16]}, 'down': {'texture': '#plate', 'uv': [0, 0, 16, 16]},
                                          'north': {'texture': '#plate', 'uv': [0, 1, 16, 2.4]}, 'south': {'texture': '#plate', 'uv': [0, 1, 16, 2.4]},
                                          'east': {'texture': '#plate', 'uv': [0, 1, 16, 2.4]}, 'west': {'texture': '#plate', 'uv': [0, 1, 16, 2.4]}}}]})
w('%s/blockstates/bridge.json' % A, {'variants': {'axis=x': {'model': 'rotarycraft:block/bridge'}, 'axis=z': {'model': 'rotarycraft:block/bridge', 'y': 90}}})

# ---- Aerosolizer ----
rendered_machine('aerosolizer', model='ModelAerosolizer', texture='aerotex.png')
gui('aerosolizergui.png', 'aerosolizer')
shaped('aerosolizer', ['BRB', 'RIR', 'BRB'], {'B': BASEPANEL, 'R': item('reservoir'), 'I': IMPELLER})
names({'block.rotarycraft.aerosolizer': 'Aerosolizer'})

# ---- Self Destruct ----


def bomb_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.ellipse([3, 4, 12, 13], fill=(30, 30, 34, 255), outline=(150, 154, 162, 255))
    g.line([(8, 4), (10, 1)], fill=(190, 150, 90, 255))
    g.point((10, 1), fill=(255, 120, 40, 255))


drawn_machine('self_destruct', bomb_mark, face='up')
shaped('self_destruct', ['STS', 'TCs', 'STS'], {'S': STEEL, 'T': vanilla('tnt'), 's': SHAFT, 'C': CIRCUIT})
names({'block.rotarycraft.self_destruct': 'Self Destruct'})

# ---- Pile Driver, and the pile it drives ----
rendered_machine('pile_driver', model='ModelPileDriver', texture='piletex.png')
shaped('pile_driver', ['PGP', 'gFg', 'PDP'], {'P': BASEPANEL, 'G': GEAR8, 'g': SHAFT, 'F': item('bedrock_flywheel_core'), 'D': DRILL})
names({'block.rotarycraft.pile_driver': 'Pile Driver', 'block.rotarycraft.pile_pipe': 'Pile'})
_sides = {f: {'texture': '#pipe', 'uv': [5, 0, 11, 16]} for f in ('north', 'south', 'east', 'west')}
_ends = {f: {'texture': '#pipe', 'uv': [5, 5, 11, 11]} for f in ('up', 'down')}
w('%s/models/block/pile_pipe.json' % A, {'textures': {'pipe': 'rotarycraft:block/mining_pipe', 'particle': 'rotarycraft:block/mining_pipe'}, 'elements': [{
    'from': [5.28, 0, 5.28], 'to': [10.72, 16, 10.72], 'faces': dict(_sides, **_ends)}]})
w('%s/blockstates/pile_pipe.json' % A, {'variants': {'': {'model': 'rotarycraft:block/pile_pipe'}}})

# ---- Beam Mirror ----
rendered_machine('beam_mirror', model='ModelBeamMirror', texture='beammirrortex.png')
shaped('beam_mirror', [' m ', ' s ', ' p '], {'p': BASEPANEL, 'm': MIRROR, 's': STEEL})
names({'block.rotarycraft.beam_mirror': 'Beam Mirror'})

# ---- Firework Machine (a plain cube) ----


def firework_mark(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.rectangle([3, 3, 12, 12], fill=(40, 30, 60, 255), outline=(150, 154, 162, 255))
    for x, y, c in ((8, 4, (255, 90, 90, 255)), (5, 7, (255, 220, 80, 255)), (11, 7, (90, 200, 255, 255)), (8, 10, (120, 255, 140, 255))):
        g.point((x, y), fill=c)
        g.point((x + 1, y), fill=c)
        g.point((x, y + 1), fill=c)


drawn_machine('firework_machine', firework_mark, face='up')
gui('basicstorage.png', 'firework_machine')
shaped('firework_machine', ['BEB', 'BDB', 'BRB'], {'B': BASEPANEL, 'E': vanilla('ender_eye'), 'D': vanilla('dispenser'), 'R': vanilla('redstone')})
names({'block.rotarycraft.firework_machine': 'Firework Machine'})

# ---- Item Cannon ----
rendered_machine('item_cannon', model='ModelItemCannon', texture='itemcannontex.png')
gui('targetgui.png', 'item_cannon')
shaped('item_cannon', ['s c', 'pcp', 'pCr'], {'s': STEEL, 'c': GEAR2, 'p': BASEPANEL, 'C': COMPRESSOR, 'r': vanilla('chest')})
names({'block.rotarycraft.item_cannon': 'Item Cannon', 'gui.rotarycraft.item_cannon.x': 'Target X', 'gui.rotarycraft.item_cannon.y': 'Target Y',
       'gui.rotarycraft.item_cannon.z': 'Target Z', 'gui.rotarycraft.item_cannon.dim': 'Target Dim'})

# ---- Arrow Gun (a plain cube with a muzzle) and Air Gun ----


def arrow_front(im):
    from PIL import ImageDraw
    g = ImageDraw.Draw(im)
    g.rectangle([3, 3, 12, 12], fill=(50, 50, 56, 255), outline=(150, 154, 162, 255))
    g.rectangle([6, 6, 9, 9], fill=(10, 10, 12, 255))
    g.line([(2, 8), (5, 8)], fill=(196, 160, 90, 255))


facing_machine('arrow_gun', arrow_front)
gui('basicstorage.png', 'arrow_gun')
shaped('arrow_gun', ['SSS', 'BDB', 'SBS'], {'B': BASEPANEL, 'S': STEEL, 'D': vanilla('dispenser')})
rendered_machine('air_gun', model='ModelAirGun', texture='airguntex.png')
shaped('air_gun', ['sps', 'I S', 'sps'], {'I': IMPELLER, 'p': BASEPANEL, 's': STEEL, 'S': item('sonar_unit')})
names({'block.rotarycraft.arrow_gun': 'Arrow Gun', 'block.rotarycraft.air_gun': 'Air Gun'})

# ---- Decorative Tank: a framed glass cube; the settings are block state (clear glass, ignore colour, glowing, resistant) ----


def tank_textures():
    from PIL import Image, ImageDraw
    for resistant in (False, True):
        for clear in (False, True):
            im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
            g = ImageDraw.Draw(im)
            frame = (60, 62, 72, 255) if resistant else (150, 154, 162, 255)
            inner = (35, 36, 44, 255) if resistant else (200, 204, 212, 255)
            if not clear:
                g.rectangle([1, 1, 14, 14], fill=(190, 225, 235, 70) if not resistant else (120, 130, 160, 110))
                g.line([(3, 3), (6, 3)], fill=(255, 255, 255, 140))
                g.line([(3, 3), (3, 6)], fill=(255, 255, 255, 140))
            g.rectangle([0, 0, 15, 15], outline=frame)
            g.rectangle([1, 1, 14, 14], outline=inner)
            im.save('%s/block/deco_tank%s%s.png' % (T, '_resistant' if resistant else '', '_clear' if clear else ''))


tank_textures()
for _resistant in (False, True):
    for _clear in (False, True):
        _name = 'deco_tank%s%s' % ('_resistant' if _resistant else '', '_clear' if _clear else '')
        w('%s/models/block/%s.json' % (A, _name), {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent', 'textures': {'all': 'rotarycraft:block/' + _name}})
_variants = {}
for _c in (False, True):
    for _n in (False, True):
        for _l in (False, True):
            for _r in (False, True):
                _key = 'clear=%s,lighted=%s,nocolor=%s,resistant=%s' % tuple(str(v).lower() for v in (_c, _l, _n, _r))
                _variants[_key] = {'model': 'rotarycraft:block/deco_tank%s%s' % ('_resistant' if _r else '', '_clear' if _c else '')}
w('%s/blockstates/deco_tank.json' % A, {'variants': _variants})
w('%s/models/item/deco_tank.json' % A, {'parent': 'rotarycraft:block/deco_tank'})

shaped('deco_tank', ['SGS', 'GGG', 'SGS'], {'S': STEEL, 'G': vanilla('glass_pane')}, count=4)
w('%s/recipe/deco_tank_settings.json' % D, {'type': 'rotarycraft:deco_tank_settings', 'category': 'misc'})
names({'block.rotarycraft.deco_tank': 'Decorative Tank', 'item.rotarycraft.deco_tank': 'Decorative Tank',
       'tooltip.rotarycraft.deco_tank.full': 'Full of %s', 'tooltip.rotarycraft.deco_tank.empty': 'Empty'})

mg.finish()
