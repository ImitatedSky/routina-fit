# -*- coding: utf-8 -*-
"""從家族種子色算出完整的 M3 色階。

用 Oklch 而不是 CIELAB：藍色系在 Lab 裡提亮會往紫偏（著名的 blue-shift），
Oklch 就是為了修這件事設計的，色相在明度變化時守得住。

色調（tone）沿用 M3 的定義 = CIE L*，所以先把 L* 轉成 Y，再取 Oklab 的 L = Y^(1/3)
（中性色在 Oklab 的 L 剛好就是 Y 的立方根）。
"""
import math

# ---------- sRGB <-> Oklab ----------

def srgb_to_linear(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4

def linear_to_srgb(c):
    return c * 12.92 if c <= 0.0031308 else 1.055 * (c ** (1 / 2.4)) - 0.055

def hex_to_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4))

def rgb_to_oklab(r, g, b):
    r, g, b = srgb_to_linear(r), srgb_to_linear(g), srgb_to_linear(b)
    l = 0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b
    m = 0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b
    s = 0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b
    l_, m_, s_ = l ** (1 / 3), m ** (1 / 3), s ** (1 / 3)
    return (
        0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
        1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_,
        0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_,
    )

def oklab_to_rgb(L, a, b):
    l_ = L + 0.3963377774 * a + 0.2158037573 * b
    m_ = L - 0.1055613458 * a - 0.0638541728 * b
    s_ = L - 0.0894841775 * a - 1.2914855480 * b
    l, m, s = l_ ** 3, m_ ** 3, s_ ** 3
    return (
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
    )

def in_gamut(lin):
    return all(-1e-4 <= c <= 1 + 1e-4 for c in lin)

def oklch_to_hex(L, C, h_deg):
    """超出 sRGB 就二分搜尋把彩度收回來，色相與明度一律保住"""
    h = math.radians(h_deg)
    lo, hi = 0.0, C
    best = (0.0, 0.0, 0.0)
    for _ in range(40):
        mid = (lo + hi) / 2
        lin = oklab_to_rgb(L, mid * math.cos(h), mid * math.sin(h))
        if in_gamut(lin):
            best, lo = lin, mid
        else:
            hi = mid
    if C == 0:
        best = oklab_to_rgb(L, 0, 0)
    rgb = [max(0.0, min(1.0, linear_to_srgb(max(0.0, min(1.0, c))))) for c in best]
    return "#%02X%02X%02X" % tuple(round(c * 255) for c in rgb)

def tone_to_okl(t):
    """M3 的 tone 就是 CIE L*"""
    y = ((t + 16) / 116) ** 3 if t > 8 else t / 903.3
    return y ** (1 / 3)

TONES = [0, 4, 6, 10, 12, 17, 20, 22, 24, 30, 40, 50, 60, 70, 80, 87, 90, 92, 94, 95, 96, 97, 98, 99, 100]

def ramp(hue, chroma):
    return {t: oklch_to_hex(tone_to_okl(t), chroma, hue) for t in TONES}

# ---------- 家族種子 ----------

SEED = "#4B5699"
L, a, b = rgb_to_oklab(*hex_to_rgb(SEED))
seed_c = math.hypot(a, b)
seed_h = math.degrees(math.atan2(b, a)) % 360
print("seed %s -> Oklch  L=%.4f  C=%.4f  h=%.1f" % (SEED, L, seed_c, seed_h))

# M3 的彩度比例（CAM16 48/16/24/4/8）換算到 Oklch，以種子彩度為基準
c_primary = max(seed_c, 0.11)
ramps = {
    "P": ramp(seed_h, c_primary),
    "S": ramp(seed_h, c_primary / 3),
    "T": ramp((seed_h + 60) % 360, c_primary * 0.5),
    # 中性色的彩度用絕對值不用比例：接近白的地方色相很容易被看出來，
    # 照比例算出來的 0.009 會讓整頁像鋪了一層藍（實機看過才調的）
    "N": ramp(seed_h, 0.0040),
    "NV": ramp(seed_h, 0.0120),
    "E": ramp(28.0, 0.16),          # 紅，標準警示色的色相
    "Rest": ramp(145.0, c_primary * 0.8),  # 綠，休息用
}

for name in ("P", "S", "T", "N", "NV", "E", "Rest"):
    row = ramps[name]
    print("\n%-3s " % name + "  ".join("%d:%s" % (t, row[t]) for t in (10, 20, 30, 40, 80, 90, 95, 98)))

# ---------- 產 Kotlin ----------

def obj(name, comment, row):
    lines = ["/** %s */" % comment, "internal object %s {" % name]
    for t in TONES:
        lines.append("    val t%d = Color(0xFF%s)" % (t, row[t].lstrip("#")))
    lines.append("}")
    return "\n".join(lines)

kt = '''package com.routina.fit.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 從家族種子色 %s 算出來的完整色階，不是手挑的幾個值。
 *
 * 產生方式：在 Oklch 裡固定色相與彩度、只變明度，超出 sRGB 就把彩度收回來。
 * 選 Oklch 不選 CIELAB 是因為藍色系在 Lab 提亮會往紫偏，而家族色正好是靛藍。
 * 色調編號沿用 M3 的定義（= CIE L*），所以下面的 t40 / t90 可以直接對上 M3 的角色表。
 *
 * **整套都要填滿**：Material 的 ColorScheme 只要有角色沒給值，就會沿用內建的紫色基線，
 * 於是分頁列、晶片、進度條軌道會冒出與家族色無關的紫——v0.2.0 就是這樣。
 */
''' % SEED

kt += "\n" + obj("P", "主色：家族靛藍", ramps["P"])
kt += "\n\n" + obj("S", "次要色：同色相、低彩度，用在不搶戲的容器", ramps["S"])
kt += "\n\n" + obj("T", "第三色：色相 +60，只有少數強調處用得到", ramps["T"])
kt += "\n\n" + obj("N", "中性色：帶一點主色色相的灰，不是純灰", ramps["N"])
kt += "\n\n" + obj("NV", "中性變體：比中性色多一點彩度，用在分隔與次要文字", ramps["NV"])
kt += "\n\n" + obj("E", "錯誤色。全 App 只有一個意思：這一項沒做滿", ramps["E"])
kt += "\n\n" + obj("Rest", "休息綠。與主色同一個明度階，讀起來才是對等的兩個狀態", ramps["Rest"])

import io
import os
out = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "app/src/main/java/com/routina/fit/ui/theme/Palette.kt".replace("/", os.sep))
io.open(out, "w", encoding="utf-8", newline="\n").write(kt)
print("\nwrote Palette.kt")

# 給 Theme.kt 用的對照表，直接印出來
print("\n--- light ---")
for role, ref in [("primary","P.t40"),("onPrimary","P.t100"),("primaryContainer","P.t90"),("onPrimaryContainer","P.t10"),
                  ("background","N.t97"),("surface","N.t100"),("surfaceVariant","NV.t92"),("onSurfaceVariant","NV.t30")]:
    print("%-20s %s" % (role, ref))
