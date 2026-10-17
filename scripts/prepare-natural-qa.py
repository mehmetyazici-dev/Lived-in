"""Copy a completed seed-42 terrain survey into the isolated client for inspection.

Requires Python 3. No scene commands, entity creation, or terrain preparation.
Run from the project root after the survey server has stopped.
"""
from pathlib import Path
import gzip
import shutil
import struct

source = Path('runs/encounter-survey/survey-final-42')
target = Path('runs/package-smoke/saves/Lived-In Natural Phase 1')
if target.exists():
    raise SystemExit('QA save already exists; refusing to overwrite it.')
if not source.joinpath('level.dat').exists():
    raise SystemExit('Run the completed seed-42 survey first.')
shutil.copytree(source, target)
raw = gzip.decompress(target.joinpath('level.dat').read_bytes())
cheat_marker = b'\x01\x00\x0dallowCommands'
cheat_offset = raw.index(cheat_marker) + len(cheat_marker)
raw = raw[:cheat_offset] + b'\x01' + raw[cheat_offset + 1:]
marker = b'\x08\x00\x09LevelName'
offset = raw.index(marker) + len(marker)
length = struct.unpack('>H', raw[offset:offset + 2])[0]
name = b'Lived-In Natural Phase 1'
target.joinpath('level.dat').write_bytes(gzip.compress(raw[:offset] + struct.pack('>H', len(name))
                                                    + name + raw[offset + 2 + length:]))
pack = target / 'datapacks/natural-qa'
functions = pack / 'data/naturalqa/function'
functions.mkdir(parents=True, exist_ok=True)
pack.joinpath('pack.mcmeta').write_text('{"pack":{"pack_format":48,"description":"Local natural encounter inspection"}}')

def write(name, commands):
    functions.joinpath(name + '.mcfunction').write_text('\n'.join(commands) + '\n')

write('start', ['gamemode spectator', 'tp @s -199 69 338 0 18'])
write('camp', ['tp @s -199 69 338 0 18'])
write('fishing', ['tp @s -68 64 339 90 12'])
write('boat', ['tp @s -46 65 187 0 15'])
write('overview', ['tp @s -121 90 276 0 30'])
centers = [(-199, 67, 344), (-73, 63, 338), (-47, 62, 193)]
check = ['data modify storage naturalqa:state ids set value []']
for kind in ['livedin:ambient_npc', 'minecraft:cat', 'minecraft:boat']:
    for x, y, z in centers:
        selector = f'@e[type={kind},distance=..12]'
        if kind == 'minecraft:cat': selector = '@e[type=minecraft:cat,tag=livedin_fishing_cat,distance=..12]'
        check.append(f'execute positioned {x} {y} {z} as {selector} run data modify storage naturalqa:state ids append from entity @s UUID')
check.append('tellraw @s [{"text":"NaturalQA IDs: "},{"nbt":"ids","storage":"naturalqa:state"}]')
for label, pos, selector, field in [
    ('fisher pet', centers[1], '@e[type=livedin:ambient_npc,nbt={NpcRole:3},distance=..12]', 'PetCat'),
    ('cat owner', centers[1], '@e[type=minecraft:cat,tag=livedin_fishing_cat,distance=..12]', 'Owner'),
    ('rider boat', centers[2], '@e[type=livedin:ambient_npc,nbt={NpcRole:4},distance=..12]', 'SceneBoat'),
    ('boat passenger', centers[2], '@e[type=minecraft:boat,distance=..12]', 'Passengers[0].UUID')]:
    check.append(f'execute positioned {pos[0]} {pos[1]} {pos[2]} as {selector} run tellraw @a '
                 f'[{{"text":"NaturalQA {label}: "}},{{"nbt":"{field}","entity":"@s"}}]')
write('check', check)
write('leave', ['tp @s 2000 120 2000', 'schedule function naturalqa:return 35s replace'])
back = []
for x, y, z in centers:
    back.extend([f'execute if loaded {x} {y} {z} run tellraw @a {{"text":"NaturalQA ERROR: scene chunk still loaded"}}',
                 f'execute unless loaded {x} {y} {z} run tellraw @a {{"text":"NaturalQA unloaded chunk {x},{z}"}}'])
back.extend(['tp @a -121 90 276 0 30', 'schedule function naturalqa:revisited 8s replace'])
write('return', back)
write('revisited', ['execute as @a run function naturalqa:check'])
print(target.resolve())
