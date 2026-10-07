"""The original's sounds (run by gen_assets.py): the ogg files, their entries in sounds.json (merged with the ones that are already there) and
their subtitles. The names are also in registry/MachineSoundRegistry.java, which makes the sound events."""
import json
import os
import shutil

R = 'src/main/resources'
A = R + '/assets/rotarycraft'
REF = 'reference/RotaryCraft/Sounds'

# event name, file under the original's Sounds folder, subtitle
SOUNDS = [
    ('afterburner', 'afterburner', 'Afterburner roars'),
    ('belt', 'belt', 'Belt runs'),
    ('coil', 'coil', 'Coil hums'),
    ('compress', 'compress', 'Compressor runs'),
    ('craft', 'craft', 'Machine crafts'),
    ('diesel', 'diesel', 'Diesel engine runs'),
    ('dynamo', 'dynamo', 'Dynamo hums'),
    ('elecengine', 'elecengine', 'Electric engine hums'),
    ('fan', 'fan', 'Fan whirs'),
    ('flywheel', 'flywheel', 'Flywheel spins'),
    ('friction', 'friction', 'Friction grinds'),
    ('fridge', 'fridge', 'Refrigerator hums'),
    ('gasengine', 'gasengine', 'Gas engine runs'),
    ('hydroengine', 'hydroengine', 'Hydro engine runs'),
    ('ingest', 'ingest', 'Engine swallows'),
    ('ingest_short', 'ingest_short', 'Engine swallows'),
    ('jetengine', 'jetengine', 'Jet engine roars'),
    ('jetstart', 'jetstart', 'Jet engine starts up'),
    ('knockback', 'knockback', 'Stun gun thumps'),
    ('linebuild', 'linebuild', 'Line builder places'),
    ('massivebang', 'massivebang', 'Massive explosion'),
    ('microengine', 'microengine', 'Microturbine whines'),
    ('pack', 'pack', 'Jetpack burns'),
    ('piledriver', 'piledriver', 'Pile driver slams'),
    ('pneu', 'pneu', 'Pneumatic engine hisses'),
    ('projector', 'projector', 'Projector whirs'),
    ('pulsejet', 'pulsejet', 'Pulse jet fires'),
    ('pump', 'pump', 'Pump runs'),
    ('rumble', 'rumble', 'Ground rumbles'),
    ('rumble2', 'rumble2', 'Ground rumbles'),
    ('shortjet', 'shortjet', 'Jet bursts'),
    ('smokealarm', 'smokealarm', 'Smoke alarm beeps'),
    ('spark', 'spark', 'Spark zaps'),
    ('sprinkler', 'sprinkler', 'Sprinkler sprays'),
    ('steamengine', 'steamengine', 'Steam engine chuffs'),
    ('windengine', 'windengine', 'Wind engine turns'),
]
# the music box's notes: event name, file under Sounds/music
NOTES = [('note_bass_low', 'basslo'), ('note_bass', 'bass'), ('note_bass_high', 'basshi'),
         ('note_harp_low', 'harplo'), ('note_harp', 'harp'), ('note_harp_high', 'harphi'),
         ('note_pling_low', 'plinglo'), ('note_pling', 'pling'), ('note_pling_high', 'plinghi')]

os.makedirs(A + '/sounds/music', exist_ok=True)
path = A + '/sounds.json'
data = json.load(open(path)) if os.path.exists(path) else {}
lang_path = A + '/lang/en_us.json'
lang = json.load(open(lang_path))
for event, name, subtitle in SOUNDS:
    shutil.copy('%s/%s.ogg' % (REF, name), '%s/sounds/%s.ogg' % (A, name))
    data[event] = {'sounds': ['rotarycraft:' + name], 'subtitle': 'subtitles.rotarycraft.' + event}
    lang['subtitles.rotarycraft.' + event] = subtitle
for event, name in NOTES:
    shutil.copy('%s/music/%s.ogg' % (REF, name), '%s/sounds/music/%s.ogg' % (A, name))
    data[event] = {'sounds': ['rotarycraft:music/' + name], 'subtitle': 'subtitles.rotarycraft.note'}
lang['subtitles.rotarycraft.note'] = 'Music box plays a note'
with open(path, 'w') as f:
    json.dump(data, f, indent=2)
    f.write('\n')
with open(lang_path, 'w') as f:
    json.dump(lang, f, indent=2)
    f.write('\n')
print('sounds ok, %d events' % len(data))
