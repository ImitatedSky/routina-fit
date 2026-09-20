package com.routina.fit

import android.app.Application
import com.routina.fit.data.FitRepository

/**
 * 整個 App 只有一份 [FitRepository]，掛在 Application 上。
 * 資料量小，不需要 DI 框架，畫面端從 ViewModel 取得。
 */
class FitApp : Application() {

    lateinit var repository: FitRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FitRepository(this)
    }
}
