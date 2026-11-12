package com.medstudy.app

import com.medstudy.feature.progress.SubjectSummary
import com.medstudy.feature.progress.subjectSummaries
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import com.medstudy.core.designsystem.rememberReducedMotion
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.CompositionLocalProvider
import com.medstudy.core.designsystem.StudyTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Today
import com.medstudy.feature.progress.SessionPlanner
import com.medstudy.feature.progress.WeakArea
import com.medstudy.feature.progress.isDue
import java.time.LocalDate
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medstudy.core.designsystem.BottomBarItem
import com.medstudy.core.designsystem.StudyBottomBar
import com.medstudy.core.model.Category
import com.medstudy.core.model.pluralize
import androidx.compose.ui.graphics.vector.ImageVector
import com.medstudy.core.model.Question
import com.medstudy.feature.practice.PracticeViewModel
import com.medstudy.feature.practice.QuestionPracticeScreen
import com.medstudy.feature.progress.ProgressScreen
import com.medstudy.feature.progress.ProgressViewModel

private enum class StudyDestination(val title: String, val icon: ImageVector) {
    TODAY("Today", Icons.Outlined.Today),
    PRACTICE("Practice", Icons.Outlined.Quiz),
    PROGRESS("Progress", Icons.Outlined.Insights),
    ME("Me", Icons.Outlined.Person),
}

private enum class PracticeView { HOME, SUBJECT_DETAIL, SAVED }

@Composable
fun StudyApp(
    container: StudyAppContainer,
    notificationQuestionId: String? = null,
    onNotificationQuestionHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileStore = remember { UserProfileStore(context) }
    var profile by remember {
        // Debug-only shortcut (DEV_SKIP_AUTH in local.properties): a ready-made profile instead of onboarding.
        mutableStateOf(profileStore.load() ?: if (BuildConfig.DEV_SKIP_AUTH) UserProfile("Dev tester", listOf(UserProfileStore.EXAMS.first()), 10) else null)
    }
    // Bumped when the account changes (sign-in, sign-out) so the gate below re-checks it.
    var accountVersion by remember { mutableIntStateOf(0) }
    // Also changes when the server ends the session (device limit, signed out from another device).
    val sessionVersion by container.account.sessionVersion.collectAsState()
    val signedIn = remember(accountVersion, sessionVersion) { container.account.isSignedIn }
    LaunchedEffect(sessionVersion) { if (!container.account.isSignedIn) container.clearEntitlement() }
    // An account is required: it is what the server-side free trial is attached to.
    if (profile == null || (!signedIn && !BuildConfig.DEV_SKIP_AUTH)) {
        OnboardingScreen(container.account, existingProfile = profile) { created ->
            profileStore.save(created)
            scope.launch {
                runCatching {
                    // A returning account brings its study days and minutes with it; a new one keeps this phone's defaults.
                    val saved = runCatching { container.account.pullStudySchedule() }.getOrNull()
                    container.weeklyStudyPlan.savePreferences(
                        (saved ?: container.weeklyStudyPlan.preferences()).copy(questionTargetPerDay = created.dailyQuestionGoal),
                    )
                }
            }
            profile = created
            accountVersion++
        }
        return
    }

    val bootstrapViewModel: CatalogBootstrapViewModel = viewModel(
        factory = CatalogBootstrapViewModel.factory(container.questionCatalog, container.medMcqaRepository, container.pyqRepository),
    )
    val bootstrapState by bootstrapViewModel.state.collectAsState()

    if (!bootstrapState.isReady) {
        Scaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (bootstrapState.isLoading) CircularProgressIndicator()
                Text(bootstrapState.error ?: "Loading MedMCQA questions...", modifier = Modifier.padding(top = 12.dp))
                if (bootstrapState.error != null) {
                    TextButton(onClick = bootstrapViewModel::retry) { Text("Retry") }
                }
            }
        }
        return
    }

    LaunchedEffect(Unit) {
        container.account.confirmDeviceSlot()
        container.syncSoon()
    }

    val entitlement by container.entitlementState.collectAsState()
    // "Now" for the plan and trial countdown. It is a state so the day count moves on its own: every minute, and when the app comes back to the front.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60_000); nowMillis = System.currentTimeMillis() } }
    var showPaywall by remember { mutableStateOf(false) }
    val billingPrice by container.billing.price.collectAsState()
    val billingMessage by container.billing.message.collectAsState()
    LaunchedEffect(accountVersion) { container.refreshEntitlement() }
    LaunchedEffect(showPaywall) { if (showPaywall) container.billing.load() }

    val practiceViewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModel.factory(container.questionCatalog, container.learnerActivity),
    )
    val progressViewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModel.factory(
            container.questionCatalog,
            container.learnerActivity,
            container.weeklyStudyPlan,
        ),
    )
    // Answers that arrive from the learner's other phone show up without reopening the app.
    val answersDownloaded by container.answersDownloaded.collectAsState()
    LaunchedEffect(answersDownloaded) { if (answersDownloaded > 0) progressViewModel.refresh() }
    val practiceState by practiceViewModel.state.collectAsState()
    val mockTestViewModel: com.medstudy.feature.practice.MockTestViewModel = viewModel(
        factory = com.medstudy.feature.practice.MockTestViewModel.factory(container.questionCatalog, container.learnerActivity),
    )
    val mockTestState by mockTestViewModel.state.collectAsState()
    var isMockTest by rememberSaveable { mutableStateOf(false) }
    val progressState by progressViewModel.state.collectAsState()
    var destinationName by rememberSaveable { mutableStateOf(StudyDestination.TODAY.name) }
    var isPracticing by rememberSaveable { mutableStateOf(false) }
    var savedQuestions by rememberSaveable { mutableStateOf(emptyList<Question>()) }
    var savedLoading by rememberSaveable { mutableStateOf(false) }
    var practiceViewName by rememberSaveable { mutableStateOf(PracticeView.HOME.name) }
    var selectedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    val destination = StudyDestination.valueOf(destinationName)
    val practiceView = PracticeView.valueOf(practiceViewName)
    val lifecycleOwner = LocalLifecycleOwner.current
    var overlayInterval by rememberSaveable {
        mutableIntStateOf(context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0)
            .getInt(QuestionOverlayService.KEY_INTERVAL, 10).coerceIn(1, 20))
    }
    var overlayEnabled by rememberSaveable {
        mutableStateOf(context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0)
            .getBoolean(QuestionOverlayService.KEY_ENABLED, false))
    }
    var overlayAccessGranted by rememberSaveable { mutableStateOf(Settings.canDrawOverlays(context)) }

    DisposableEffect(lifecycleOwner, overlayEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                nowMillis = System.currentTimeMillis()
                overlayAccessGranted = Settings.canDrawOverlays(context)
                scope.launch { container.refreshEntitlement() }
                if (overlayEnabled && overlayAccessGranted) QuestionOverlayService.start(context)
            }
            // Leaving the app: back up what was done in this visit.
            if (event == Lifecycle.Event.ON_STOP) container.syncSoon()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(notificationQuestionId, bootstrapState.isReady) {
        val questionId = notificationQuestionId
        if (bootstrapState.isReady && questionId != null) {
            practiceViewModel.start(questionId = questionId)
            isPracticing = true
            onNotificationQuestionHandled()
        }
    }

    val goal = profile!!.dailyQuestionGoal
    val today = LocalDate.now()
    val answeredToday = progressState.attempts.count { it.studyDateIso == today.toString() }
    val dueAreas = progressState.areas.filter(WeakArea::isDue)
    val mistakeCount = SessionPlanner.mistakes(progressState.attempts).size
    val todayBlock = progressState.plan?.blocks?.firstOrNull { it.studyDateIso == today.toString() }

    // Entitlement: full access during the trial and while subscribed; otherwise the free tier (a daily allowance, no premium modes).
    val isFull = entitlement.isFullAt(nowMillis)
    val trialDaysLeft = entitlement.trialDaysLeft(nowMillis)
    val questionsLeft = entitlement.questionsLeftToday(answeredToday, nowMillis)
    fun requireFull(block: () -> Unit) { if (isFull) block() else showPaywall = true }

    // The overlay is part of the paid plan: when access drops to the free tier, switch it off so the toggle tells the truth.
    LaunchedEffect(isFull) {
        if (!isFull && overlayEnabled) {
            overlayEnabled = false
            context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0).edit()
                .putBoolean(QuestionOverlayService.KEY_ENABLED, false).apply()
            QuestionOverlayService.stop(context)
        }
    }

    fun startQuestionFlow(category: Category? = null, questionId: String? = null) {
        if (questionsLeft == 0) { showPaywall = true; return }
        practiceViewModel.start(
            subjectId = category?.let { if (it.parentId == null) it.id else it.parentId },
            topicId = category?.takeIf { it.parentId != null }?.id,
            questionId = questionId,
            limit = questionsLeft.takeIf { it != Int.MAX_VALUE },
        )
        isPracticing = true
    }

    fun startSession(ids: List<String>, emptyMessage: String) {
        if (questionsLeft == 0) { showPaywall = true; return }
        practiceViewModel.startQuestions(ids.take(questionsLeft), emptyMessage)
        isPracticing = true
    }

    val subjects = remember(progressState.categories, progressState.questions, progressState.attempts) {
        subjectSummaries(progressState.categories, progressState.questions, progressState.attempts)
    }
    var examPapers by remember { mutableStateOf<List<com.medstudy.core.model.ExamPaper>>(emptyList()) }
    LaunchedEffect(bootstrapState.isReady) {
        if (bootstrapState.isReady) examPapers = runCatching { container.questionCatalog.examPapers() }.getOrDefault(emptyList())
    }
    val paperSummaries = remember(examPapers, progressState.questions, progressState.attempts) {
        val attempted = progressState.attempts.mapTo(HashSet()) { it.questionId }
        examPapers.sortedByDescending { it.year }.mapNotNull { paper ->
            val ids = progressState.questions.filter { it.examPaperId == paper.id }.sortedBy { it.sourceNumber ?: 0 }.map { it.id }
            if (ids.isEmpty()) null
            else PaperSummary(paper.id, paper.title, paper.year, paper.kind == com.medstudy.core.model.PaperKind.RECALL, ids, ids.filterTo(HashSet()) { it in attempted })
        }
    }
    // The blob is generated from the account id, so two people with the same name get different blobs.
    val avatarSeed = remember(accountVersion, profile) { if (container.account.isSignedIn) container.account.userId else "" }
    val planRows = progressState.plan?.blocks.orEmpty()
        .map { PlanRow(it, LocalDate.parse(it.studyDateIso)) }
        .sortedBy(PlanRow::date)
    val meStats = remember(progressState.attempts, today) {
        val graded = progressState.attempts.filter { it.result != com.medstudy.core.model.AttemptResult.UNANSWERED }
        MeStats(
            answered = progressState.attempts.mapTo(HashSet()) { it.questionId }.size,
            accuracyPercent = graded.takeIf { it.isNotEmpty() }
                ?.let { list -> list.count { it.result == com.medstudy.core.model.AttemptResult.CORRECT } * 100 / list.size },
            studyDays = progressState.attempts.mapTo(HashSet()) { it.studyDateIso }.size,
            streak = SessionPlanner.streak(progressState.attempts, today),
        )
    }
    val weekStart = today.with(java.time.DayOfWeek.MONDAY)
    val weekDays = (0L..6L).map { offset ->
        val date = weekStart.plusDays(offset)
        WeekDay(date, progressState.attempts.count { it.studyDateIso == date.toString() })
    }
    // Continue where the plan says; otherwise the most-due topic; otherwise a fresh daily set.
    val continueArea = dueAreas.firstOrNull()
    val continueCategoryId = todayBlock?.let { it.topicId ?: it.subjectId } ?: continueArea?.let { it.topicId ?: it.subjectId }
    val continueTarget = run {
        val subjectId = todayBlock?.subjectId ?: continueArea?.subjectId
        val subject = subjects.firstOrNull { it.id == subjectId }
        when {
            todayBlock != null -> ContinueTarget(
                todayBlock.title, subject?.name ?: "Today's plan", subject?.colorIndex ?: 0, "Plan",
                "${pluralize(todayBlock.questionTarget, "question")} · ${todayBlock.minuteTarget} min",
            )
            continueArea != null -> ContinueTarget(
                continueArea.title, subject?.name ?: "Review", subject?.colorIndex ?: 0, "Due",
                "${pluralize(goal, "question")} · about ${goal * 3} min",
            )
            else -> ContinueTarget("Daily set", pluralize(goal, "fresh question"), 0, "Today", "About ${goal * 3} min")
        }
    }

    fun startDaily() = startSession(
        SessionPlanner.daily(progressState.questions, progressState.attempts, goal),
        "You've answered every available question. Try Review due or Mistakes.",
    )
    fun startDue() = requireFull { startSession(
        SessionPlanner.due(progressState.areas, progressState.questions, progressState.attempts, goal),
        "Nothing is due for review right now.",
    ) }

    LaunchedEffect(destination, practiceView, isPracticing) {
        if (!isPracticing && destination == StudyDestination.PRACTICE) {
            savedLoading = true
            savedQuestions = runCatching { container.questionCatalog.bookmarkedQuestions() }.getOrDefault(emptyList())
            savedLoading = false
        }
    }

    fun applyProfile(updated: UserProfile, pushToCloud: Boolean) {
        profileStore.save(updated)
        profile = updated
        scope.launch {
            runCatching {
                container.weeklyStudyPlan.savePreferences(
                    container.weeklyStudyPlan.preferences().copy(questionTargetPerDay = updated.dailyQuestionGoal),
                )
            }
            if (pushToCloud && container.account.isSignedIn) runCatching { container.account.pushProfile(updated) }
            progressViewModel.refresh()
        }
    }

    var pickingExamDate by rememberSaveable { mutableStateOf(false) }
    if (pickingExamDate) {
        ExamDatePickerDialog(
            profile!!.examDateIso,
            onPick = { picked -> pickingExamDate = false; applyProfile(profile!!.copy(examDateIso = picked), pushToCloud = true) },
            onDismiss = { pickingExamDate = false },
        )
    }

    fun leavePractice(toToday: Boolean = false) {
        isPracticing = false
        if (toToday) destinationName = StudyDestination.TODAY.name
        progressViewModel.refresh()
        container.syncSoon()
    }

    BackHandler(enabled = isPracticing) { leavePractice() }
    BackHandler(enabled = !isPracticing && destination == StudyDestination.PRACTICE && practiceView != PracticeView.HOME) {
        practiceViewName = PracticeView.HOME.name
    }

    BackHandler(enabled = showPaywall) { showPaywall = false }
    val activity = remember(context) { context.findActivity() }
    val reducedMotion = rememberReducedMotion()
    val rootScreen = when {
        showPaywall -> RootScreen.PAYWALL
        isMockTest -> RootScreen.MOCK_TEST
        isPracticing -> RootScreen.PRACTICE
        else -> RootScreen.TABS
    }
    AnimatedContent(
        targetState = rootScreen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { rootTransition(initialState, targetState, reducedMotion) },
        label = "root",
    ) { shownRoot ->
    if (shownRoot == RootScreen.PAYWALL) {
        PaywallScreen(
            trialDaysLeft = trialDaysLeft,
            price = billingPrice,
            message = billingMessage,
            onSubscribe = { activity?.let { container.billing.launch(it) } },
            onRestore = { scope.launch { if (container.billing.restore()) showPaywall = false } },
            onDismiss = { container.billing.clearMessage(); showPaywall = false },
        )
    } else if (shownRoot == RootScreen.MOCK_TEST) {
        com.medstudy.feature.practice.MockTestScreen(
            state = mockTestState,
            onSelectOption = mockTestViewModel::select,
            onToggleFlag = mockTestViewModel::toggleFlag,
            onGoTo = mockTestViewModel::goTo,
            onPrevious = mockTestViewModel::previous,
            onNext = mockTestViewModel::next,
            onSubmit = mockTestViewModel::submit,
            onTick = mockTestViewModel::tick,
            onExit = { isMockTest = false; progressViewModel.refresh(); container.syncSoon() },
            imageResource = { assetId ->
                if (assetId == "demo.media.placeholder-landscape") R.drawable.demo_imaging_placeholder else null
            },
        )
    } else if (shownRoot == RootScreen.PRACTICE) {
        QuestionPracticeScreen(
            state = practiceState,
            onBack = { leavePractice() },
            onSelectOption = practiceViewModel::selectOption,
            onSubmit = practiceViewModel::submit,
            onShowAnswer = practiceViewModel::showAnswer,
            onToggleBookmark = practiceViewModel::toggleBookmark,
            onPrevious = practiceViewModel::previous,
            onNext = practiceViewModel::next,
            onFinish = practiceViewModel::finish,
            subjectColorIndex = { subjectId -> subjects.indexOfFirst { it.id == subjectId }.coerceAtLeast(0) },
            onReport = { questionId, reason -> scope.launch { container.account.report(questionId, reason) } },
            onReviewMissed = {
                val missed = practiceState.missedQuestionIds
                practiceViewModel.startQuestions(missed, "No missed questions to review.")
            },
            onExit = { leavePractice(toToday = true) },
            imageResource = { assetId ->
                if (assetId == "demo.media.placeholder-landscape") R.drawable.demo_imaging_placeholder else null
            },
        )
    } else {
        // The bar floats over the content (no opaque band behind it); screens leave LocalNavBarInset of room at their end.
        val navInset = 90.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Scaffold(containerColor = StudyTheme.colors.canvas) { padding ->
            val contentModifier = Modifier.padding(top = padding.calculateTopPadding())
            Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalNavBarInset provides navInset, LocalAvatarSeed provides avatarSeed) {
            AnimatedContent(
                targetState = destination,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { tabTransition(reducedMotion) },
                label = "tab",
            ) { shownDestination ->
            when (shownDestination) {
                StudyDestination.TODAY -> TodayScreen(
                    name = profile!!.name,
                    goal = goal,
                    answeredToday = answeredToday,
                    streak = SessionPlanner.streak(progressState.attempts, today),
                    continueTarget = continueTarget,
                    subjects = subjects,
                    planRows = planRows,
                    dueAreas = dueAreas,
                    weekDays = weekDays,
                    mistakeCount = mistakeCount,
                    trialDaysLeft = trialDaysLeft,
                    examDaysLeft = examDaysLeft(profile!!.examDateIso, today),
                    onSetExamDate = { pickingExamDate = true },
                    freeQuestionsLeft = if (isFull) null else questionsLeft,
                    onUpgrade = { showPaywall = true },
                    onQuickFive = {
                        startSession(
                            SessionPlanner.daily(progressState.questions, progressState.attempts, 5),
                            "You've answered every available question. Try Review due or Mistakes.",
                        )
                    },
                    onMistakes = {
                        requireFull { startSession(SessionPlanner.mistakes(progressState.attempts), "No missed questions to review.") }
                    },
                    onContinue = {
                        val category = progressState.categories.firstOrNull { it.id == continueCategoryId }
                        if (category != null) startQuestionFlow(category) else startDaily()
                    },
                    onOpenSubject = { id ->
                        selectedSubjectId = id
                        practiceViewName = PracticeView.SUBJECT_DETAIL.name
                        destinationName = StudyDestination.PRACTICE.name
                    },
                    onStartBlock = { block ->
                        val category = progressState.categories.firstOrNull { it.id == (block.topicId ?: block.subjectId) }
                        if (category != null) startQuestionFlow(category) else startDaily()
                    },
                    onReviewDue = ::startDue,
                    modifier = contentModifier,
                )
                StudyDestination.PRACTICE -> when (practiceView) {
                    PracticeView.HOME -> PracticeHomeScreen(
                        goal = goal,
                        dueCount = dueAreas.size,
                        mistakeCount = mistakeCount,
                        savedCount = savedQuestions.size,
                        subjects = subjects,
                        papers = paperSummaries,
                        onStartPaper = { ids -> startSession(ids, "This paper has no questions.") },
                        // Mock tests are part of the paid plan.
                        onStartMockTest = { paper -> requireFull { mockTestViewModel.start(paper.title, paper.questionIds); isMockTest = true } },
                        onDaily = ::startDaily,
                        onDue = ::startDue,
                        onMistakes = {
                            requireFull { startSession(SessionPlanner.mistakes(progressState.attempts), "No missed questions to review.") }
                        },
                        onOpenSubject = { id ->
                            selectedSubjectId = id
                            practiceViewName = PracticeView.SUBJECT_DETAIL.name
                        },
                        onSaved = { practiceViewName = PracticeView.SAVED.name },
                        modifier = Modifier,
                    )
                    PracticeView.SUBJECT_DETAIL -> {
                        val subject = subjects.firstOrNull { it.id == selectedSubjectId }
                        if (subject == null) {
                            practiceViewName = PracticeView.HOME.name
                        } else {
                            val topics = remember(subject.id, progressState.categories, progressState.questions, progressState.attempts) {
                                topicSummaries(subject.id, progressState.categories, progressState.questions, progressState.attempts)
                            }
                            val mistakes = remember(subject.id, progressState.questions, progressState.attempts) {
                                val missed = SessionPlanner.mistakes(progressState.attempts).toSet()
                                progressState.questions.filter { it.subjectId == subject.id && it.id in missed }
                            }
                            SubjectDetailScreen(
                                subject = subject,
                                topics = topics,
                                mistakes = mistakes,
                                onBack = { practiceViewName = PracticeView.HOME.name },
                                onPracticeAll = {
                                    progressState.categories.firstOrNull { it.id == subject.id }?.let { startQuestionFlow(it) }
                                },
                                onPracticeTopic = { id ->
                                    progressState.categories.firstOrNull { it.id == id }?.let { startQuestionFlow(it) }
                                },
                                onPracticeMistakes = { startSession(mistakes.map { it.id }, "No missed questions to review.") },
                                onOpenQuestion = { id -> startQuestionFlow(questionId = id) },
                                modifier = contentModifier,
                            )
                        }
                    }
                    PracticeView.SAVED -> SavedScreen(
                        questions = savedQuestions,
                        isLoading = savedLoading,
                        onBack = { practiceViewName = PracticeView.HOME.name },
                        onOpen = { question -> startQuestionFlow(questionId = question.id) },
                        modifier = contentModifier,
                    )
                }
                StudyDestination.PROGRESS -> ProgressScreen(
                    state = progressState,
                    onMonthChange = progressViewModel::changeMonth,
                    onSelectDate = progressViewModel::selectDate,
                    onToggleSubject = progressViewModel::toggleSubject,
                    onReviewQuestion = { question -> startQuestionFlow(questionId = question.id) },
                    onToggleStudyDay = progressViewModel::toggleStudyDay,
                    onQuestionTargetChange = progressViewModel::adjustQuestionTargetPerDay,
                    onMinuteTargetChange = progressViewModel::adjustMinuteTargetPerDay,
                    onRegeneratePlan = { requireFull { progressViewModel.regeneratePlan() } },
                    onMoveBlock = progressViewModel::moveBlock,
                    onRemoveBlock = progressViewModel::removeBlock,
                    onAddBlock = progressViewModel::addBlock,
                    onRetry = progressViewModel::refresh,
                    modifier = Modifier,
                    bottomInset = navInset,
                )
                StudyDestination.ME -> MeScreen(
                    profile = profile!!,
                    stats = meStats,
                    onSaveProfile = { updated -> applyProfile(updated, pushToCloud = true) },
                    onProfileRestored = { restored -> applyProfile(restored, pushToCloud = false) },
                    account = container.account,
                    subscription = subscriptionSummary(entitlement, nowMillis),
                    onSeePlans = { showPaywall = true },
                    onManageSubscription = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions")))
                    },
                    onSignedOut = { container.clearEntitlement(); accountVersion++ },
                    // Back to a first-run app: no profile, so onboarding starts from the beginning.
                    onAccountDeleted = { profileStore.clear(); profile = null; container.clearEntitlement() },
                    overlayInterval = overlayInterval,
                    overlayEnabled = overlayEnabled,
                    overlayAccessGranted = overlayAccessGranted,
                    onOverlayIntervalChange = { minutes ->
                        overlayInterval = minutes.toInt().coerceIn(1, 20)
                        context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0).edit()
                            .putInt(QuestionOverlayService.KEY_INTERVAL, overlayInterval).apply()
                    },
                    onEnableOverlay = {
                      requireFull {
                        overlayEnabled = true
                        context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0).edit()
                            .putBoolean(QuestionOverlayService.KEY_ENABLED, true).apply()
                        if (!Settings.canDrawOverlays(context)) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        } else {
                            QuestionOverlayService.start(context)
                        }
                      }
                    },
                    onDisableOverlay = {
                        overlayEnabled = false
                        context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0).edit()
                            .putBoolean(QuestionOverlayService.KEY_ENABLED, false).apply()
                        QuestionOverlayService.stop(context)
                    },
                    modifier = Modifier,
                )
            }
            }
            }
            StudyBottomBar(
                items = StudyDestination.entries.map { BottomBarItem(it.title, it.icon) },
                selectedIndex = destination.ordinal,
                onSelect = { index ->
                    destinationName = StudyDestination.entries[index].name
                    practiceViewName = PracticeView.HOME.name
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            }
        }
    }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Title and one line of detail for the Subscription card on Me. */
fun subscriptionSummary(entitlement: Entitlement, nowMillis: Long): SubscriptionSummary {
    val trialDays = entitlement.trialDaysLeft(nowMillis)
    return when {
        trialDays != null -> SubscriptionSummary(
            "Free trial",
            "${if (trialDays == 1) "1 day" else "$trialDays days"} left, ends ${java.time.Instant.ofEpochMilli(entitlement.endsAtMillis).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("d MMM"))}. Subscribe any time to keep everything.",
            paid = false,
        )
        entitlement.isFullAt(nowMillis) && entitlement.endsAtMillis < Long.MAX_VALUE / 4 -> SubscriptionSummary(
            "MedStudy Plus",
            "Active until " + java.time.Instant.ofEpochMilli(entitlement.endsAtMillis).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")),
            paid = true,
        )
        entitlement.isFullAt(nowMillis) -> SubscriptionSummary("MedStudy", "Checking your plan…", paid = false)
        else -> SubscriptionSummary("Free plan", "${Entitlement.FREE_DAILY_QUESTIONS} questions a day. Subscribe to unlock everything.", paid = false)
    }
}

data class SubscriptionSummary(val title: String, val detail: String, val paid: Boolean)
