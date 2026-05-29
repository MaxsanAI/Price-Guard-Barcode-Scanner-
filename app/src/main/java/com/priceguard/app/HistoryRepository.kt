package com.priceguard.app

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistoryFlow()

    suspend fun insert(item: HistoryItem) {
        historyDao.insertHistory(item)
    }

    suspend fun delete(id: Int) {
        historyDao.deleteHistoryItem(id)
    }

    suspend fun clearAll() {
        historyDao.clearAllHistory()
    }
}
