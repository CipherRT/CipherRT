package com.gymplanner.app.core.data

import kotlinx.coroutines.flow.Flow

class WeightRepository(db: AppDatabase) {
    private val dao = db.weightDao()

    fun entries(profileId: Long): Flow<List<BodyWeightEntry>> = dao.observe(profileId)
    suspend fun get(id: Long) = dao.get(id)
    suspend fun latest(profileId: Long) = dao.latest(profileId)

    suspend fun add(profileId: Long, date: Long, weightKg: Double, note: String = "") =
        dao.insert(BodyWeightEntry(profileId = profileId, date = date, weightKg = weightKg, note = note))

    suspend fun update(entry: BodyWeightEntry) = dao.update(entry)
    suspend fun delete(entry: BodyWeightEntry) = dao.delete(entry)
}
