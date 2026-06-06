package com.example.db

import kotlinx.coroutines.flow.Flow

class DecisionRepository(private val decisionDao: DecisionDao) {
    val allDecisions: Flow<List<DecisionEntity>> = decisionDao.getAllDecisions()

    suspend fun insert(decision: DecisionEntity): Long {
        return decisionDao.insertDecision(decision)
    }

    suspend fun delete(id: Int) {
        decisionDao.deleteDecisionById(id)
    }

    suspend fun toggleFavorite(id: Int, currentStatus: Boolean) {
        decisionDao.updateFavorite(id, !currentStatus)
    }

    suspend fun getById(id: Int): DecisionEntity? {
        return decisionDao.getDecisionById(id)
    }
}
