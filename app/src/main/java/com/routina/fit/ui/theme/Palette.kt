package com.routina.fit.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 從家族種子色 #4B5699 算出來的完整色階，不是手挑的幾個值。
 *
 * 產生方式：在 Oklch 裡固定色相與彩度、只變明度，超出 sRGB 就把彩度收回來。
 * 選 Oklch 不選 CIELAB 是因為藍色系在 Lab 提亮會往紫偏，而家族色正好是靛藍。
 * 色調編號沿用 M3 的定義（= CIE L*），所以下面的 t40 / t90 可以直接對上 M3 的角色表。
 *
 * **整套都要填滿**：Material 的 ColorScheme 只要有角色沒給值，就會沿用內建的紫色基線，
 * 於是分頁列、晶片、進度條軌道會冒出與家族色無關的紫——v0.2.0 就是這樣。
 */

/** 主色：家族靛藍 */
internal object P {
    val t0 = Color(0xFF000002)
    val t4 = Color(0xFF08003A)
    val t6 = Color(0xFF0B0343)
    val t10 = Color(0xFF110E4D)
    val t12 = Color(0xFF141452)
    val t17 = Color(0xFF1C205F)
    val t20 = Color(0xFF222767)
    val t22 = Color(0xFF262C6C)
    val t24 = Color(0xFF2A3171)
    val t30 = Color(0xFF363F81)
    val t40 = Color(0xFF4C589C)
    val t50 = Color(0xFF6471B7)
    val t60 = Color(0xFF7D8BD4)
    val t70 = Color(0xFF96A6F1)
    val t80 = Color(0xFFB5C3FF)
    val t87 = Color(0xFFCFD8FF)
    val t90 = Color(0xFFDAE1FF)
    val t92 = Color(0xFFE1E7FF)
    val t94 = Color(0xFFE9EDFF)
    val t95 = Color(0xFFECF0FF)
    val t96 = Color(0xFFF0F3FF)
    val t97 = Color(0xFFF4F6FF)
    val t98 = Color(0xFFF7F9FF)
    val t99 = Color(0xFFFBFCFF)
    val t100 = Color(0xFFFFFFFF)
}

/** 次要色：同色相、低彩度，用在不搶戲的容器 */
internal object S {
    val t0 = Color(0xFF000001)
    val t4 = Color(0xFF0A0C1E)
    val t6 = Color(0xFF0E1224)
    val t10 = Color(0xFF161A2D)
    val t12 = Color(0xFF1A1E31)
    val t17 = Color(0xFF24293C)
    val t20 = Color(0xFF2B2F43)
    val t22 = Color(0xFF2F3348)
    val t24 = Color(0xFF33384C)
    val t30 = Color(0xFF41455B)
    val t40 = Color(0xFF585D74)
    val t50 = Color(0xFF70768D)
    val t60 = Color(0xFF8A90A8)
    val t70 = Color(0xFFA4AAC3)
    val t80 = Color(0xFFBFC5DF)
    val t87 = Color(0xFFD2D9F3)
    val t90 = Color(0xFFDBE1FC)
    val t92 = Color(0xFFE1E7FF)
    val t94 = Color(0xFFE9EDFF)
    val t95 = Color(0xFFECF0FF)
    val t96 = Color(0xFFF0F3FF)
    val t97 = Color(0xFFF4F6FF)
    val t98 = Color(0xFFF7F9FF)
    val t99 = Color(0xFFFBFCFF)
    val t100 = Color(0xFFFFFFFF)
}

/** 第三色：色相 +60，只有少數強調處用得到 */
internal object T {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF1B0417)
    val t6 = Color(0xFF21081D)
    val t10 = Color(0xFF2B1026)
    val t12 = Color(0xFF2F142A)
    val t17 = Color(0xFF3A1F35)
    val t20 = Color(0xFF41253C)
    val t22 = Color(0xFF462A40)
    val t24 = Color(0xFF4B2E45)
    val t30 = Color(0xFF593B53)
    val t40 = Color(0xFF72536B)
    val t50 = Color(0xFF8C6B84)
    val t60 = Color(0xFFA6849E)
    val t70 = Color(0xFFC29FB9)
    val t80 = Color(0xFFDEBAD5)
    val t87 = Color(0xFFF2CDE9)
    val t90 = Color(0xFFFBD5F2)
    val t92 = Color(0xFFFFDCF6)
    val t94 = Color(0xFFFFE5F8)
    val t95 = Color(0xFFFFE9FA)
    val t96 = Color(0xFFFFEEFB)
    val t97 = Color(0xFFFFF2FC)
    val t98 = Color(0xFFFFF6FD)
    val t99 = Color(0xFFFFFBFE)
    val t100 = Color(0xFFFFFFFF)
}

/** 中性色：帶一點主色色相的灰，不是純灰 */
internal object N {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF0E0E10)
    val t6 = Color(0xFF131315)
    val t10 = Color(0xFF1B1B1D)
    val t12 = Color(0xFF1F1F21)
    val t17 = Color(0xFF292A2C)
    val t20 = Color(0xFF303032)
    val t22 = Color(0xFF343537)
    val t24 = Color(0xFF38393B)
    val t30 = Color(0xFF464749)
    val t40 = Color(0xFF5E5E61)
    val t50 = Color(0xFF767779)
    val t60 = Color(0xFF909093)
    val t70 = Color(0xFFAAABAE)
    val t80 = Color(0xFFC6C6C9)
    val t87 = Color(0xFFD9DADD)
    val t90 = Color(0xFFE1E2E5)
    val t92 = Color(0xFFE7E8EB)
    val t94 = Color(0xFFEDEEF1)
    val t95 = Color(0xFFF0F1F3)
    val t96 = Color(0xFFF3F3F6)
    val t97 = Color(0xFFF5F6F9)
    val t98 = Color(0xFFF8F9FC)
    val t99 = Color(0xFFFBFCFF)
    val t100 = Color(0xFFFFFFFF)
}

/** 中性變體：比中性色多一點彩度，用在分隔與次要文字 */
internal object NV {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF0C0E13)
    val t6 = Color(0xFF121319)
    val t10 = Color(0xFF1A1B21)
    val t12 = Color(0xFF1E1F25)
    val t17 = Color(0xFF282A30)
    val t20 = Color(0xFF2E3037)
    val t22 = Color(0xFF33343B)
    val t24 = Color(0xFF373940)
    val t30 = Color(0xFF45464D)
    val t40 = Color(0xFF5C5E65)
    val t50 = Color(0xFF75777E)
    val t60 = Color(0xFF8E9098)
    val t70 = Color(0xFFA9ABB3)
    val t80 = Color(0xFFC4C6CF)
    val t87 = Color(0xFFD7DAE2)
    val t90 = Color(0xFFE0E2EB)
    val t92 = Color(0xFFE5E8F0)
    val t94 = Color(0xFFEBEDF6)
    val t95 = Color(0xFFEEF0F9)
    val t96 = Color(0xFFF1F3FC)
    val t97 = Color(0xFFF4F6FF)
    val t98 = Color(0xFFF7F9FF)
    val t99 = Color(0xFFFBFCFF)
    val t100 = Color(0xFFFFFFFF)
}

/** 錯誤色。全 App 只有一個意思：這一項沒做滿 */
internal object E {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF250000)
    val t6 = Color(0xFF2E0000)
    val t10 = Color(0xFF3C0001)
    val t12 = Color(0xFF430001)
    val t17 = Color(0xFF560002)
    val t20 = Color(0xFF610003)
    val t22 = Color(0xFF690003)
    val t24 = Color(0xFF710004)
    val t30 = Color(0xFF890307)
    val t40 = Color(0xFFA62B25)
    val t50 = Color(0xFFC3483E)
    val t60 = Color(0xFFE26457)
    val t70 = Color(0xFFFF8072)
    val t80 = Color(0xFFFFAFA4)
    val t87 = Color(0xFFFFCCC5)
    val t90 = Color(0xFFFFD8D2)
    val t92 = Color(0xFFFFE0DB)
    val t94 = Color(0xFFFFE8E4)
    val t95 = Color(0xFFFFECE9)
    val t96 = Color(0xFFFFF0ED)
    val t97 = Color(0xFFFFF3F2)
    val t98 = Color(0xFFFFF7F6)
    val t99 = Color(0xFFFFFBFB)
    val t100 = Color(0xFFFFFFFF)
}

/** 休息綠。與主色同一個明度階，讀起來才是對等的兩個狀態 */
internal object Rest {
    val t0 = Color(0xFF000000)
    val t4 = Color(0xFF001401)
    val t6 = Color(0xFF001A02)
    val t10 = Color(0xFF002304)
    val t12 = Color(0xFF002804)
    val t17 = Color(0xFF013409)
    val t20 = Color(0xFF093B10)
    val t22 = Color(0xFF0F4015)
    val t24 = Color(0xFF154519)
    val t30 = Color(0xFF245327)
    val t40 = Color(0xFF3C6B3E)
    val t50 = Color(0xFF558556)
    val t60 = Color(0xFF6E9F6F)
    val t70 = Color(0xFF88BA89)
    val t80 = Color(0xFFA3D6A4)
    val t87 = Color(0xFFB6EAB7)
    val t90 = Color(0xFFBEF3BF)
    val t92 = Color(0xFFC4F9C4)
    val t94 = Color(0xFFCAFECA)
    val t95 = Color(0xFFD2FFD2)
    val t96 = Color(0xFFDCFFDC)
    val t97 = Color(0xFFE5FFE5)
    val t98 = Color(0xFFEEFFEE)
    val t99 = Color(0xFFF7FFF6)
    val t100 = Color(0xFFFFFFFF)
}

/** 訓練量。格子圖的深淺與那團火共用，等距的五階明度梯 */
internal object Heat {
    val t28 = Color(0xFF603700)
    val t40 = Color(0xFF874F00)
    val t52 = Color(0xFFB06900)
    val t64 = Color(0xFFD68729)
    val t76 = Color(0xFFF9A750)
    val t88 = Color(0xFFFFD4AB)
}