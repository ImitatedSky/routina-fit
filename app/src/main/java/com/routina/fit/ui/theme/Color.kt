package com.routina.fit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 家族基準色，與 Hub、Flow、Bite 同值，讓幾個 App 放在一起看起來是同一家出的東西。
 */
object FamilyColors {
    val Primary = Color(0xFF4B5699)
    val Secondary = Color(0xFF254C89)
    val Failure = Color(0xFFC62828)
}

/**
 * 訓練狀態色：正在做一種、休息一種。
 *
 * 執行畫面會整片換底色，理由是實用的——手機放在地上一公尺外，
 * 要能瞄一眼就知道現在該動還是該喘。
 *
 * 但「整片」指的是很淡的底色，不是飽和填色：飽和色一頁只留給一個 hero
 * （執行畫面就是那顆主要按鈕）。底色透明度取 8%（亮）／14%（暗），
 * 這個強度在視覺研究裡與 2dp 線條等效，但安靜四倍。
 */
private val Work = Color(0xFFB4543C)
private val WorkDark = Color(0xFFE08066)
private val Rest = Color(0xFF55703F)
private val RestDark = Color(0xFF9FB189)

@Composable
fun workColor(): Color = if (isSystemInDarkTheme()) WorkDark else Work

@Composable
fun restColor(): Color = if (isSystemInDarkTheme()) RestDark else Rest

/** 整頁底色用的透明度。亮色主題要更淡，深色主題底下淡了就看不出來 */
@Composable
fun tintAlpha(): Float = if (isSystemInDarkTheme()) 0.14f else 0.08f
