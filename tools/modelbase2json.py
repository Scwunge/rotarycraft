"""Converts the original's Techne-generated ModelBase classes into box lists the client draws with vanilla ModelParts
(client/ReikaModel), keeping each part's texture offset, boxes, rotation point, rotation and mirror flag.

usage: python tools/modelbase2json.py <ModelName>[:out_name] [...]   (looked up under reference/RotaryCraft/Models)
writes src/main/resources/assets/rotarycraft/reika_models/<model_name>.json
"""
import glob
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import modelanim

OUT = 'src/main/resources/assets/rotarycraft/reika_models'
NUM = r'(-?[0-9.]+)[Ff]?'


def snake(name):
    name = re.sub(r'^Model', '', name)
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()


def num(s):
    return float(s.rstrip('Ff'))


def convert(name, out_name=None):
    paths = glob.glob('reference/RotaryCraft/**/%s.java' % name, recursive=True)
    if not paths:
        raise SystemExit('no model ' + name)
    src = open(paths[0], encoding='utf-8', errors='replace').read()
    own_src = src
    # a model may take its parts, or its texture size, from the class it extends
    cls_src = src
    for _ in range(4):
        parent = re.search(r'class\s+\w+\s+extends\s+(\w+)', cls_src)
        found = glob.glob('reference/RotaryCraft/**/%s.java' % parent.group(1), recursive=True) if parent else []
        if not found:
            break
        cls_src = open(found[0], encoding='utf-8', errors='replace').read()
        src = src + chr(10) + cls_src
    tw = int(re.search(r'textureWidth\s*=\s*(\d+)', src).group(1))
    th = int(re.search(r'textureHeight\s*=\s*(\d+)', src).group(1))
    parts = {}
    for m in re.finditer(r'(\w+)\s*=\s*new\s+\w+\(\s*this\s*,\s*(-?\d+)\s*,\s*(-?\d+)\s*\)', src):
        parts[m.group(1)] = {'uv': [int(m.group(2)), int(m.group(3))], 'mirror': False, 'boxes': [], 'pivot': [0, 0, 0], 'rotation': [0, 0, 0]}
    for m in re.finditer(r'(\w+)\.addBox\(\s*%s\s*,\s*%s\s*,\s*%s\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)' % (NUM, NUM, NUM), src):
        if m.group(1) not in parts:
            continue
        parts[m.group(1)]['boxes'].append([num(m.group(i)) for i in range(2, 8)])
    for m in re.finditer(r'(\w+)\.setRotationPoint\(\s*%s\s*,\s*%s\s*,\s*%s\s*\)' % (NUM, NUM, NUM), src):
        if m.group(1) not in parts:
            continue
        parts[m.group(1)]['pivot'] = [num(m.group(i)) for i in range(2, 5)]
    for m in re.finditer(r'(\w+)\.mirror\s*=\s*(true|false)', src):
        if m.group(1) not in parts:
            continue
        parts[m.group(1)]['mirror'] = m.group(2) == 'true'
    for m in re.finditer(r'setRotation\(\s*(\w+)\s*,\s*%s\s*,\s*%s\s*,\s*%s\s*\)' % (NUM, NUM, NUM), src):
        if m.group(1) not in parts:
            continue
        parts[m.group(1)]['rotation'] = [num(m.group(i)) for i in range(2, 5)]
    for m in re.finditer(r'(\w+)\.rotateAngle([XYZ])\s*=\s*%s\s*;' % NUM, src):
        if m.group(1) not in parts:
            continue
        parts[m.group(1)]['rotation']['XYZ'.index(m.group(2))] = num(m.group(3))
    def load_class(cls):
        found = glob.glob('reference/RotaryCraft/**/%s.java' % cls, recursive=True)
        return open(found[0], encoding='utf-8', errors='replace').read() if found else None

    ops, warnings = modelanim.program_for(own_src, load_class)
    for warning in warnings:
        print('  %s: %s' % (name, warning))
    doc = {'texture_size': [tw, th], 'parts': parts}
    if ops:
        doc['anim'] = ops
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, (out_name or snake(name)) + '.json')
    with open(out, 'w', newline='\n') as f:
        json.dump(doc, f, indent=1)
        f.write('\n')
    return out, len(parts)


if __name__ == '__main__':
    for n in sys.argv[1:]:
        model, _, out_name = n.partition(':')
        print(*convert(model, out_name or None))
