package com.medstudy.app

import android.content.Context
import kotlinx.coroutines.launch
import androidx.room.Room
import com.medstudy.core.data.MedMcqaRepository
import com.medstudy.core.data.PyqRepository
import com.medstudy.core.data.RoomLearnerActivityRepository
import com.medstudy.core.data.RoomQuestionCatalogRepository
import com.medstudy.core.database.StudyDatabase

class StudyAppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        StudyDatabase::class.java,
        "medstudy.db",
    ).addMigrations(StudyDatabase.MIGRATION_1_2, StudyDatabase.MIGRATION_2_3, StudyDatabase.MIGRATION_3_4, StudyDatabase.MIGRATION_4_5).build()

    // Production MedMCQA data repository
    val medMcqaRepository = MedMcqaRepository(context.applicationContext)
    val pyqRepository = PyqRepository(context.applicationContext)
    
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
    val learnerActivity = RoomLearnerActivityRepository(database.activityDao(), { account.userId }, account::noteBookmarkRemoved)
    private val roomStudyPlan = com.medstudy.core.data.RoomWeeklyStudyPlanRepository(database)
    /** The plan repository the screens use: a change to the plan settings is also queued for upload to the account. */
    val weeklyStudyPlan: com.medstudy.core.data.WeeklyStudyPlanRepository = object : com.medstudy.core.data.WeeklyStudyPlanRepository by roomStudyPlan {
        override suspend fun savePreferences(preferences: com.medstudy.core.model.StudySchedulePreferences) {
            roomStudyPlan.savePreferences(preferences)
            account.markStudyScheduleChanged()
        }
    }

    /** Outlives any one screen, so a sync started as the app goes to the background can finish. */
    private val appScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    /** Backs up to the account in the background: on launch, after a practice session, and when the app is left. Quietly skipped offline. */
    fun syncSoon() {
        appScope.launch { if (account.isSignedIn) runCatching { account.sync() } }
    }

    init {
        account.studySchedule = { roomStudyPlan.preferences() }
        account.onDifferentAccount = { roomStudyPlan.clear() }
    }
}
