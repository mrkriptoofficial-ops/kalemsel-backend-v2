import math
CX, CY, R, N, GAP = 54.0, 45.5, 20.5, 14, 50.0
seg = (360 - GAP) / N
br = 2 * math.pi * R * seg / 360 * 0.46
beads = []
for i in range(N):
    a = math.radians(90 + GAP / 2 + (i + 0.5) * seg)
    beads.append((CX + R * math.cos(a), CY + R * math.sin(a)))

def circ(x, y, r):
    return f"M{x-r:.2f},{y:.2f} a{r:.2f},{r:.2f} 0 1,0 {2*r:.2f},0 a{r:.2f},{r:.2f} 0 1,0 {-2*r:.2f},0 Z"

# 8 köşeli yıldız (iki kare)
def star(cx, cy, h):
    sq = f"M{cx-h:.2f},{cy-h:.2f} L{cx+h:.2f},{cy-h:.2f} L{cx+h:.2f},{cy+h:.2f} L{cx-h:.2f},{cy+h:.2f} Z"
    d = h * math.sqrt(2)
    di = f"M{cx:.2f},{cy-d:.2f} L{cx+d:.2f},{cy:.2f} L{cx:.2f},{cy+d:.2f} L{cx-d:.2f},{cy:.2f} Z"
    return sq, di

# imame geometrisi
S = 0.82
ix, top = CX, CY + R - 1.0
cap1 = f"M{ix-3.6*S:.2f},{top:.2f} L{ix+3.6*S:.2f},{top:.2f} L{ix+1.8*S:.2f},{top+4.2*S:.2f} L{ix-1.8*S:.2f},{top+4.2*S:.2f} Z"
body_y = top + 3.8*S; bw, bh = 7.6*S, 11.5*S
body = f"M{ix:.2f},{body_y:.2f} C{ix+bw/2+1:.2f},{body_y:.2f} {ix+bw/2:.2f},{body_y+bh:.2f} {ix:.2f},{body_y+bh:.2f} C{ix-bw/2:.2f},{body_y+bh:.2f} {ix-bw/2-1:.2f},{body_y:.2f} {ix:.2f},{body_y:.2f} Z"
c2y = body_y + bh - 0.6
cap2 = f"M{ix-2.4*S:.2f},{c2y:.2f} L{ix+2.4*S:.2f},{c2y:.2f} L{ix+0.8*S:.2f},{c2y+3.4*S:.2f} L{ix-0.8*S:.2f},{c2y+3.4*S:.2f} Z"
ty = c2y + 3.2*S
tassel = " ".join(f"M{ix + t*0.6:.2f},{ty:.2f} L{ix + t*3.0:.2f},{ty+7.6-abs(t)*0.7:.2f}" for t in [-1,-0.66,-0.33,0,0.33,0.66,1])

SILVER = [("0", "#F5F5F7"), ("0.45", "#A3A3AB"), ("0.7", "#E6E6EA"), ("1", "#5E5E66")]
AMBER = [("0", "#F6CF74"), ("0.55", "#D49A2E"), ("1", "#7E4F0C")]

def svg():
    o = ['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="432" height="432"><defs>']
    o.append('<radialGradient id="bg" cx="40" cy="30" r="95" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#2E5299"/><stop offset="0.5" stop-color="#0E1D40"/><stop offset="1" stop-color="#040917"/></radialGradient>')
    o.append('<radialGradient id="glow" cx="54" cy="45.5" r="27" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#F1D9A0" stop-opacity="0.28"/><stop offset="1" stop-color="#F1D9A0" stop-opacity="0"/></radialGradient>')
    o.append('<linearGradient id="gold" x1="40" y1="36" x2="68" y2="64" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#F7E4AA"/><stop offset="0.5" stop-color="#C79A45"/><stop offset="1" stop-color="#8A6424"/></linearGradient>')
    o.append('<linearGradient id="silver" x1="48" y1="70" x2="60" y2="80" gradientUnits="userSpaceOnUse">' + "".join(f'<stop offset="{a}" stop-color="{c}"/>' for a, c in SILVER) + '</linearGradient>')
    for i, (x, y) in enumerate(beads + [(ix - 1.0, body_y + 2.9)]):
        rr = br * 1.45 if i < N else 7.5
        o.append(f'<radialGradient id="b{i}" cx="{x-br*0.3:.2f}" cy="{y-br*0.36:.2f}" r="{rr:.2f}" gradientUnits="userSpaceOnUse">' + "".join(f'<stop offset="{a}" stop-color="{c}"/>' for a, c in AMBER) + '</radialGradient>')
    o.append('</defs><rect width="108" height="108" fill="url(#bg)"/>')
    o.append(f'<path d="{circ(CX, CY, 27)}" fill="url(#glow)"/>')
    o.append(f'<path d="{circ(CX, CY, R)}" fill="none" stroke="#B08A50" stroke-opacity="0.6" stroke-width="0.6"/>')
    sq, di = star(CX, CY, 5.6)
    o.append(f'<path d="{sq}" fill="url(#gold)"/><path d="{di}" fill="url(#gold)"/>')
    o.append(f'<path d="{circ(CX, CY, 2.7)}" fill="#0E1D40"/><path d="{circ(CX, CY, 1.15)}" fill="#F7E4AA"/>')
    for i, (x, y) in enumerate(beads):
        o.append(f'<path d="{circ(x, y+br*0.35, br)}" fill="#000" fill-opacity="0.35"/>')
    for i, (x, y) in enumerate(beads):
        o.append(f'<path d="{circ(x, y, br)}" fill="url(#b{i})"/>')
        o.append(f'<path d="{circ(x-br*0.30, y-br*0.38, br*0.13)}" fill="#FFFFFF" fill-opacity="0.95"/>')
    o.append(f'<path d="{tassel}" stroke="#E0AE4C" stroke-width="0.7" stroke-linecap="round" fill="none"/>')
    o.append(f'<path d="{body}" fill="url(#b{N})"/>')
    o.append(f'<path d="{circ(ix-1.3, body_y+2.5, 0.6)}" fill="#FFFFFF" fill-opacity="0.9"/>')
    o.append(f'<path d="{cap1}" fill="url(#silver)"/><path d="{cap2}" fill="url(#silver)"/>')
    o.append('</svg>')
    return "\n".join(o)

open("icon.svg", "w").write(svg())

# ── Android vektörleri ──
def grad_radial(cx, cy, r, stops):
    s = "".join(f'<item android:offset="{a}" android:color="#FF{c[1:]}"/>' for a, c in stops)
    return f'<aapt:attr name="android:fillColor"><gradient android:type="radial" android:centerX="{cx:.2f}" android:centerY="{cy:.2f}" android:gradientRadius="{rr(r)}">{s}</gradient></aapt:attr>'
def rr(r): return f"{r:.2f}"
def grad_linear(x1, y1, x2, y2, stops):
    s = "".join(f'<item android:offset="{a}" android:color="#FF{c[1:]}"/>' for a, c in stops)
    return f'<aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="{x1}" android:startY="{y1}" android:endX="{x2}" android:endY="{y2}">{s}</gradient></aapt:attr>'
def path(d, fill=None, alpha=None, inner=None, stroke=None, sw=None):
    a = f'<path android:pathData="{d}"'
    if fill: a += f' android:fillColor="{fill}"'
    if alpha: a += f' android:fillAlpha="{alpha}"'
    if stroke: a += f' android:strokeColor="{stroke}" android:strokeWidth="{sw}" android:strokeLineCap="round"'
    if inner: return a + f'>{inner}</path>'
    return a + '/>'

HEAD = '<?xml version="1.0" encoding="utf-8"?>\n<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
bg = HEAD + path("M0,0 L108,0 L108,108 L0,108 Z", inner=grad_radial(40, 30, 95, [("0", "#2E5299"), ("0.5", "#0E1D40"), ("1", "#040917")])) + "\n</vector>\n"
fg = [HEAD]
fg.append(path(circ(CX, CY, R), stroke="#99B08A50", sw="0.6"))
GOLD = [("0", "#F7E4AA"), ("0.5", "#C79A45"), ("1", "#8A6424")]
sq, di = star(CX, CY, 5.6)
fg.append(path(sq, inner=grad_linear(40, 36, 68, 64, GOLD))); fg.append(path(di, inner=grad_linear(40, 36, 68, 64, GOLD)))
fg.append(path(circ(CX, CY, 2.7), fill="#FF0E1D40")); fg.append(path(circ(CX, CY, 1.15), fill="#FFF7E4AA"))
for x, y in beads: fg.append(path(circ(x, y + br * 0.35, br), fill="#FF000000", alpha="0.35"))
for x, y in beads:
    fg.append(path(circ(x, y, br), inner=grad_radial(x - br * 0.3, y - br * 0.36, br * 1.45, AMBER)))
    fg.append(path(circ(x - br * 0.30, y - br * 0.38, br * 0.13), fill="#F2FFFFFF"))
fg.append(path(tassel, stroke="#FFE0AE4C", sw="0.7"))
fg.append(path(body, inner=grad_radial(ix - 1.0, body_y + 2.9, 7.5, AMBER)))
fg.append(path(circ(ix - 1.3, body_y + 2.5, 0.6), fill="#E6FFFFFF"))
fg.append(path(cap1, inner=grad_linear(48, 70, 60, 80, SILVER))); fg.append(path(cap2, inner=grad_linear(48, 70, 60, 80, SILVER)))
fg.append("</vector>\n")
mono = [HEAD.replace('xmlns:aapt="http://schemas.android.com/aapt" ', '')]
for x, y in beads: mono.append(path(circ(x, y, br), fill="#FFFFFFFF"))
mono.append(path(sq, fill="#FFFFFFFF")); mono.append(path(di, fill="#FFFFFFFF"))
mono.append(path(body, fill="#FFFFFFFF")); mono.append(path(cap1, fill="#FFFFFFFF")); mono.append(path(tassel, stroke="#FFFFFFFF", sw="0.7"))
mono.append("</vector>\n")
open("ic_launcher_background.xml", "w").write(bg)
open("ic_launcher_foreground.xml", "w").write("\n".join(fg))
open("ic_launcher_monochrome.xml", "w").write("\n".join(mono))
print("bead r", round(br, 2))
