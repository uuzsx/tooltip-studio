"""Pack the user's existing pixel sprites, losslessly, into one atlas per style.

Usage: python tools/pack_aurorian.py <path-to-Aurorian-tooltips>
Requires Pillow. The original files are only read.
"""
import json
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
DEFAULTS = RES / 'assets/tooltipstudio/defaults'
TEXTURES = RES / 'assets/tooltipstudio/textures/styles'

def region(x, y, w, h):
    return dict(u=x, v=y, width=w, height=h)

def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def main(source):
    TEXTURES.mkdir(parents=True, exist_ok=True)
    manifest = []
    for folder in sorted(Path(source).iterdir()):
        if not folder.is_dir() or not (folder / (folder.name + '.png')).exists():
            continue
        name = folder.name
        atlas = Image.new('RGBA', (128, 128))
        original = Image.open(folder / (name + '.png')).convert('RGBA')
        size = 10 if name == 'tslat_sword' else 16 if name == 'uncommon' else 18
        border = 2 if name == 'tslat_sword' else 4
        atlas.paste(original.crop((0, 0, size, size)), (0, 0))
        # A dark translucent background lives in the SAME atlas as the frame.
        atlas.putpixel((24, 0), (10, 10, 22, 242))
        decorations = []
        for index, (part, anchor) in enumerate([
            ('top_left', 'TOP_LEFT'), ('top_right', 'TOP_RIGHT'),
            ('bottom_left', 'BOTTOM_LEFT'), ('bottom_right', 'BOTTOM_RIGHT'),
            ('top', 'TOP'), ('bottom', 'BOTTOM')]):
            file = folder / (part + '.png')
            if not file.exists():
                continue
            sprite = Image.open(file).convert('RGBA')
            x, y = (index % 4) * 28, 40 + (index // 4) * 28
            atlas.paste(sprite, (x, y))
            # Ornament sprites sit over the edge, with outward space included in layout.
            dx = -3 if anchor.endswith('LEFT') else 3 if anchor.endswith('RIGHT') else 0
            dy = -4 if anchor.startswith('TOP') else 3
            if anchor == 'TOP':
                dy = 2 - sprite.height
            if anchor == 'BOTTOM':
                dy = sprite.height - 2
            decorations.append(dict(region=region(x, y, *sprite.size), anchor=anchor,
                                    x=dx, y=dy, foreground=True))
        atlas.save(TEXTURES / (name + '.png'))
        style = dict(texture=f'tooltipstudio:textures/styles/{name}.png',
                     textureWidth=128, textureHeight=128,
                     background=region(24, 0, 1, 1),
                     frame=dict(region=region(0, 0, size, size), left=border, top=border,
                                right=border, bottom=border),
                     separator=dict(enabled=True, region=region(0, 0, size, border),
                                    leftCap=border, rightCap=border, inset=0, marginTop=3, marginBottom=4),
                     padding=dict(left=9, top=9, right=9, bottom=9), minWidth=100, maxWidth=320,
                     decorations=decorations)
        write_json(DEFAULTS / 'styles' / (name + '.json'), style)
        manifest.append(name)
    write_json(DEFAULTS / 'styles.json', manifest)
    write_json(DEFAULTS / 'config.json', dict(schemaVersion=1, enabled=True,
        defaultStyle='rare', nbtStyleKey='TooltipStyle', rules=[
            dict(style='legendary', priority=100, items=['minecraft:netherite_*']),
            dict(style='epic', priority=50, rarities=['epic']),
            dict(style='rare', priority=40, rarities=['rare']),
            dict(style='uncommon', priority=30, rarities=['uncommon'])]))
    print(f'Packed {len(manifest)} styles into {TEXTURES}')

if __name__ == '__main__':
    main(sys.argv[1])
