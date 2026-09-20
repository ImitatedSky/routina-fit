package com.routina.fit.data

import com.routina.fit.model.FitBackup
import kotlinx.serialization.json.Json

/**
 * 備份檔的編碼與驗證。合併寫在 [FitRepository.applyBackup]，這裡只負責「讀得懂嗎、收不收」。
 */
object BackupCodec {

    /** 目前支援的備份格式版本。比這個新就不收，免得靜靜漏掉不認得的欄位 */
    const val SCHEMA_VERSION = 1

    private const val APP_ID = "fit"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    enum class Reject { UNREADABLE, NOT_FIT, NEWER_SCHEMA }

    sealed interface Parsed {
        data class Ok(val backup: FitBackup) : Parsed
        data class Failed(val reason: Reject) : Parsed
    }

    fun encode(backup: FitBackup): String = json.encodeToString(FitBackup.serializer(), backup)

    fun parse(text: String): Parsed {
        val backup = try {
            json.decodeFromString(FitBackup.serializer(), text)
        } catch (t: Throwable) {
            return Parsed.Failed(Reject.UNREADABLE)
        }
        if (backup.app != APP_ID) return Parsed.Failed(Reject.NOT_FIT)
        if (backup.schemaVersion > SCHEMA_VERSION) return Parsed.Failed(Reject.NEWER_SCHEMA)
        return Parsed.Ok(backup)
    }

    fun defaultFileName(): String = "fit-backup-" + compactToday() + ".json"
}
