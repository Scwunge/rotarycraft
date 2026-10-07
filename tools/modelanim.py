"""Reads the render program out of one of the original's ModelBase classes (its renderAll method: the GL translates, rotates and part draws,
with the parts' angles in terms of phi and theta) and writes it as a list of operations the client replays (see ReikaModel.renderAnimated).

Operations (JSON lists):
  ["t", x, y, z]                      translate
  ["r", c, a, b, x, y, z]             rotate by c + a*phi + b*theta degrees about the axis (x, y, z)
  ["s", x, y, z]                      scale
  ["p", "ShapeName"]                  draw a part
  ["push"] / ["pop"]                  save / restore the pose
  ["if", flag, true|false] ... ["end"]  the operations in between only if the model's flag number is (or is not) set
Flags are the booleans the original's renderers hand over in the list argument (li.get(n)). Anything this does not understand is skipped
with a warning, so a machine can still be drawn from its parts alone.
"""
import re
import sys

NUM = r'(?:[0-9]+\.[0-9]*|\.[0-9]+|[0-9]+)(?:[eE][-+]?[0-9]+)?'


class Lin:
    """c + a*phi + b*theta"""

    def __init__(self, c=0.0, a=0.0, b=0.0):
        self.c, self.a, self.b = c, a, b

    def is_const(self):
        return self.a == 0 and self.b == 0

    def __add__(self, o):
        return Lin(self.c + o.c, self.a + o.a, self.b + o.b)

    def __sub__(self, o):
        return Lin(self.c - o.c, self.a - o.a, self.b - o.b)

    def __neg__(self):
        return Lin(-self.c, -self.a, -self.b)

    def __mul__(self, o):
        if o.is_const():
            return Lin(self.c * o.c, self.a * o.c, self.b * o.c)
        if self.is_const():
            return o * self
        raise ValueError('non-linear product')

    def __truediv__(self, o):
        if not o.is_const() or o.c == 0:
            raise ValueError('non-constant divisor')
        return Lin(self.c / o.c, self.a / o.c, self.b / o.c)


class Expr:
    def __init__(self, text, env):
        self.toks = re.findall(r'[A-Za-z_][A-Za-z_0-9]*|' + NUM + r'|[-+*/()]', text)
        self.i = 0
        self.env = env

    def peek(self):
        return self.toks[self.i] if self.i < len(self.toks) else None

    def take(self):
        t = self.toks[self.i]
        self.i += 1
        return t

    def parse(self):
        v = self.add()
        if self.peek() is not None:
            raise ValueError('trailing ' + self.peek())
        return v

    def add(self):
        v = self.mul()
        while self.peek() in ('+', '-'):
            op = self.take()
            r = self.mul()
            v = v + r if op == '+' else v - r
        return v

    def mul(self):
        v = self.unary()
        while self.peek() in ('*', '/'):
            op = self.take()
            r = self.unary()
            v = v * r if op == '*' else v / r
        return v

    def unary(self):
        t = self.peek()
        if t == '-':
            self.take()
            return -self.unary()
        if t == '+':
            self.take()
            return self.unary()
        return self.atom()

    def atom(self):
        t = self.take()
        if t == '(':
            # a cast like (float) or (double) or (int)
            if self.peek() in ('float', 'double', 'int') and self.toks[self.i + 1] == ')':
                self.i += 2
                return self.unary()
            v = self.add()
            if self.take() != ')':
                raise ValueError('missing )')
            return v
        if re.fullmatch(NUM, t):
            return Lin(float(t))
        if t == 'phi':
            return Lin(0, 1, 0)
        if t == 'theta':
            return Lin(0, 0, 1)
        if t in self.env:
            return self.env[t]
        raise ValueError('unknown name ' + t)


def evaluate(text, env):
    text = text.strip()
    text = re.sub(r'(?<=[0-9.])[fFdD]\b', '', text)
    return Expr(text, env).parse()


def split_args(s):
    out, depth, cur = [], 0, ''
    for ch in s:
        if ch == ',' and depth == 0:
            out.append(cur)
            cur = ''
            continue
        if ch in '([':
            depth += 1
        if ch in ')]':
            depth -= 1
        cur += ch
    out.append(cur)
    return [a.strip() for a in out]


def find_block(src, start):
    """The text of the {...} block whose opening brace is at or after start, and the index after it."""
    i = src.index('{', start)
    depth = 0
    for j in range(i, len(src)):
        if src[j] == '{':
            depth += 1
        elif src[j] == '}':
            depth -= 1
            if depth == 0:
                return src[i + 1:j], j + 1
    raise ValueError('unbalanced')


def find_method(src, name, args=None):
    """The body of the method with that name (the one with the most parameters if several)."""
    best = None
    for m in re.finditer(r'(?:public|protected|private)?\s*(?:final\s+)?void\s+' + name + r'\s*\(([^)]*)\)\s*\{', src):
        n = len([a for a in m.group(1).split(',') if a.strip()])
        if args is not None and n != args:
            continue
        body, _ = find_block(src, m.end() - 1)
        if best is None or n > best[0]:
            best = (n, body)
    return best[1] if best else None


class Program:
    def __init__(self, load_class):
        self.ops = []
        self.load_class = load_class
        self.warnings = []
        self.flags = {}

    def run(self, cls_src, cls_name):
        body = find_method(cls_src, 'renderAll', 4) or find_method(cls_src, 'renderAll')
        if body is None:
            parent = re.search(r'class\s+\w+\s+extends\s+(\w+)', cls_src)
            if parent:
                psrc = self.load_class(parent.group(1))
                if psrc:
                    return self.run(psrc, parent.group(1))
            return False
        self.exec_body(body, cls_src, {}, 0)
        return True

    def exec_body(self, body, cls_src, env, depth):
        if depth > 6:
            return
        # strip comments
        body = re.sub(r'//[^\n]*', '', body)
        body = re.sub(r'/\*.*?\*/', '', body, flags=re.S)
        self.exec_stmts(body, cls_src, env, depth)

    def exec_stmts(self, text, cls_src, env, depth):
        i = 0
        n = len(text)
        while i < n:
            while i < n and text[i] in ' \t\r\n;':
                i += 1
            if i >= n:
                break
            if text.startswith('if', i) and re.match(r'if\s*\(', text[i:]):
                j = text.index('(', i)
                k, d = j, 0
                while True:
                    if text[k] == '(':
                        d += 1
                    elif text[k] == ')':
                        d -= 1
                        if d == 0:
                            break
                    k += 1
                cond = text[j + 1:k]
                k += 1
                while text[k] in ' \t\r\n':
                    k += 1
                if text[k] == '{':
                    block, after = find_block(text, k)
                else:
                    e = text.index(';', k)
                    block, after = text[k:e + 1], e + 1
                flag = self.condition(cond, env)
                if flag is not None:
                    self.ops.append(['if', flag[0], flag[1]])
                self.exec_stmts(block, cls_src, env, depth)
                if flag is not None:
                    self.ops.append(['end'])
                # else branches are skipped
                m = re.match(r'\s*else\b', text[after:])
                if m:
                    p = after + m.end()
                    while text[p] in ' \t\r\n':
                        p += 1
                    if text[p] == '{':
                        _, after = find_block(text, p)
                    elif text.startswith('if', p):
                        # else if: skip the whole chain by running the statement parser on a dummy
                        e = text.index(';', p)
                        after = e + 1
                    else:
                        after = text.index(';', p) + 1
                i = after
                continue
            if text.startswith('for', i) and re.match(r'for\s*\(', text[i:]):
                self.warnings.append('for loop skipped')
                j = text.index('(', i)
                k, d = j, 0
                while True:
                    if text[k] == '(':
                        d += 1
                    elif text[k] == ')':
                        d -= 1
                        if d == 0:
                            break
                    k += 1
                k += 1
                while text[k] in ' \t\r\n':
                    k += 1
                if text[k] == '{':
                    _, i = find_block(text, k)
                else:
                    i = text.index(';', k) + 1
                continue
            e = i
            depth_p = 0
            while e < n and not (text[e] == ';' and depth_p == 0):
                if text[e] == '(':
                    depth_p += 1
                elif text[e] == ')':
                    depth_p -= 1
                e += 1
            stmt = text[i:e].strip()
            i = e + 1
            if stmt:
                self.stmt(stmt, cls_src, env, depth)

    def condition(self, cond, env):
        cond = cond.strip()
        neg = False
        if cond.startswith('!'):
            neg = True
            cond = cond[1:].strip()
        if re.fullmatch(r'\w+', cond) and cond in self.flags:
            return (self.flags[cond], not neg)
        m = re.fullmatch(r'\(Boolean\)\s*li\.get\((\d+)\)', cond)
        if m:
            return (int(m.group(1)), not neg)
        self.warnings.append('condition not understood: ' + cond)
        return None

    def stmt(self, s, cls_src, env, depth):
        m = re.match(r'(\w+)\.render\s*\(', s)
        if m and m.group(1) not in ('GL11', 'super', 'this'):
            self.ops.append(['p', m.group(1)])
            return
        m = re.match(r'GL11\.glTranslate[fd]\s*\((.*)\)$', s, re.S)
        if m:
            a = split_args(m.group(1))
            try:
                v = [evaluate(x, env) for x in a]
                if all(x.is_const() for x in v):
                    self.ops.append(['t'] + [x.c for x in v])
                else:
                    raise ValueError('animated translate')
            except ValueError as ex:
                self.warnings.append('translate: %s (%s)' % (s, ex))
            return
        m = re.match(r'GL11\.glScale[fd]\s*\((.*)\)$', s, re.S)
        if m:
            a = split_args(m.group(1))
            try:
                v = [evaluate(x, env) for x in a]
                self.ops.append(['s'] + [x.c for x in v])
            except ValueError as ex:
                self.warnings.append('scale: %s (%s)' % (s, ex))
            return
        m = re.match(r'GL11\.glRotate[fd]\s*\((.*)\)$', s, re.S)
        if m:
            a = split_args(m.group(1))
            try:
                ang = evaluate(a[0], env)
                axis = [evaluate(x, env).c for x in a[1:4]]
                self.ops.append(['r', ang.c, ang.a, ang.b] + axis)
            except ValueError as ex:
                self.warnings.append('rotate: %s (%s)' % (s, ex))
            return
        if s.startswith('GL11.glPushMatrix'):
            self.ops.append(['push'])
            return
        if s.startswith('GL11.glPopMatrix'):
            self.ops.append(['pop'])
            return
        m = re.match(r'(?:final\s+)?(?:double|float|int)\s+(\w+)\s*=\s*(.*)$', s, re.S)
        if m:
            try:
                env[m.group(1)] = evaluate(m.group(2), env)
            except ValueError as ex:
                self.warnings.append('variable %s: %s' % (m.group(1), ex))
            return
        m = re.match(r'(?:final\s+)?boolean\s+(\w+)\s*=\s*\(Boolean\)\s*li\.get\((\d+)\)', s)
        if m:
            self.flags[m.group(1)] = int(m.group(2))
            return
        m = re.match(r'super\.renderAll\s*\(', s)
        if m:
            parent = re.search(r'class\s+\w+\s+extends\s+(\w+)', cls_src)
            if parent:
                psrc = self.load_class(parent.group(1))
                if psrc:
                    body = find_method(psrc, 'renderAll', 4) or find_method(psrc, 'renderAll')
                    if body:
                        self.exec_body(body, psrc, dict(env), depth + 1)
            return
        m = re.match(r'(?:this\.)?(\w+)\s*\(', s)
        if m and m.group(1) not in ('setRotation', 'addBox', 'setRotationPoint', 'setTextureSize'):
            body = find_method(cls_src, m.group(1))
            if body is None:
                parent = re.search(r'class\s+\w+\s+extends\s+(\w+)', cls_src)
                psrc = self.load_class(parent.group(1)) if parent else None
                body = find_method(psrc, m.group(1)) if psrc else None
                cls_src = psrc or cls_src
            if body:
                self.exec_body(body, cls_src, dict(env), depth + 1)
                return
        # everything else (field reads, locals of other kinds) is not part of the drawing


def program_for(src, load_class):
    prog = Program(load_class)
    if not prog.run(src, ''):
        return None, ['no renderAll']
    return prog.ops, prog.warnings
