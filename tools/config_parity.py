"""Writes src/main/resources/gametest/original_config.txt: every option of the original's ConfigRegistry against what this port calls it.
Lines are  MAP|original|port key  (the key must exist in one of the config specs),  PENDING|original|what it needs|key it will get  (the GameTest fails when the
key appears, so the list is moved on to MAP), or  NA|original|why it does not apply. Run from the repo root."""
import os
import re

SRC = open('reference/RotaryCraft/Registry/ConfigRegistry.java', encoding='latin-1').read()

MAP = {
    'HANDBOOK': 'spawnWithHandbook',
    'SNEAKWINGS': 'jetpackWingsOnSneak',
    'KICKFLYING': 'jetpackBypassesFlyCheck',
    'JETFUELPACK': 'jetpackNeedsJetFuel',
    'CONSERVEPACK': 'conservativeJetpack',
    'ENGINEVOLUME': 'engineVolume', 'MACHINEVOLUME': 'machineVolume', 'FLOODLIGHTRANGE': 'floodlightRange', 'HEATRAYRANGE': 'heatRayRange',
    'BRIDGERANGE': 'bridgeRange', 'FANRANGE': 'fanRange', 'AERORANGE': 'aerosolizerRange', 'VACUUMRANGE': 'vacuumRange', 'FORCERANGE': 'forceFieldRange',
    'SONICBORERRANGE': 'sonicBorerRange', 'SPAWNERLIMIT': 'spawnerMobLimit', 'BREEDERRANGE': 'breederRange', 'BAITRANGE': 'baitBoxRange', 'DETECTORRANGE': 'detectorRange', 'GRAVELPLAYER': 'gravelGunPvp', 'HARDGRAVELGUN': 'hardGravelGun', 'FREEWATER': 'freeWaterFactor',
    'LINEBUILDER': 'lineBuilderLength', 'BAITMOBS': 'baitBoxMobs', 'CAVEFINDERRANGE': 'caveScannerRange', 'BANRAIN': 'weatherControllerBansRain',
    'BLOCKDAMAGE': 'explosionsBreakBlocks', 'BIOMEBLOCKS': 'terraformerEditsBlocks', 'RAILGUNDAMAGE': 'railgunBlockDamage', 'TURRETPLAYERS': 'turretsTargetPlayers',
    'ATTACKBLOCKS': 'weaponBlockDamage', 'VOIDHOLE': 'bedrockBreakerVoidHole', 'BLOWERSPILL': 'itemPumpSpills', 'BORERMAINTAIN': 'borerRequiresMaintenance', 'BEDPICKSPAWNERS': 'bedrockPickHarvestsSpawners', 'PREENCHANT': 'lockBedrockEnchants',
    'FRICTIONXP': 'frictionHeaterXp', 'SPILLERRANGE': 'areaFillerRange', 'CONVERTERLOSS': 'converterEfficiency', 'ALLOWTNTCANNON': 'tntCannon',
    'ALLOWEMP': 'emp', 'ALLOWLIGHTBRIDGE': 'lightBridge', 'ALLOWITEMCANNON': 'itemCannon', 'ALLOWCHUNKLOADER': 'chunkLoader',
    'LOCKMACHINES': 'ownerOnlyMachines',
    'EXTRACTORMAINTAIN': 'extractorWear',
    'CHUNKLOADERSIZE': 'chunkLoaderMaxRadius', 'BORERPOW': 'borerPowerFactor',
    'GPRORES': 'gprShowsOres',
    'SPAWNERLEAK': 'spawnersLeakMobs',
    'EMPLOAD': 'empChargeSpeed',
    'PROJECTORLINES': 'projectorLines',
    'SPRINKLER': 'sprinklerParticles',
    'FLOWSPEED': 'fluidFlowSpeed',
    'PIPEHARDNESS': 'pipeHardness',
    'FAKEBEDROCK': 'fakePlayerBedrockAbilities',
    'BORERGEN': 'borerChunkRadius',
    'CRAFTERPROFILE': 'crafterProfiling',
    'HSLAHARVEST': 'steelToolsHarvestHigher',
    'VACPOWER': 'vacuumPowerPerMetre',
}
NA = {
    'DYNAMICHANDBOOK': 'the handbook is read from the resource packs, so reloading them (F3 + T) picks up changes',
    'ACHIEVEMENTS': 'no achievements; advancements can be added by a data pack', 'MODORES': 'ores are matched through c: tags',
    'DIFFICULTY': 'the original\'s medium-difficulty numbers are used throughout', 'TABLEMACHINES': 'machines are made at the ordinary crafting table; a data pack can change that',
    'ROTATEHOSE': 'a recipe-conflict workaround for old mods', 'HSLADICT': 'steel is the c:ingots/steel tag', 'TEGLASS': 'Thermal Expansion only',
    'NOMINERS': 'switch the Borer, Bedrock Breaker and Sonic Borer off in their own settings', 'HARDEU': 'IC2/GregTech EU compatibility only',
    'POWERCLIENT': 'power is read on the server and synced', 'FRAMES': 'frame mods are not supported', 'RECIPEMOD': 'recipes are data packs', 'STRONGRECIPEMOD': 'recipes are data packs',
    'BEEYEAST': 'Forestry only', 'HARDCONVERTERS': 'converter units to other mods\' energy are not made', 'OREALUDUST': 'other mods\' dusts join through c: tags',
    'GATEBLAST': 'recipe gating is a data-pack concern', 'GATEWORK': 'recipe gating is a data-pack concern', 'HYDROSTREAMFALLMAX': 'Streams mod only',
    'TINKERFLAKES': 'Tinkers\' Construct only', 'IC2BLAZECOMPRESS': 'IC2 only', 'LOGBLOCKS': 'claim and logging mods see the block events the machines fire',
    'INSTACUT': 'only the instant mode (the originals default) exists',
    'CRAFTABLEBEDROCK': 'the bedrock recipes ship on; a data pack removes them (recipes load before this port\'s configs)',
    'LATEDYNAMO': 'recipe gating is a data-pack concern (recipes load before this port\'s configs)',
    'CHESTGEN': 'dungeon loot is a loot-table data pack in 1.21 (data/rotarycraft/loot_modifiers)',
    'EXTRAIRON': 'ore density is a placed-feature data pack in 1.21 (change the iron ore feature\'s count)',
    'CLEARCHAT': 'messages go to the action bar, which does not pile up in chat',
    'COLORBLIND': 'the port has no colour-coded IO-side overlay for it to change',
    'ALLOWBAN': 'claim mods can stop any machine', 'HARDCONVERTERS2': '',
}
PENDING = {
    'INSTACUT': ('the Woodcutter dropping its wood at once', 'woodcutterInstant'),
}


def options():
    body = SRC[SRC.index('enum ConfigRegistry') if 'enum ConfigRegistry' in SRC else 0:]
    names = []
    for m in re.finditer(r'^\s*([A-Z][A-Z0-9_]+)\(\s*"', body, re.M):
        names.append(m.group(1))
    return names


def main():
    lines = []
    for name in options():
        if name in MAP:
            lines.append('MAP|%s|%s' % (name, MAP[name]))
        elif name in NA:
            lines.append('NA|%s|%s' % (name, NA[name]))
        elif name in PENDING:
            lines.append('PENDING|%s|%s|%s' % (name, PENDING[name][0], PENDING[name][1]))
        else:
            print('UNCLASSIFIED', name)
            lines.append('PENDING|%s|not looked at yet|' % name)
    os.makedirs('src/main/resources/gametest', exist_ok=True)
    with open('src/main/resources/gametest/original_config.txt', 'w') as f:
        f.write('\n'.join(lines) + '\n')
    print(len(lines), 'options;', sum(l.startswith('MAP') for l in lines), 'mapped,', sum(l.startswith('PENDING') for l in lines), 'pending,',
          sum(l.startswith('NA') for l in lines), 'not applicable')


main()
