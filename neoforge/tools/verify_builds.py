"""Audit all six NeoForge builds; optionally verify real-client smoke screenshots (Pillow)."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re
import tomllib
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.search(r"ext.modVersion = '([^']+)'", (ROOT / 'build.gradle').read_text()).group(1)
TARGETS = {'1.21.1': (65, [34, 0]), '1.21.4': (65, [46, 0]), '26.1.1': (69, [84, 0]),
           '26.1.2': (69, [84, 0]), '26.2': (69, [88, 0]), '26.3': (69, [97, 1])}
COLORS = [(238, 85, 68), (102, 221, 68), (68, 170, 255), (221, 85, 238)]


def audit(screenshots=False, selected=None):
    if screenshots:
        from PIL import Image
    records = []
    for mc, (major, pack_format) in TARGETS.items():
        if selected and mc != selected:
            continue
        build = ROOT / 'build' / mc
        jar = build / 'libs' / f'tooltip-studio-neoforge-{VERSION}+{mc}.jar'
        with zipfile.ZipFile(jar) as archive:
            names = archive.namelist()
            assert 'fabric.mod.json' not in names
            assert 'LICENSE' in names
            assert not any('/smoke/' in n or n.endswith('Test.class') for n in names)
            for name in (n for n in names if n.endswith('.class')):
                data = archive.read(name)
                assert int.from_bytes(data[6:8], 'big') == major, name
                assert b'net/fabricmc/' not in data, name
            metadata = tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode())
            assert metadata['license'] == 'MIT'
            assert metadata['mods'][0]['authors'] == '幼幼紫、千村'
            assert metadata['mods'][0]['version'] == VERSION
            deps = {d['modId']: d for d in metadata['dependencies']['tooltipstudio']}
            assert deps['minecraft']['versionRange'] == f'[{mc}]'
            assert deps['neoforge']['side'] == 'CLIENT'
            pngs = [n for n in names if n.endswith('.png')]
            assert sorted(pngs) == ['assets/tooltipstudio/icon.png', 'assets/tooltipstudio/textures/styles/default.png']
            for name in pngs:
                assert archive.read(name) == (ROOT.parent / 'src/main/resources' / name).read_bytes()
        reports = [ET.parse(p).getroot() for p in (build / 'test-results/test').glob('TEST-*.xml')]
        count = sum(int(r.get('tests')) for r in reports)
        assert count == 86, (mc, count)
        assert not any(int(r.get('failures', 0)) or int(r.get('errors', 0)) for r in reports)
        packs = list((build / 'resourcepacks').glob(f'*{VERSION}+{mc}.zip'))
        assert len(packs) == 7, (mc, len(packs))
        for pack in packs:
            with zipfile.ZipFile(pack) as archive:
                metadata = json.loads(archive.read('pack.mcmeta'))['pack']
                if mc.startswith('26.'):
                    assert metadata['min_format'] == metadata['max_format'] == pack_format
                else:
                    assert metadata['pack_format'] == pack_format[0]
                assert 'docs/neoforge.md' in archive.namelist()
                readme = archive.read('README.md').decode()
                assert 'Fabric API' not in readme and 'NeoForge API' not in readme
                assert 'docs/1.21-components.md' not in readme
        if screenshots:
            images = ROOT / 'run-smoke' / mc / 'screenshots'
            frames = []
            for tick in [20, 23, 60]:
                with Image.open(images / f'neoforge-{tick}.png') as img:
                    assert img.size == (1280, 900)
                    pixels = Counter(img.convert('RGB').get_flattened_data())
                # 16 GUI pixels * 1.5 decoration scale * 2 GUI scale = 48 physical pixels.
                frame = next((color for color in COLORS if pixels[color] == 48 * 48), None)
                assert frame is not None, (mc, tick, 'animation missing or incorrectly scaled')
                frames.append(frame)
                if tick == 60:
                    assert pixels[(255, 85, 85)] > 10, (mc, 'red text segment missing')
            assert frames[0] != frames[1], (mc, 'animation did not advance')
            assert (images / 'neoforge-100.png').is_file()
        records.append({'minecraft': mc, 'tests': count, 'packs': len(packs),
                        'jar': jar.name, 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()})
    return records


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--screenshots', action='store_true')
    parser.add_argument('--mc', choices=TARGETS)
    args = parser.parse_args()
    print(json.dumps(audit(args.screenshots, args.mc), indent=2, ensure_ascii=False))
