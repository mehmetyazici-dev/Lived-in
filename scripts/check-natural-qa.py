"""Read natural-scene identity, association, unload, and music logs; requires Python 3."""
import json
from pathlib import Path
import re
import sys

log = Path(sys.argv[1]).read_text(encoding='utf-8', errors='replace')
def arrays(line):
    return [tuple(map(int, text.split(','))) for text in re.findall(r'\[I;([\d, -]+)\]', line)]
snapshots = []
for line in log.splitlines():
    if 'NaturalQA IDs: ' in line:
        snapshots.append({'ids': arrays(line), 'pet': [], 'owner': [], 'boat': [], 'passenger': []})
    elif snapshots:
        for label, key in [('fisher pet', 'pet'), ('cat owner', 'owner'), ('rider boat', 'boat'), ('boat passenger', 'passenger')]:
            if f'NaturalQA {label}: ' in line: snapshots[-1][key].extend(arrays(line))
assert len(snapshots) >= 3, 'Need baseline, revisit, and reopened-world snapshots'
baseline = snapshots[0]
assert len(baseline['ids']) == len(set(baseline['ids'])) == 7, 'Expected five travelers, one cat, one boat'
for snapshot in snapshots:
    assert len(snapshot['ids']) == 7 and set(snapshot['ids']) == set(baseline['ids']), 'Identities changed'
    for key in ['pet', 'owner', 'boat', 'passenger']:
        assert len(snapshot[key]) == 1 and snapshot[key] == baseline[key], f'Association changed: {key}'
    assert snapshot['pet'][0] == snapshot['ids'][5], 'Pet reference must match cat'
    assert snapshot['boat'][0] == snapshot['ids'][6], 'Boat reference must match boat'
    assert snapshot['owner'][0] in snapshot['ids'][:5] and snapshot['passenger'][0] in snapshot['ids'][:5]
active = set()
totals = []
for line in log.splitlines():
    match = re.search(r'Lived-In music: voice (opened|closed) for ([\da-f-]+); total (\d+)', line)
    if match:
        action, uuid, count = match.groups()
        if action == 'opened':
            assert uuid not in active, 'Duplicate musician voice'
            active.add(uuid)
        else:
            assert uuid in active, 'Closed unknown voice'
            active.remove(uuid)
        assert len(active) == int(count)
        totals.append(int(count))
    elif 'Lived-In music: cleared ' in line and ' on world change' in line:
        active.clear()
assert totals and max(totals) == 1 and min(totals) == 0, 'Expected one musician voice and full cleanup'
assert 'NaturalQA ERROR:' not in log
unloaded = log.count('NaturalQA unloaded chunk')
assert unloaded >= 3 and unloaded % 3 == 0, 'Incomplete three-scene unload check'
print(json.dumps({'snapshots': len(snapshots), 'unchanged_entity_uuids': 7,
                  'unload_revisit_cycles': unloaded // 3, 'max_music_voices': max(totals),
                  'pet_and_passenger_links_unchanged': True}, indent=2))
