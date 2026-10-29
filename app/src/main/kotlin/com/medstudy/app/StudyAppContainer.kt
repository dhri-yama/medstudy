package com.medstudy.app

import android.content.Context
import androidx.room.Room
import com.medstudy.core.data.MedMcqaRepository
import com.medstudy.core.data.RoomLearnerActivityRepository
import com.medstudy.core.data.RoomQuestionCatalogRepository
import com.medstudy.core.database.StudyDatabase

class StudyAppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        StudyDatabase::class.java,
        "medstudy.db",
    ).addMigrations(StudyDatabase.MIGRATION_1_2, StudyDatabase.MIGRATION_2_3, StudyDatabase.MIGRATION_3_4).build()

    // Production MedMCQA data repository
    val medMcqaRepository = MedMcqaRepository(context.applicationContext)
    
    // Room-based repositories for user data
    val account = SupabaseAccount(context, database.activityDao())
    val entitlements = EntitlementStore(context)
    /** What the app currently believes the user may do; updated from the server and cached for offline use. */
    val entitlementState = kotlinx.coroutines.flow.MutableStateFlow(entitlements.load())

    suspend fun refreshEntitlement() {
        account.fetchEntitlement()?.let { entitlements.save(it); entitlementState.value = it }
    }

    fun clearEntitlement() {
        entitlements.clear()
        entitlementState.value = Entitlement.Unknown
    }

    val billing by lazy { BillingManager(context.applicationContext, account) { refreshEntitlement() } }
    val questionCatalog = RoomQuestionCatalogRepository(database) { account.userId }
    val learnerActivity = RoomLearnerActivityRepository(database.activityDao()) { account.userId }
    val weeklyStudyPlan = com.medstudy.core.data.RoomWeeklyStudyPlanRepository(database)
}
