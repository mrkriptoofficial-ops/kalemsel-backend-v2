"""Google Fonts deposundan gerekli fontlari alir; degisken fontlari statik agirliklara cevirir.
Cikti: app/src/main/res/font/*.ttf  (lisans: SIL Open Font License)"""
import glob
import os
import shutil
import sys

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

OUT = "app/src/main/res/font"
os.makedirs(OUT, exist_ok=True)
os.makedirs("app/src/main/assets/licenses", exist_ok=True)


def pick(folder, static_name, var_glob):
    p = os.path.join("gf", "ofl", folder, static_name)
    if os.path.exists(p):
        return p, False
    g = sorted(x for x in glob.glob(os.path.join("gf", "ofl", folder, var_glob)) if "Italic" not in x)
    if g:
        return g[0], True
    print("BULUNAMADI:", folder, static_name, os.listdir(os.path.join("gf", "ofl", folder)))
    sys.exit(1)


def make(folder, family_static, var_glob, weights, prefix):
    for wname, wval in weights:
        src, var = pick(folder, f"{family_static}-{wname}.ttf", var_glob)
        f = TTFont(src)
        if "fvar" in f:
            axes = {a.axisTag: (a.minValue, a.maxValue) for a in f["fvar"].axes}
            lo, hi = axes.get("wght", (wval, wval))
            loc = {"wght": max(lo, min(hi, wval))}
            for tag in axes:
                if tag != "wght":
                    loc[tag] = None  # varsayilan degerde sabitle
            f = instancer.instantiateVariableFont(f, loc)
        dst = os.path.join(OUT, f"{prefix}_{wname.lower()}.ttf")
        f.save(dst)
        print("ok", dst, "(degisken)" if var else "(statik)")
    lic = os.path.join("gf", "ofl", folder, "OFL.txt")
    if os.path.exists(lic):
        shutil.copy(lic, f"app/src/main/assets/licenses/{prefix}_OFL.txt")


make("amiri", "Amiri", "Amiri*.ttf", [("Regular", 400), ("Bold", 700)], "amiri")
make("cormorantgaramond", "CormorantGaramond", "CormorantGaramond*.ttf",
     [("Light", 300), ("Regular", 400), ("Medium", 500), ("SemiBold", 600)], "cormorant")
make("manrope", "Manrope", "Manrope*.ttf",
     [("Regular", 400), ("Medium", 500), ("SemiBold", 600), ("Bold", 700)], "manrope")
print(sorted(os.listdir(OUT)))
# Sayaç rakamlarının titrememesi için tabular rakam (tnum) desteğini doğrula
for fn in ("cormorant_regular.ttf", "manrope_regular.ttf"):
    f = TTFont(os.path.join(OUT, fn))
    tags = sorted({r.FeatureTag for r in f["GSUB"].table.FeatureList.FeatureRecord}) if "GSUB" in f else []
    print(fn, "tnum" in tags, "lnum" in tags, tags)
