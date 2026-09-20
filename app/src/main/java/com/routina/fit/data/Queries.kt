package com.routina.fit.data

import com.routina.fit.model.Exercise
import com.routina.fit.model.Measure
import com.routina.fit.model.Routine
import com.routina.fit.model.Session

/** 動作庫依部位分組的一組。[part] 空字串代表未分類 */
data class PartGroup(val part: String, val exercises: List<Exercise>)

/**
 * 依部位分組。組的順序照部位在動作庫裡第一次出現的順序，
 * 所以種子資料的排列就是使用者看到的排列；未分類永遠排最後。
 */
fun groupByPart(exercises: List<Exercise>): List<PartGroup> {
    val byPart = LinkedHashMap<String, MutableList<Exercise>>()
    exercises.forEach { exercise ->
        byPart.getOrPut(exercise.part) { mutableListOf() }.add(exercise)
    }
    val named = byPart.entries.filter { it.key.isNotEmpty() }.map { PartGroup(it.key, it.value) }
    val none = byPart[""]?.let { listOf(PartGroup("", it)) } ?: emptyList()
    return named + none
}

/** 動作庫搜尋。只比名稱與部位，夠用 */
fun searchExercises(exercises: List<Exercise>, query: String): List<Exercise> {
    val q = query.trim()
    if (q.isEmpty()) return exercises
    return exercises.filter { it.name.contains(q, true) || it.part.contains(q, true) }
}

/** 目前在用的部位名稱，編輯動作時當現成的晶片列 */
fun knownParts(exercises: List<Exercise>): List<String> =
    exercises.map { it.part }.filter { it.isNotEmpty() }.distinct()

/**
 * 課表大概要練多久（分鐘），只是給首頁一個概念。
 * 計次動作用一次三秒估，計時動作用目標秒數，再加上每一項的休息。
 */
fun estimateMinutes(routine: Routine, exercises: List<Exercise>): Int {
    val seconds = routine.items.sumOf { item ->
        val measure = exercises.firstOrNull { it.id == item.exerciseId }?.measure ?: Measure.REPS
        val work = if (measure == Measure.SECONDS) item.target else item.target * SECONDS_PER_REP
        work + item.restSec
    }
    return ((seconds + 59) / 60).coerceAtLeast(1)
}

/** 本週有練的日期。今日頁那排格子用 */
fun trainedDates(sessions: List<Session>, dates: List<String>): Set<String> {
    val trained = sessions.map { it.date }.toSet()
    return dates.filter { it in trained }.toSet()
}

/** 最近做過的動作 id，最新的在前。快速計數頁用來排「最近」 */
fun recentExerciseIds(sessions: List<Session>, limit: Int): List<String> =
    sessions.sortedByDescending { it.startedAt }
        .flatMap { session -> session.logs.map { it.exerciseId } }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(limit)

private const val SECONDS_PER_REP = 3
