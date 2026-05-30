package com.priceguard.app

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {

    // Ovo je "živ" tok podataka. Kad god se baza promeni, UI se automatski ažurira.
    val allHistory: Flow<List<HistoryItem>> = historyDao.getAllHistoryFlow()

    // Upisuje proizvod u bazu. Ako bar-kod već postoji, menja ga novim podacima.
    suspend fun insert(item: HistoryItem) {
        historyDao.insertHistory(item)
    }

    // Briše stavku po ID-u iz istorije.
    suspend fun delete(id: Int) {
        historyDao.deleteHistoryItem(id)
    }

    // Čisti celu istoriju.
    suspend fun clearAll() {
        historyDao.clearAllHistory()
    }
}