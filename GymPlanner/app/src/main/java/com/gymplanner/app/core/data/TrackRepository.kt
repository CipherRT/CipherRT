package com.gymplanner.app.core.data

import kotlinx.coroutines.flow.Flow

/** Cardio sessions and body measurements. */
class TrackRepository(db: AppDatabase) {
    private val dao = db.trackDao()

    fun cardio(profileId: Long): Flow<List<CardioSession>> = dao.observeCardio(profileId)
    fun cardioSince(profileId: Long, fromDay: Long): Flow<List<CardioSession>> = dao.observeCardioSince(profileId, fromDay)
    suspend fun getCardio(id: Long) = dao.getCardio(id)
    suspend fun addCardio(c: CardioSession) = dao.insertCardio(c)
    suspend fun updateCardio(c: CardioSession) = dao.updateCardio(c)
    suspend fun deleteCardio(c: CardioSession) = dao.deleteCardio(c)

    fun measurements(profileId: Long): Flow<List<BodyMeasurement>> = dao.observeMeasurements(profileId)
    suspend fun addMeasurement(m: BodyMeasurement) = dao.insertMeasurement(m)
    suspend fun updateMeasurement(m: BodyMeasurement) = dao.updateMeasurement(m)
    suspend fun deleteMeasurement(m: BodyMeasurement) = dao.deleteMeasurement(m)
}
