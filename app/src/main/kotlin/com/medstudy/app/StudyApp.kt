package com.medstudy.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Arrangement
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
import com.medstudy.core.model.Category
import com.medstudy.core.model.Question
import com.medstudy.feature.categories.CategoriesScreen
import com.medstudy.feature.categories.CategoriesViewModel
import com.medstudy.feature.practice.PracticeViewModel
import com.medstudy.feature.practice.QuestionPracticeScreen
import com.medstudy.feature.progress.ProgressScreen
import com.medstudy.feature.progress.ProgressViewModel

private enum class StudyDestination(val title: String) {
    TODAY("Today"),
    PRACTICE("Practice"),
    PROGRESS("Progress"),
    ME("Me"),
}

private enum class PracticeView { HOME, SUBJECTS, SAVED }

@Composable
fun StudyApp(
    container: StudyAppContainer,
    notificationQuestionId: String? = null,
    onNotificationQuestionHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileStore = remember { UserProfileStore(context) }
    var profile by remember { mutableStateOf(profileStore.load()) }
    if (profile == null) {
        OnboardingScreen { created ->
            profileStore.save(created)
            scope.launch {
                runCatching {
                    container.weeklyStudyPlan.savePreferences(
                        container.weeklyStudyPlan.preferences().copy(questionTargetPerDay = created.dailyQuestionGoal),
                    )
                }
            }
            profile = created
        }
        return
    }

    val bootstrapViewModel: CatalogBootstrapViewModel = viewModel(
        factory = CatalogBootstrapViewModel.factory(container.questionCatalog, container.medMcqaRepository),
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
        if (container.account.isSignedIn) runCatching { container.account.sync() }
    }

    val categoriesViewModel: CategoriesViewModel = viewModel(
        factory = CategoriesViewModel.factory(container.questionCatalog),
    )
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
    val categoriesState by categoriesViewModel.state.collectAsState()
    val practiceState by practiceViewModel.state.collectAsState()
    val progressState by progressViewModel.state.collectAsState()
    var destinationName by rememberSaveable { mutableStateOf(StudyDestination.TODAY.name) }
    var isPracticing by rememberSaveable { mutableStateOf(false) }
    var savedQuestions by rememberSaveable { mutableStateOf(emptyList<Question>()) }
    var savedLoading by rememberSaveable { mutableStateOf(false) }
    var practiceViewName by rememberSaveable { mutableStateOf(PracticeView.HOME.name) }
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
                overlayAccessGranted = Settings.canDrawOverlays(context)
                if (overlayEnabled && overlayAccessGranted) QuestionOverlayService.start(context)
            }
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

    fun startQuestionFlow(category: Category? = null, questionId: String? = null) {
        practiceViewModel.start(
            subjectId = category?.let { if (it.parentId == null) it.id else it.parentId },
            topicId = category?.takeIf { it.parentId != null }?.id,
            questionId = questionId,
        )
        isPracticing = true
    }

    fun startSession(ids: List<String>, emptyMessage: String) {
        practiceViewModel.startQuestions(ids, emptyMessage)
        isPracticing = true
    }

    val goal = profile!!.dailyQuestionGoal
    val today = LocalDate.now()
    val answeredToday = progressState.attempts.count { it.studyDateIso == today.toString() }
    val dueAreas = progressState.areas.filter(WeakArea::isDue)
    val mistakeCount = SessionPlanner.mistakes(progressState.attempts).size
    val todayBlock = progressState.plan?.blocks?.firstOrNull { it.studyDateIso == today.toString() }

    fun startDaily() = startSession(
        SessionPlanner.daily(progressState.questions, progressState.attempts, goal),
        "You've answered every available question. Try Review due or Mistakes.",
    )
    fun startDue() = startSession(
        SessionPlanner.due(progressState.areas, progressState.questions, progressState.attempts, goal),
        "Nothing is due for review right now.",
    )

    LaunchedEffect(destination, practiceView, isPracticing) {
        if (!isPracticing && destination == StudyDestination.PRACTICE && practiceView == PracticeView.SAVED) {
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

    fun leavePractice(toToday: Boolean = false) {
        isPracticing = false
        if (toToday) destinationName = StudyDestination.TODAY.name
        progressViewModel.refresh()
    }

    BackHandler(enabled = isPracticing) { leavePractice() }
    BackHandler(enabled = !isPracticing && destination == StudyDestination.PRACTICE && practiceView != PracticeView.HOME) {
        practiceViewName = PracticeView.HOME.name
    }

    if (isPracticing) {
        QuestionPracticeScreen(
            state = practiceState,
            onBack = { leavePractice() },
            onSelectOption = practiceViewModel::selectOption,
            onSubmit = practiceViewModel::submit,
            onShowAnswer = practiceViewModel::showAnswer,
            onToggleBookmark = practiceViewModel::toggleBookmark,
            onPrevious = practiceViewModel::previous,
            onNext = practiceViewModel::next,
            onFinish = { leavePractice(toToday = true) },
            imageResource = { assetId ->
                if (assetId == "demo.media.placeholder-landscape") R.drawable.demo_imaging_placeholder else null
            },
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    StudyDestination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = {
                                destinationName = item.name
                                practiceViewName = PracticeView.HOME.name
                            },
                            icon = {
                                Icon(
                                    when (item) {
                                        StudyDestination.TODAY -> Icons.Outlined.Today
                                        StudyDestination.PRACTICE -> Icons.Outlined.Quiz
                                        StudyDestination.PROGRESS -> Icons.Outlined.Insights
                                        StudyDestination.ME -> Icons.Outlined.Person
                                    },
                                    contentDescription = item.title,
                                )
                            },
                            label = { Text(item.title) },
                        )
                    }
                }
            },
        ) { padding ->
            val contentModifier = Modifier.padding(padding)
            when (destination) {
                StudyDestination.TODAY -> TodayScreen(
                    name = profile!!.name,
                    goal = goal,
                    answeredToday = answeredToday,
                    streak = SessionPlanner.streak(progressState.attempts, today),
                    todayBlock = todayBlock,
                    dueAreas = dueAreas,
                    onStartDaily = ::startDaily,
                    onStartBlock = {
                        val category = progressState.categories.firstOrNull { it.id == (todayBlock?.topicId ?: todayBlock?.subjectId) }
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
                        onDaily = ::startDaily,
                        onDue = ::startDue,
                        onMistakes = {
                            startSession(SessionPlanner.mistakes(progressState.attempts), "No missed questions to review.")
                        },
                        onSubjects = { practiceViewName = PracticeView.SUBJECTS.name },
                        onSaved = { practiceViewName = PracticeView.SAVED.name },
                        modifier = contentModifier,
                    )
                    PracticeView.SUBJECTS -> Column(contentModifier) {
                        TextButton(onClick = { practiceViewName = PracticeView.HOME.name }) { Text("\u2190 Practice") }
                        CategoriesScreen(
                            state = categoriesState,
                            onCategorySelected = { category -> startQuestionFlow(category) },
                            onRetry = categoriesViewModel::load,
                        )
                    }
                    PracticeView.SAVED -> Column(contentModifier) {
                        TextButton(onClick = { practiceViewName = PracticeView.HOME.name }) { Text("\u2190 Practice") }
                        BookmarksScreen(
                            questions = savedQuestions,
                            isLoading = savedLoading,
                            onOpen = { question -> startQuestionFlow(questionId = question.id) },
                        )
                    }
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
                    onRegeneratePlan = progressViewModel::regeneratePlan,
                    onMoveBlock = progressViewModel::moveBlock,
                    onRemoveBlock = progressViewModel::removeBlock,
                    onAddBlock = progressViewModel::addBlock,
                    onRetry = progressViewModel::refresh,
                    modifier = contentModifier,
                )
                StudyDestination.ME -> MeScreen(
                    profile = profile!!,
                    onSaveProfile = { updated -> applyProfile(updated, pushToCloud = true) },
                    onProfileRestored = { restored -> applyProfile(restored, pushToCloud = false) },
                    account = container.account,
                    overlayInterval = overlayInterval,
                    overlayEnabled = overlayEnabled,
                    overlayAccessGranted = overlayAccessGranted,
                    onOverlayIntervalChange = { minutes ->
                        overlayInterval = minutes.toInt().coerceIn(1, 20)
                        context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0).edit()
                            .putInt(QuestionOverlayService.KEY_INTERVAL, overlayInterval).apply()
                    },
                    onEnableOverlay = {
                        overlayEnabled = true
                        context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0).edit()
                            .putBoolean(QuestionOverlayService.KEY_ENABLED, true).apply()
                        if (!Settings.canDrawOverlays(context)) {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        } else {
                            QuestionOverlayService.start(context)
                        }
                    },
                    onDisableOverlay = {
                        overlayEnabled = false
                        context.getSharedPreferences(QuestionOverlayService.PREFS_NAME, 0).edit()
                            .putBoolean(QuestionOverlayService.KEY_ENABLED, false).apply()
                        QuestionOverlayService.stop(context)
                    },
                    modifier = contentModifier,
                )
            }
        }
    }
}

@Composable
private fun BookmarksScreen(
    questions: List<Question>,
    isLoading: Boolean,
    onOpen: (Question) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Text("Saved questions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
            questions.isEmpty() -> Text(
                "Questions you bookmark will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(questions, key = Question::id) { question ->
                    Surface(
                        onClick = { onOpen(question) },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(question.subjectId.substringAfterLast('.').replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            Text(question.content.filterIsInstance<com.medstudy.core.model.ContentBlock.Text>().firstOrNull()?.text.orEmpty(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}
