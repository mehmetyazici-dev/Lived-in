"""Read a closed Java 1.21.1 design save and export its sponge-bounded house.

Uses only Python's standard library. Reads source region files without changing them.
First run with --source WORLD; add --output FILE to export after reviewing the report.
"""
import argparse
import copy
import gzip
import io
import json
from pathlib import Path
import struct
import zlib
from collections import Counter


def read(stream, fmt):
    return struct.unpack('>' + fmt, stream.read(struct.calcsize('>' + fmt)))[0]


def string(stream):
    return stream.read(read(stream, 'H')).decode('utf-8')


def payload(stream, kind):
    if kind in range(1, 7):
        return read(stream, {1: 'b', 2: 'h', 3: 'i', 4: 'q', 5: 'f', 6: 'd'}[kind])
    if kind == 8:
        return string(stream)
    if kind == 9:
        subtype, size = read(stream, 'B'), read(stream, 'i')
        return subtype, [payload(stream, subtype) for _ in range(size)]
    if kind == 10:
        result = {}
        while (subtype := read(stream, 'B')):
            name = string(stream)
            result[name] = (subtype, payload(stream, subtype))
        return result
    if kind in (7, 11, 12):
        fmt = {7: 'b', 11: 'i', 12: 'q'}[kind]
        return [read(stream, fmt) for _ in range(read(stream, 'i'))]
    raise ValueError(f'Unsupported NBT tag {kind}')


def nbt(raw):
    stream = io.BytesIO(raw)
    assert read(stream, 'B') == 10, 'Expected compound root'
    string(stream)
    return payload(stream, 10)


def put_string(value):
    raw = value.encode('utf-8')
    return struct.pack('>H', len(raw)) + raw


def encode(kind, value):
    if kind in range(1, 7):
        return struct.pack('>' + {1: 'b', 2: 'h', 3: 'i', 4: 'q', 5: 'f', 6: 'd'}[kind], value)
    if kind == 8:
        return put_string(value)
    if kind == 9:
        subtype, values = value
        return struct.pack('>Bi', subtype, len(values)) + b''.join(encode(subtype, v) for v in values)
    if kind == 10:
        return b''.join(bytes([t]) + put_string(k) + encode(t, v) for k, (t, v) in value.items()) + b'\0'
    if kind in (7, 11, 12):
        fmt = {7: 'b', 11: 'i', 12: 'q'}[kind]
        return struct.pack('>i', len(value)) + b''.join(struct.pack('>' + fmt, v) for v in value)
    raise ValueError(kind)


def plain(value):
    kind, data = value
    if kind == 10:
        return {k: plain(v) for k, v in data.items()}
    if kind == 9:
        subtype, values = data
        return [plain((subtype, v)) for v in values]
    return data


def chunks(world):
    for path in sorted((world / 'region').glob('*.mca')):
        raw = path.read_bytes()
        for index in range(1024):
            location = int.from_bytes(raw[index * 4:index * 4 + 4], 'big')
            if not location:
                continue
            offset = (location >> 8) * 4096
            length = int.from_bytes(raw[offset:offset + 4], 'big')
            method = raw[offset + 4]
            if method & 128:
                raise ValueError('External chunk files are not supported by this design importer')
            compressed = raw[offset + 5:offset + 4 + length]
            decoder = {1: gzip.decompress, 2: zlib.decompress, 3: lambda x: x}.get(method)
            if decoder is None:
                raise ValueError(f'Unsupported region compression {method}')
            yield nbt(decoder(compressed))


def section_blocks(chunk):
    cx, cz = chunk['xPos'][1] * 16, chunk['zPos'][1] * 16
    for section in chunk.get('sections', (9, (10, [])))[1][1]:
        if 'block_states' not in section:
            continue
        sy = section['Y'][1] * 16
        states = section['block_states'][1]
        palette = states['palette'][1][1]
        bits = max(4, (len(palette) - 1).bit_length())
        per_long = 64 // bits
        data = states.get('data', (12, []))[1]
        for index in range(4096):
            pi = 0 if len(palette) == 1 else ((data[index // per_long] & ((1 << 64) - 1)) >> ((index % per_long) * bits)) & ((1 << bits) - 1)
            yield (cx + index % 16, sy + index // 256, cz + (index // 16) % 16), palette[pi]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--report', type=Path)
    parser.add_argument('--trim-empty-max-x', action='store_true', help='Omit an empty eastern border if markers span 21x20')
    parser.add_argument('--color-variants', action='store_true', help='Export all sixteen wool colors, changing only white beds/carpets')
    args = parser.parse_args()
    source_chunks = list(chunks(args.source))
    sponges = []
    # Only decode mixed sections containing markers, rather than all terrain.
    for chunk in source_chunks:
        relevant = any(any(p['Name'][1] == 'minecraft:sponge' for p in s.get('block_states', (10, {}))[1].get('palette', (9, (10, [])))[1][1])
                       for s in chunk.get('sections', (9, (10, [])))[1][1])
        if relevant:
            sponges.extend(pos for pos, state in section_blocks(chunk) if state['Name'][1] == 'minecraft:sponge')
    assert len(sponges) == 4, f'Expected four sponge corners; found {sponges}'
    xs, ys, zs = [sorted(set(p[i] for p in sponges)) for i in range(3)]
    assert len(xs) == 2 and len(ys) == 1 and len(zs) == 2, f'Corners must share a level: {sponges}'
    bottom = ys[0] + 1
    blocks, entities = {}, {}
    for chunk in source_chunks:
        cx, cz = chunk['xPos'][1] * 16, chunk['zPos'][1] * 16
        if cx > xs[1] or cx + 15 < xs[0] or cz > zs[1] or cz + 15 < zs[0]:
            continue
        for pos, state in section_blocks(chunk):
            if xs[0] <= pos[0] <= xs[1] and zs[0] <= pos[2] <= zs[1] and pos[1] >= bottom:
                if state['Name'][1] not in ('minecraft:air', 'minecraft:cave_air', 'minecraft:void_air'):
                    blocks[pos] = state
        for entity in chunk.get('block_entities', (9, (10, [])))[1][1]:
            pos = tuple(entity[k][1] for k in ('x', 'y', 'z'))
            if pos in blocks:
                entities[pos] = entity
    assert blocks, 'No build above sponge level'
    if args.trim_empty_max_x:
        assert xs[1] - xs[0] == 20 and zs[1] - zs[0] == 19, 'Expected a 21x20 marked area'
        assert not any(p[0] == xs[1] for p in blocks), 'Cannot trim a border containing blocks'
        xs[1] -= 1
    top = max(p[1] for p in blocks)
    assert top - bottom + 1 <= 48, 'Build is taller than a vanilla structure block capture'
    origin = (xs[0], bottom, zs[0])
    local = lambda p: [p[i] - origin[i] for i in range(3)]
    notable = [{'world': list(pos), 'local': local(pos), 'state': plain((10, state))}
               for pos, state in sorted(blocks.items())
               if state['Name'][1].endswith(('_door', '_bed', '_carpet'))]
    report = {'source': str(args.source.resolve()), 'sponges': sorted(sponges), 'origin': origin,
              'size': [xs[1] - xs[0] + 1, top - bottom + 1, zs[1] - zs[0] + 1], 'non_air_blocks': len(blocks),
              'occupied_bounds': [[min(p[i] for p in blocks) for i in range(3)], [max(p[i] for p in blocks) for i in range(3)]],
              'counts': dict(sorted(Counter(s['Name'][1] for s in blocks.values()).items())),
              'notable_blocks': notable,
              'block_entities': [{'local': local(p), 'data': plain((10, e))} for p, e in sorted(entities.items())]}
    print(json.dumps(report, indent=2))
    if args.output:
        assert report['size'][0] == 20 and report['size'][2] == 20, f'Footprint is not 20x20: {report["size"]}'
        palette, lookup, entries = [], {}, []
        for y in range(bottom, top + 1):
            for z in range(zs[0], zs[1] + 1):
                for x in range(xs[0], xs[1] + 1):
                    pos = (x, y, z)
                    state = blocks.get(pos, {'Name': (8, 'minecraft:air')})
                    key = json.dumps(plain((10, state)), sort_keys=True)
                    if key not in lookup:
                        lookup[key] = len(palette); palette.append(state)
                    entry = {'pos': (9, (3, local(pos))), 'state': (3, lookup[key])}
                    if pos in entities:
                        entry['nbt'] = (10, {k: v for k, v in entities[pos].items() if k not in ('x', 'y', 'z', 'keepPacked')})
                    entries.append(entry)
        versions = set(c['DataVersion'][1] for c in source_chunks)
        assert len(versions) == 1, f'Mixed data versions: {versions}'
        structure = {'DataVersion': (3, versions.pop()), 'size': (9, (3, report['size'])),
                     'palette': (9, (10, palette)), 'blocks': (9, (10, entries)), 'entities': (9, (10, []))}
        raw = b'\x0a\0\0' + encode(10, structure)
        assert nbt(raw) == structure, 'NBT round trip failed'
        def write_template(path, root):
            raw = b'\x0a\0\0' + encode(10, root)
            assert nbt(raw) == root, 'NBT round trip failed'
            compressed = gzip.compress(raw, mtime=0)
            if path.exists():
                assert path.read_bytes() == compressed, f'Refusing to replace changed file: {path}'
            else:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(compressed)
        write_template(args.output, structure)
        if args.color_variants:
            colors = ('white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray',
                      'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black')
            for color in colors:
                variant = copy.deepcopy(structure)
                for state in variant['palette'][1][1]:
                    for suffix in ('bed', 'carpet'):
                        if state['Name'][1] == f'minecraft:white_{suffix}':
                            state['Name'] = (8, f'minecraft:{color}_{suffix}')
                write_template(args.output.parent / args.output.stem / f'{color}.nbt', variant)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
