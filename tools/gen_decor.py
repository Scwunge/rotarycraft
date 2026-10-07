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

mg.finish()
