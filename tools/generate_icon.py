# -*- coding: utf-8 -*-
"""Routina Fit 啟動圖示：從 430603.jpg 的設計稿產出所有尺寸。

設計稿上有「Fitness」字樣，這裡**刻意不要**：
launcher 本來就會在圖示下面印 App 名稱，而 48dp 下手寫體會糊成一團。
（Bite 的圖示也是同樣理由拿掉字樣的。）

自適應圖示的前景必須落在 66dp 安全圈內。判斷依據是圖案**實際著墨處的最小外接圓**，
不是外框矩形——這張圖是上寬下窄的形狀，用外框會過度縮小。
"""
import math
from PIL import Image

# 設計稿原檔（不進版控，重做圖示時要用）
SRC = r"C:/Users/User/Downloads/430603.jpg"
RES = r"C:/Users/User/Desktop/SideProject/routina-fit/app/src/main/res"
HUB_ICON = r"C:/Users/User/Desktop/SideProject/routina/icons/fit.png"

CREAM = (252, 248, 239)
# 圖形區（不含字樣），量自設計稿
FIG = (200, 250, 1060, 855)

SS = 4  # 超取樣，邊緣才不會鋸齒


def load_art():
    """裁出圖形、把奶油底去掉，邊緣留半透明"""
    im = Image.open(SRC).convert("RGB").crop(FIG)
    w, h = im.size
    src = im.load()
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dst = out.load()
    for y in range(h):
        for x in range(w):
            p = src[x, y]
            d = max(abs(p[0] - CREAM[0]), abs(p[1] - CREAM[1]), abs(p[2] - CREAM[2]))
            if d <= 10:
                continue
            a = 255 if d >= 40 else int(255 * (d - 10) / 30)
            dst[x, y] = (p[0], p[1], p[2], a)
    return out.crop(out.getbbox())


def enclosing_circle(img):
    """著墨處的最小外接圓。候選圓心在格點上掃一遍就夠準了"""
    w, h = img.size
    a = img.load()
    pts = [(x, y) for y in range(0, h, 2) for x in range(0, w, 2) if a[x, y][3] > 96]
    best = None
    cx0, cy0 = w / 2, h / 2
    step = max(w, h) / 16.0
    while step > 0.5:
        improved = False
        for dx in (-step, 0, step):
            for dy in (-step, 0, step):
                cx, cy = cx0 + dx, cy0 + dy
                r = max(math.hypot(x - cx, y - cy) for x, y in pts)
                if best is None or r < best[0] - 1e-6:
                    best, cx0, cy0 = (r, cx, cy), cx, cy
                    improved = True
        if not improved:
            step /= 2
    return best[0], cx0, cy0


def render(art, size, art_diameter_ratio, bg, mask, out_path):
    """art_diameter_ratio = 圖案外接圓直徑 佔 整張圖邊長 的比例"""
    big = size * SS
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    if bg is not None:
        from PIL import ImageDraw
        d = ImageDraw.Draw(canvas)
        if mask == "round":
            d.ellipse([0, 0, big - 1, big - 1], fill=bg)
        elif mask == "square":
            d.rounded_rectangle([0, 0, big - 1, big - 1], radius=big * 0.18, fill=bg)
        else:
            d.rectangle([0, 0, big, big], fill=bg)

    r, cx, cy = CIRCLE
    scale = (big * art_diameter_ratio / 2.0) / r
    w, h = art.size
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    small = art.resize((nw, nh), Image.LANCZOS)
    # 讓外接圓的圓心落在畫布中心
    left = round(big / 2 - cx * scale)
    top = round(big / 2 - cy * scale)
    canvas.alpha_composite(small, (left, top))
    canvas.resize((size, size), Image.LANCZOS).save(out_path)


def render_mono(art, size, art_diameter_ratio, out_path):
    """主題圖示：只取形狀填黑"""
    big = size * SS
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    r, cx, cy = CIRCLE
    scale = (big * art_diameter_ratio / 2.0) / r
    w, h = art.size
    small = art.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    black = Image.new("RGBA", small.size, (0, 0, 0, 255))
    black.putalpha(small.getchannel("A"))
    canvas.alpha_composite(black, (round(big / 2 - cx * scale), round(big / 2 - cy * scale)))
    canvas.resize((size, size), Image.LANCZOS).save(out_path)


ART = load_art()
CIRCLE = enclosing_circle(ART)
print("art size", ART.size, " 外接圓 r=%.1f 圓心=(%.1f, %.1f)" % CIRCLE)
print("cream", CREAM)

# 自適應前景：432px = 108dp，安全圈 66dp -> 直徑佔 66/108
SAFE = 66.0 / 108.0
render(ART, 432, SAFE, None, None, RES + "/drawable/ic_launcher_foreground.png")
render_mono(ART, 432, SAFE, RES + "/drawable/ic_launcher_monochrome.png")

# 舊版 mipmap：自己帶底色，沒有自適應遮罩，圖案可以大一點
LEGACY = 0.80
for name, pxs in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
    render(ART, pxs, LEGACY, CREAM + (255,), "square", RES + "/mipmap-%s/ic_launcher.png" % name)
    render(ART, pxs, LEGACY, CREAM + (255,), "round", RES + "/mipmap-%s/ic_launcher_round.png" % name)

# Hub 名冊用的 144x144
render(ART, 144, LEGACY, CREAM + (255,), "square", HUB_ICON)
print("icons written")
