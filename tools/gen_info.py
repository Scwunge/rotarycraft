"""The information JEI shows for a machine (run by gen_logistics.py): the description is the machine's class comment, cleaned of its source notes, so what the
screen tells a player and what the code does cannot drift apart. Written to the lang file as info.rotarycraft.<id>."""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from machinegen import *  # noqa: E402,F401,F403

JAVA = 'src/main/java/net/scwunge/rotarycraft/'

# block id -> the source file whose class comment describes it
SOURCES = {
    'lamp': 'blockentity/LampBlockEntity', 'floodlight': 'blockentity/FloodlightBlockEntity', 'light_bridge': 'blockentity/LightBridgeBlockEntity',
    'particle_emitter': 'blockentity/ParticleEmitterBlockEntity', 'aerosolizer': 'blockentity/AerosolizerBlockEntity', 'self_destruct': 'blockentity/SelfDestructBlockEntity',
    'pile_driver': 'blockentity/PileDriverBlockEntity', 'beam_mirror': 'blockentity/BeamMirrorBlockEntity', 'firework_machine': 'blockentity/FireworkMachineBlockEntity',
    'item_cannon': 'blockentity/ItemCannonBlockEntity', 'arrow_gun': 'blockentity/ArrowGunBlockEntity', 'air_gun': 'blockentity/AirGunBlockEntity',
    'block_cannon': 'blockentity/BlockCannonBlockEntity', 'obsidian_maker': 'blockentity/ObsidianMakerBlockEntity', 'line_builder': 'blockentity/LineBuilderBlockEntity',
    'block_filler': 'blockentity/BlockFillerBlockEntity', 'spiller': 'blockentity/SpillerBlockEntity', 'music_box': 'decor/MusicBoxBlockEntity',
    'player_detector': 'logistics/PlayerDetectorBlockEntity', 'smoke_detector': 'logistics/SmokeDetectorBlockEntity', 'heater': 'logistics/HeaterBlockEntity',
    'firestarter': 'logistics/IgniterBlockEntity', 'refresher': 'logistics/ItemRefresherBlockEntity', 'wetter': 'logistics/WetterBlockEntity',
    'aggregator': 'logistics/AggregatorBlockEntity', 'grindstone': 'logistics/GrindstoneBlockEntity', 'purifier': 'logistics/PurifierBlockEntity',
    'bucket_filler': 'logistics/BucketFillerBlockEntity', 'filling_station': 'logistics/FillingStationBlockEntity', 'drop_processor': 'logistics/DropProcessorBlockEntity',
    'item_filter': 'logistics/ItemFilterBlockEntity', 'sorting': 'logistics/SortingBlockEntity', 'scale_chest': 'logistics/ScaleChestBlockEntity',
    'spillway': 'logistics/SpillwayBlockEntity',
}


def clean(comment):
    text = re.sub(r'\s*\n\s*\*\s?', ' ', comment).strip()
    text = re.sub(r'^[^:]*\(TileEntity\w+\):\s*', '', text)
    text = re.sub(r'\s*\((?:see )?\{@link [^}]*\}\)', '', text)
    text = re.sub(r'\{@link\s+#?([^}\s]*)[^}]*\}', '', text)
    text = re.sub(r'\{@code ([^}]*)\}', r'\1', text)
    sentences = re.split(r'(?<=[.!?])\s+', text)
    sentences = [s for s in sentences if 'id the screen sends' not in s and 'see ' not in s.lower()[:4]]
    text = ' '.join(sentences).replace('  ', ' ').strip()
    return text[:1].upper() + text[1:]


info = {}
for block, source in SOURCES.items():
    path = JAVA + source + '.java'
    if not os.path.exists(path):
        print('info: no source for', block)
        continue
    code = open(path, encoding='utf-8').read()
    m = re.search(r'/\*\*(.*?)\*/\s*public (?:abstract )?class', code, re.S)
    if m:
        info['info.rotarycraft.' + block] = clean(m.group(1))
names(info)

# docs/MACHINES_WORLD.md: the same descriptions as a reference page
import json  # noqa: E402

lang = json.load(open(LANG_PATH))
lang.update(LANG)
rows = ['# World, decoration and logistics machines', '',
        'What each machine does. This page is written by `tools/gen_info.py` from the comments in the code, and JEI shows the same text. Machines that change blocks or',
        'burn things (the Spillway, Self Destruct, Pile Driver, Line Builder, Block Filler, Spiller, Block Cannon, Firestarter) are off until the server turns them on in',
        '`rotarycraft-machines.toml`.', '']
for block in sorted(SOURCES, key=lambda b: lang.get('block.rotarycraft.' + b, b)):
    key = 'info.rotarycraft.' + block
    if key in info:
        rows += ['## ' + lang.get('block.rotarycraft.' + block, block), '', info[key], '']
open('docs/MACHINES_WORLD.md', 'w', encoding='utf-8').write('\n'.join(rows))
