# -*- coding: utf-8 -*-
"""把 Routina Fit 現在的畫面畫成 drawio，一格 = 一個 dp。

尺寸全部照程式裡的實際值算，不是目測：這樣使用者在圖上挪一個東西，
量到幾格就是幾 dp，我照著改就好。
"""
import io, os, html

# ---------- 調色盤（從 Palette.kt 撈出來的實際值） ----------
BG        = "#F5F6F9"   # N.t97  頁面
SURFACE   = "#FFFFFF"   # N.t100 卡片與列
CONTAINER = "#EDEEF1"   # N.t94  格子圖的空格 / 分頁列
PRIMARY   = "#4C589C"   # P.t40
P_CONT    = "#DAE1FF"   # P.t90  進度填色、FAB
ON_SURF   = "#1B1B1D"   # N.t10
ON_VAR    = "#45464D"   # NV.t30
VARIANT   = "#E5E8F0"   # NV.t92 晶片
ERR_CONT  = "#FFD8D2"   # E.t90
ERROR     = "#A62B25"   # E.t40
HEAT      = ["#603700", "#874F00", "#B06900", "#D68729", "#F9A750", "#FFD4AB"]

# ---------- 手機框：使用者的 Pixel 10 Pro XL（1344x2992 @ 480dpi = 448x997dp） ----------
# 上下兩條的高度是從實機截圖量的，不是 M3 規格書上的 24/64/80——
# Pixel 10 Pro XL 的狀態列很高，上方列又是兩行標題，分頁列還包含手勢列。
W, H       = 448, 997
STATUS_H   = 67
APPBAR_H   = 131         # 實機量到內容區從 198dp 開始
NAVBAR_H   = 104         # 80 的分頁列加 24 的手勢列
PAD        = 16          # 內容左右留白
ROW_DEFAULT = 88         # 加一項那些頁面的列高
ROW_MIN    = 72          # 列高下限
ROW_MAX    = 176         # 列高上限
ROW_GAP    = 12
LABEL_BLOCK = 38         # 小標連上下留白
LIST_TOP_PAD = 4
FAB_RESERVE = 72         # 清單底下留給 ＋ 的高度
ROW_R      = 18
CARD_R     = 20
FAB        = 40

CONTENT_W  = W - PAD * 2         # 379
BODY_TOP   = STATUS_H + APPBAR_H # 198
NAV_TOP    = H - NAVBAR_H        # 893

cells = []
_n = [0]


def nid(prefix):
    _n[0] += 1
    return "%s%d" % (prefix, _n[0])


def esc(s):
    return html.escape(s, quote=True)


def box(x, y, w, h, fill, label="", parent="1", radius=0, stroke="none",
        font=13, color=None, align="center", valign="middle", bold=False,
        dashed=False, extra=""):
    style = "rounded=%d;whiteSpace=wrap;html=1;" % (1 if radius else 0)
    if radius:
        style += "absoluteArcSize=1;arcSize=%d;" % radius
    style += "fillColor=%s;strokeColor=%s;" % (fill, stroke)
    if dashed:
        style += "dashed=1;dashPattern=4 4;"
    style += "fontSize=%d;fontColor=%s;align=%s;verticalAlign=%s;" % (
        font, color or ON_SURF, align, valign)
    if bold:
        style += "fontStyle=1;"
    style += extra
    cells.append(
        '<mxCell id="%s" value="%s" style="%s" vertex="1" parent="%s">'
        '<mxGeometry x="%s" y="%s" width="%s" height="%s" as="geometry"/></mxCell>'
        % (nid("b"), esc(label), style, parent, x, y, w, h))


def label(x, y, w, h, text, parent="1", font=13, color=None, align="left",
          bold=False, valign="middle"):
    style = ("text;html=1;strokeColor=none;fillColor=none;whiteSpace=wrap;"
             "fontSize=%d;fontColor=%s;align=%s;verticalAlign=%s;"
             % (font, color or ON_SURF, align, valign))
    if bold:
        style += "fontStyle=1;"
    cells.append(
        '<mxCell id="%s" value="%s" style="%s" vertex="1" parent="1">'
        '<mxGeometry x="%s" y="%s" width="%s" height="%s" as="geometry"/></mxCell>'
        % (nid("t"), esc(text), style, x, y, w, h))


def group(x, y, w, h):
    gid = nid("g")
    cells.append(
        '<mxCell id="%s" value="" style="group" vertex="1" connectable="0" parent="1">'
        '<mxGeometry x="%s" y="%s" width="%s" height="%s" as="geometry"/></mxCell>'
        % (gid, x, y, w, h))
    return gid


# ---------- 畫面的共用零件 ----------

def frame(ox, oy, title):
    """手機外框 + 標題"""
    label(ox, oy - 34, W, 24, title, font=18, bold=True)
    box(ox, oy, W, H, BG, radius=28, stroke="#9AA0B5",
        extra="strokeWidth=2;", label="")
    box(ox, oy, W, STATUS_H + APPBAR_H, SURFACE, radius=28,
        extra="strokeWidth=0;", label="")
    # 圓角只要上面，下緣補一塊蓋掉
    box(ox, oy + STATUS_H + APPBAR_H - 20, W, 20, SURFACE, label="")


def appbar(ox, oy, title, sub=None, back=False, actions=()):
    x = ox + PAD
    if back:
        label(ox + 14, oy + STATUS_H + 18, 28, 28, "←", font=22, align="center")
        x = ox + 54
    label(x, oy + STATUS_H + (10 if sub else 18), 220, 28, title, font=19)
    if sub:
        label(x, oy + STATUS_H + 36, 220, 18, sub, font=12, color=ON_VAR)
    ax = ox + W - PAD
    for text, color, size in reversed(actions):
        ax -= 40
        label(ax, oy + STATUS_H + 18, 40, 28, text, font=size,
              color=color, align="center")


def navbar(ox, oy, selected=0):
    box(ox, oy + NAV_TOP, W, NAVBAR_H, CONTAINER, label="")
    tabs = ["今天", "動作", "紀錄"]
    span = W / 3.0
    for i, name in enumerate(tabs):
        cx = ox + span * i + span / 2
        if i == selected:
            box(cx - 32, oy + NAV_TOP + 14, 64, 32, P_CONT, radius=16, label="")
        box(cx - 12, oy + NAV_TOP + 22, 24, 16, "none", radius=3,
            stroke=ON_SURF if i == selected else ON_VAR, label="")
        label(cx - 40, oy + NAV_TOP + 48, 80, 18, name, font=11,
              color=ON_SURF if i == selected else ON_VAR, align="center")


def day_row(ox, y, name, done, target, progress=0.0, shift=0, reached=False,
            row_h=None):
    """今天清單的一列。[shift] 是往左滑開的距離"""
    ROW_H = row_h or ROW_DEFAULT
    x = ox + PAD - shift
    if progress > 0:
        box(x, y, CONTENT_W * progress, ROW_H, P_CONT, radius=ROW_R, label="")
    box(x, y, CONTENT_W, ROW_H, SURFACE, radius=ROW_R, label="",
        extra="fillColor=none;")
    if progress <= 0:
        box(x, y, CONTENT_W, ROW_H, SURFACE, radius=ROW_R, label="")
    ratio = max(0.0, min(1.0, (ROW_H - ROW_MIN) / float(ROW_MAX - ROW_MIN)))
    label(x + 18, y, 200, ROW_H, name, font=int(16 + ratio * 6))
    label(x + CONTENT_W - 150, y, 100, ROW_H, "%d" % done,
          font=int(24 + ratio * 24), bold=True,
          align="right")
    if target:
        label(x + CONTENT_W - 44, y + 6, 34, ROW_H, "/ %d" % target, font=13,
              color=ON_VAR, align="left")
    if reached:
        label(x + CONTENT_W - 170, y, 30, ROW_H, "達成", font=11, color=PRIMARY)


def heat_grid(inner_w):
    """欄數、格子邊長、整張高度——和程式裡 Heatmap 同一套算法"""
    target, gap = 11.0, 3.0
    weeks = int((inner_w + gap) / (target + gap))
    cell = (inner_w - gap * (weeks - 1)) / weeks
    return weeks, cell, cell * 7 + gap * 6


def heatmap(ox, y, inner_x, inner_w):
    gap = 3.0
    weeks, cell, _ = heat_grid(inner_w)
    gid = group(ox + inner_x, y, inner_w, cell * 7 + gap * 6)
    filled = {(3, 2): 5, (5, 3): 4, (6, 1): 5, (8, 0): 3, (9, 4): 5,
              (11, 2): 2, (12, 5): 4, (14, 1): 5, (15, 3): 3, (17, 0): 4,
              (18, 2): 5, (19, 6): 4, (20, 1): 2, (21, 4): 5, (22, 0): 3,
              (23, 3): 5, (24, 2): 4}
    for c in range(weeks):
        for r in range(7):
            lvl = filled.get((c, r))
            fill = HEAT[lvl] if lvl else CONTAINER
            box(round(c * (cell + gap), 1), round(r * (cell + gap), 1),
                round(cell, 1), round(cell, 1), fill, parent=gid,
                radius=3, label="")
    return cell * 7 + gap * 6


def fab(ox, oy, label_text="＋"):
    x = ox + W / 2 - FAB / 2
    y = oy + NAV_TOP - 16 - FAB
    box(x, y, FAB, FAB, P_CONT, radius=FAB // 2, label=label_text,
        font=20, color=PRIMARY)


def field(ox, y, text, w=None, hint=False):
    w = w or CONTENT_W
    box(ox + PAD, y, w, 56, "none", radius=4, stroke="#79747E", label="")
    label(ox + PAD + 14, y, w - 28, 56, text, font=15,
          color=ON_VAR if hint else ON_SURF)


# =====================================================================
#  1. 今天
# =====================================================================
def card_h_today():
    return 14 + 36 + 10 + heat_grid(CONTENT_W - 28)[2] + 14


def row_h_today(items=3):
    """一列多高——和 TodayScreen.rowHeightFor 同一套算法"""
    header = card_h_today() + LABEL_BLOCK
    free = ((NAV_TOP - BODY_TOP) - LIST_TOP_PAD - header
            - FAB_RESERVE - ROW_GAP * items)
    return max(ROW_MIN, min(ROW_MAX, free / items))


def screen_today(ox, oy, swiped=False):
    frame(ox, oy, "今天（左滑開）" if swiped else "今天")
    appbar(ox, oy, "今天", "10月30日（週五）",
           actions=[("結算", PRIMARY, 15), ("設定", ON_VAR, 12)])

    y = oy + BODY_TOP + LIST_TOP_PAD
    card_h = card_h_today()
    box(ox + PAD, y, CONTENT_W, card_h, SURFACE, radius=CARD_R, label="")
    box(ox + PAD + 14, y + 14, 36, 36, HEAT[2], radius=18, label="火",
        font=13, color="#FFFFFF")
    label(ox + PAD + 60, y + 14, 200, 36, "連續 10 天", font=19)
    heatmap(ox + PAD, y + 60, 14, CONTENT_W - 28)

    y += card_h + ROW_GAP + 16
    label(ox + PAD, y, 200, 21, "今天 85 次", font=14, color=ON_VAR)

    y += (LABEL_BLOCK - 16) + ROW_GAP
    row_h = row_h_today()
    day_row(ox, y, "伏地挺身", 85, 100, progress=0.85,
            shift=100 if swiped else 0, row_h=row_h)
    if swiped:
        bx = ox + W - PAD - 44
        cy = y + row_h / 2 - 22
        box(bx, cy, 44, 44, ERR_CONT, radius=22, label="刪除", font=11,
            color=ERROR)
        box(bx - 52, cy, 44, 44, VARIANT, radius=22, label="編輯", font=11,
            color=ON_VAR)
    day_row(ox, y + row_h + ROW_GAP, "深蹲", 0, 100, row_h=row_h)
    day_row(ox, y + (row_h + ROW_GAP) * 2, "核心", 0, 100, row_h=row_h)

    fab(ox, oy)
    navbar(ox, oy, selected=0)


# =====================================================================
#  2. 加一項
# =====================================================================
def screen_add(ox, oy):
    frame(ox, oy, "加一項")
    appbar(ox, oy, "加一項", back=True)
    y = oy + BODY_TOP + 8
    field(ox, y, "名稱")
    field(ox, y + 62, "目標次數（可留空）", hint=True)
    y += 136
    box(ox + PAD, y, CONTENT_W, 1, "#CAC4D0", stroke="none", label="")
    y += 16
    label(ox + PAD, y, 200, 20, "最近做過", font=14, color=ON_VAR)
    y += 24
    box(ox + PAD, y, CONTENT_W, 64, SURFACE, radius=12, label="")
    label(ox + PAD + 14, y + 10, 200, 22, "伏地挺身", font=15)
    label(ox + PAD + 14, y + 34, 60, 18, "20 次", font=12, color=ON_VAR)
    box(ox + PAD + 76, y + 36, 46, 18, VARIANT, radius=6, label="徒手",
        font=10, color=ON_VAR)
    y += 78
    field(ox, y, "搜尋動作", hint=True)
    y += 68
    label(ox + PAD, y, 200, 20, "全部動作", font=14, color=ON_VAR)
    y += 24
    for i, (nm, meta) in enumerate((("啞鈴三頭肌屈伸", "14 次"),
                                    ("啞鈴後弓步", "16 次"))):
        box(ox + PAD, y + i * 72, CONTENT_W, 64, SURFACE, radius=12, label="")
        label(ox + PAD + 14, y + i * 72 + 10, 220, 22, nm, font=15)
        label(ox + PAD + 14, y + i * 72 + 34, 60, 18, meta, font=12,
              color=ON_VAR)
        box(ox + PAD + 76, y + i * 72 + 36, 46, 18, VARIANT, radius=6,
            label="啞鈴", font=10, color=ON_VAR)
    by = oy + H - 16 - 52
    box(ox + PAD, by, CONTENT_W, 52, PRIMARY, radius=18, label="加入今天",
        font=16, color="#FFFFFF")


# =====================================================================
#  3. 編輯對話框
# =====================================================================
def screen_edit(ox, oy):
    frame(ox, oy, "編輯（左滑 → 編輯）")
    appbar(ox, oy, "今天", "10月30日（週五）",
           actions=[("結算", PRIMARY, 15), ("設定", ON_VAR, 12)])
    navbar(ox, oy, selected=0)
    box(ox, oy, W, H, "#000000", radius=28,
        extra="opacity=40;strokeWidth=0;", label="")
    dx, dy, dw = ox + 40, oy + 190, W - 80
    dh = 24 + 36 + 3 * 62 + 20 + 48 + 16
    box(dx, dy, dw, dh, CONTAINER, radius=28, label="")
    label(dx + 24, dy + 20, 200, 32, "編輯", font=20)
    fy = dy + 64
    for i, (lab, val) in enumerate((("名稱", "伏地挺身"),
                                    ("目標次數（可留空）", "100"),
                                    ("已經做了幾下", "85"))):
        box(dx + 24, fy + i * 62, dw - 48, 54, "none", radius=4,
            stroke=PRIMARY if i == 2 else "#79747E", label="")
        label(dx + 32, fy + i * 62 - 8, 160, 16, lab, font=10,
              color=PRIMARY if i == 2 else ON_VAR)
        label(dx + 38, fy + i * 62, 160, 54, val, font=15)
    label(dx + dw - 160, dy + dh - 56, 60, 32, "取消", font=15, color=PRIMARY,
          align="center")
    label(dx + dw - 84, dy + dh - 56, 60, 32, "儲存", font=15, color=PRIMARY,
          align="center")


# =====================================================================
#  4. 動作
# =====================================================================
def screen_library(ox, oy):
    frame(ox, oy, "動作")
    appbar(ox, oy, "動作庫", "24 個動作")
    y = oy + BODY_TOP + 8
    field(ox, y, "搜尋動作", hint=True)
    y += 68
    chips = [("全部", True), ("徒手", False), ("啞鈴", False), ("椅凳", False)]
    cx = ox + PAD
    for name, on in chips:
        w = 28 + len(name) * 14
        box(cx, y, w, 32, P_CONT if on else "none", radius=8,
            stroke="none" if on else "#79747E", label=name, font=12,
            color=ON_SURF if on else ON_VAR)
        cx += w + 8
    y += 48
    for part, items in (("胸", ("伏地挺身", "窄握啞鈴臥推")),
                        ("腿", ("啞鈴後弓步", "啞鈴踏凳"))):
        label(ox + PAD, y, 200, 24, part, font=15)
        label(ox + W - PAD - 40, y, 32, 24, "2", font=12, color=ON_VAR,
              align="right")
        label(ox + W - PAD - 20, y, 20, 24, "v", font=13, color=ON_VAR)
        y += 30
        for nm in items:
            box(ox + PAD, y, CONTENT_W, 64, SURFACE, radius=12, label="")
            label(ox + PAD + 14, y + 10, 220, 22, nm, font=15)
            label(ox + PAD + 14, y + 34, 60, 18, "12 次", font=12,
                  color=ON_VAR)
            label(ox + W - PAD - 26, y, 20, 64, "···", font=15, color=ON_VAR,
                  align="center")
            y += 70
        y += 8
    fab(ox, oy)
    navbar(ox, oy, selected=1)


# =====================================================================
#  5. 紀錄
# =====================================================================
def screen_history(ox, oy):
    frame(ox, oy, "紀錄")
    appbar(ox, oy, "紀錄", "共 12 次")
    y = oy + BODY_TOP + 8
    for date, rows in (("10月30日（週五）", (("每日清單", "00:42　·　0 分 · 85 次"),)),
                       ("10月29日（週四）", (("每日清單", "23:30　·　1 分 · 28 次"),)),
                       ("10月20日（週二）", (("居家啞鈴 Day1", "01:46　·　25 分 · 18 次"),))):
        label(ox + PAD, y, 260, 24, date, font=14, color=ON_VAR)
        y += 28
        for name, meta in rows:
            box(ox + PAD, y, CONTENT_W, 62, SURFACE, radius=12, label="")
            label(ox + PAD + 14, y + 8, 240, 22, name, font=15)
            label(ox + PAD + 14, y + 32, 260, 18, meta, font=12, color=ON_VAR)
            label(ox + W - PAD - 26, y, 20, 62, "···", font=15, color=ON_VAR,
                  align="center")
            y += 70
        y += 6
    navbar(ox, oy, selected=2)


# =====================================================================
#  6. 設定
# =====================================================================
def screen_settings(ox, oy):
    frame(ox, oy, "設定")
    appbar(ox, oy, "設定", back=True)
    y = oy + BODY_TOP + 16
    box(ox + PAD, y, CONTENT_W, 48, "none", radius=24, stroke="#79747E",
        label="每天的預設清單", font=15, color=PRIMARY)
    y += 68
    for name in ("達成目標時提示音", "達成目標時震動"):
        label(ox + PAD, y, 260, 40, name, font=16)
        box(ox + W - PAD - 52, y + 6, 52, 30, PRIMARY, radius=15, label="")
        box(ox + W - PAD - 28, y + 9, 24, 24, "#FFFFFF", radius=12, label="")
        y += 56
    y += 8
    box(ox + PAD, y, CONTENT_W, 1, "#CAC4D0", stroke="none", label="")
    y += 18
    label(ox + PAD, y, 200, 24, "備份", font=16)
    y += 30
    label(ox + PAD, y, CONTENT_W, 44,
          "動作庫、每日清單與全部紀錄匯出成一個 JSON 檔。重複匯入同一份不會長出重複資料。",
          font=12, color=ON_VAR, valign="top")
    y += 54
    box(ox + PAD, y, 124, 44, "none", radius=22, stroke="#79747E",
        label="匯出備份", font=14, color=PRIMARY)
    box(ox + PAD + 134, y, 124, 44, "none", radius=22, stroke="#79747E",
        label="匯入備份", font=14, color=PRIMARY)
    y += 60
    box(ox + PAD, y, CONTENT_W, 1, "#CAC4D0", stroke="none", label="")
    y += 18
    label(ox + PAD, y, CONTENT_W, 24,
          "這個 App 只要求震動權限，沒有通知權限、沒有背景服務。", font=12,
          color=ON_VAR)
    label(ox + PAD, y + 30, 200, 24, "版本 0.4.0", font=12, color=ON_VAR)


# =====================================================================
#  7. 每天的預設清單
# =====================================================================
def screen_defaults(ox, oy):
    frame(ox, oy, "每天的預設清單")
    appbar(ox, oy, "每天的預設清單", back=True, actions=[("···", ON_SURF, 17)])
    y = oy + BODY_TOP + 10
    label(ox + PAD, y, CONTENT_W, 24,
          "每天換日時，「今天」會照這一份重新長出來。", font=12, color=ON_VAR)
    y += 34
    for nm in ("伏地挺身", "深蹲", "核心"):
        box(ox + PAD, y, CONTENT_W, 56, SURFACE, radius=14, label="")
        label(ox + PAD + 16, y, 200, 56, nm, font=16)
        label(ox + W - PAD - 110, y, 70, 56, "100 次", font=14, color=ON_VAR,
              align="right")
        label(ox + W - PAD - 26, y, 20, 56, "···", font=15, color=ON_VAR,
              align="center")
        y += 62
    fab(ox, oy)


# ---------- 排版 ----------
X0, Y0 = 40, 150
PITCH_X, PITCH_Y = W + 80, H + 130

plan = [
    (screen_today, dict(swiped=False)),
    (screen_today, dict(swiped=True)),
    (screen_add, {}),
    (screen_edit, {}),
    (screen_library, {}),
    (screen_history, {}),
    (screen_settings, {}),
    (screen_defaults, {}),
]

label(X0, 40, 1200, 34, "Routina Fit v0.4.0　現在的畫面", font=26, bold=True)
label(X0, 80, 1600, 48,
      "一格 = 1dp。手機框 448 × 997 dp＝你的 Pixel 10 Pro XL（1344 × 2992 @ 480dpi）。"
      "顏色是 App 裡實際在用的值。\n"
      "要改就直接在這一頁上拖、改字、加框——量得到幾 dp 我就照著做幾 dp。",
      font=14, color=ON_VAR, valign="top")

SPEC = [
    ("左右留白", "16"),
    ("清單一列高", "%d（依螢幕算，72–176）" % round(row_h_today())),
    ("列與列間距", "12"),
    ("列圓角", "18"), ("列內左右留白", "18"), ("卡片圓角", "20"),
    ("卡片內留白", "14"), ("格子邊長", "11"), ("格子間距", "3"),
    ("格子欄數", "27（由寬度算出）"), ("FAB 直徑", "40"),
    ("FAB 距分頁列", "16"), ("清單底部預留", "72"),
    ("狀態列＋上方列", "198（實機量的）"),
    ("分頁列＋手勢列", "104"),
    ("動作名字級", "%dsp（16–22）"
     % int(16 + (row_h_today() - ROW_MIN) / (ROW_MAX - ROW_MIN) * 6)),
    ("次數字級", "%dsp 粗（24–48）"
     % int(24 + (row_h_today() - ROW_MIN) / (ROW_MAX - ROW_MIN) * 24)),
    ("目標字級", "13sp"), ("小標字級", "14sp"),
]
sx = X0 + 4 * PITCH_X
label(sx, Y0 - 34, 300, 24, "尺寸（單位 dp）", font=18, bold=True)
box(sx, Y0, 300, 28 + len(SPEC) * 26, SURFACE, radius=12, stroke="#9AA0B5",
    label="")
for i, (k, v) in enumerate(SPEC):
    label(sx + 16, Y0 + 14 + i * 26, 170, 24, k, font=13, color=ON_VAR)
    label(sx + 190, Y0 + 14 + i * 26, 100, 24, v, font=13, bold=True)

for i, (fn, kw) in enumerate(plan):
    ox = X0 + (i % 4) * PITCH_X
    oy = Y0 + (i // 4) * PITCH_Y
    fn(ox, oy, **kw)

# ---------- 輸出：保留使用者原本那一頁 ----------
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
target = r"C:\Users\User\Desktop\SideProject\routina-fit\FIT.DRAWIO"
original = io.open(target, encoding="utf-8").read()

# 只取第一頁。用 rindex 會把上一次產的頁也當成「原本的」一起留下來，
# 每跑一次就多一頁。
start = original.index("<diagram")
end = original.index("</diagram>", start) + len("</diagram>")
old_page = original[start:end]
# 原本那頁改個名字，內容一個字都不動
old_page = old_page.replace('name="Page-1"', 'name="原始草稿（我畫的）"', 1)

new_page = (
    '<diagram id="fit-v040-screens" name="現在的畫面 v0.4.0">'
    '<mxGraphModel dx="2400" dy="1400" grid="1" gridSize="10" guides="1" '
    'tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" '
    'pageWidth="2400" pageHeight="1800" math="0" shadow="0">'
    '<root><mxCell id="0"/><mxCell id="1" parent="0"/>'
    + "".join(cells) +
    '</root></mxGraphModel></diagram>'
)

out = '<mxfile host="65bd71144e">' + old_page + new_page + "</mxfile>"
io.open(target, "w", encoding="utf-8", newline="\n").write(out)
print("wrote %s  (%d cells)" % (target, len(cells)))
