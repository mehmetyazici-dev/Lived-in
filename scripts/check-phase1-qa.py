"""Validate the recorded local QA chat/music diagnostics; requires Python 3.

Usage: python scripts/check-phase1-qa.py runs/package-smoke/logs/latest.log
This reads logs only. It does not launch Minecraft or modify a save.
"""
import json
import pathlib
import re
import sys

log = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8", errors="replace")
arrays = lambda text: [tuple(map(int, item.split(","))) for item in re.findall(r"\[I;([\d, -]+)\]", text)]
snapshots = []
current = None
for line in log.splitlines():
    if "Phase1QA IDs: " in line:
        current = {"ids": arrays(line), "pet": [], "owner": [], "boat": [], "passenger": []}
        snapshots.append(current)
    elif current:
        for label, key in [("fisher pet", "pet"), ("cat owner", "owner"), ("rider boat", "boat"), ("boat passenger", "passenger")]:
            if "Phase1QA " + label + ": " in line: current[key].extend(arrays(line))

assert snapshots, "No complete QA snapshots found"
baseline = set(snapshots[0]["ids"])
assert len(baseline) == 21, "Expected 15 NPCs + 3 cats + 3 boats"
for index, snapshot in enumerate(snapshots):
    ids = snapshot["ids"]
    assert len(ids) == 21 and set(ids) == baseline, f"Entity identities changed in snapshot {index}"
    for key in ("pet", "owner", "boat", "passenger"):
        assert len(snapshot[key]) == 3 and len(set(snapshot[key])) == 3, f"Missing/duplicate {key} in snapshot {index}"
        assert set(snapshot[key]) == set(snapshots[0][key]), f"Association changed: {key} in snapshot {index}"
    assert set(snapshot["pet"]) == set(ids[15:18]), "Fishers must reference the three cats"
    assert set(snapshot["boat"]) == set(ids[18:21]), "Riders must reference the three boats"
    assert set(snapshot["owner"]) <= set(ids[:15]), "Cat owners must still be present"
    assert set(snapshot["passenger"]) <= set(ids[:15]), "Boat passengers must still be present"

voices = []
active = set()
for line in log.splitlines():
    match = re.search(r"Lived-In music: voice (opened|closed) for ([\da-f-]+); total (\d+)", line)
    if match:
        action, uuid, total = match.groups()
        if action == "opened":
            assert uuid not in active, "Duplicate simultaneous voice for musician " + uuid
            active.add(uuid)
        else:
            assert uuid in active, "Closed an untracked music voice " + uuid
            active.remove(uuid)
        assert len(active) == int(total), "Music voice count disagrees with individual identities"
        voices.append(int(total))
    elif "Lived-In music: cleared " in line and " on world change" in line:
        active.clear()
assert voices and max(voices) == 3 and min(voices) == 0, "Expected three music voices and full cleanup"
assert "Phase1QA ERROR:" not in log, "A scene chunk remained loaded"
unloaded = len(re.findall(r"Phase1QA unloaded chunk", log))
assert unloaded % 8 == 0, "Incomplete scene chunk unload check"
print(json.dumps({"snapshots": len(snapshots), "unchanged_entity_uuids": len(baseline),
                  "unload_revisit_cycles": unloaded // 8, "max_music_voices": max(voices),
                  "music_reached_zero": 0 in voices, "pet_and_passenger_links_unchanged": True}, indent=2))
