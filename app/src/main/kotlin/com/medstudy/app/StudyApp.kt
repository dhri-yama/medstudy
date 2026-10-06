package com.medstudy.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    HOME("Home"),
    CATEGORIES("Subjects"),
    PROGRESS("Progress"),
    BOOKMARKS("Saved"),
}

@Composable
fun StudyApp(container: StudyAppContainer) {
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
    var destinationName by rememberSaveable { mutableStateOf(StudyDestination.HOME.name) }
    var isPracticing by rememberSaveable { mutableStateOf(false) }
    var savedQuestions by rememberSaveable { mutableStateOf(emptyList<Question>()) }
    var savedLoading by rememberSaveable { mutableStateOf(false) }
    val destination = StudyDestination.valueOf(destinationName)

    fun startQuestionFlow(category: Category? = null, questionId: String? = null) {
        practiceViewModel.start(
            subjectId = category?.let { if (it.parentId == null) it.id else it.parentId },
            topicId = category?.takeIf { it.parentId != null }?.id,
            questionId = questionId,
        )
        isPracticing = true
    }

    fun startRandomUnattemptedQuestion() {
        practiceViewModel.startRandomUnattempted()
        isPracticing = true
    }

    LaunchedEffect(destination, isPracticing) {
        if (!isPracticing && destination == StudyDestination.BOOKMARKS) {
            savedLoading = true
            savedQuestions = runCatching { container.questionCatalog.bookmarkedQuestions() }.getOrDefault(emptyList())
            savedLoading = false
        }
    }

    BackHandler(enabled = isPracticing) {
        isPracticing = false
        if (destination == StudyDestination.PROGRESS) progressViewModel.refresh()
    }

    if (isPracticing) {
        QuestionPracticeScreen(
            state = practiceState,
            onBack = {
                isPracticing = false
                if (destination == StudyDestination.PROGRESS) progressViewModel.refresh()
            },
            onSelectOption = practiceViewModel::selectOption,
            onSubmit = practiceViewModel::submit,
            onShowAnswer = practiceViewModel::showAnswer,
            onToggleBookmark = practiceViewModel::toggleBookmark,
            onPrevious = practiceViewModel::previous,
            onNext = practiceViewModel::next,
            onFinish = {
                isPracticing = false
                destinationName = StudyDestination.PROGRESS.name
                progressViewModel.refresh()
            },
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
                            onClick = { destinationName = item.name },
                            icon = {
                                Icon(
                                    when (item) {
                                        StudyDestination.HOME -> Icons.Outlined.Home
                                        StudyDestination.CATEGORIES -> Icons.Outlined.Category
                                        StudyDestination.PROGRESS -> Icons.Outlined.CalendarMonth
                                        StudyDestination.BOOKMARKS -> Icons.Outlined.Bookmarks
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
            when (destination) {
                StudyDestination.HOME -> HomeScreen(
                    onBrowse = { destinationName = StudyDestination.CATEGORIES.name },
                    onStart = ::startRandomUnattemptedQuestion,
                    modifier = Modifier.padding(padding),
                )
                StudyDestination.CATEGORIES -> CategoriesScreen(
                    state = categoriesState,
                    onCategorySelected = { category -> startQuestionFlow(category) },
                    onRetry = categoriesViewModel::load,
                    modifier = Modifier.padding(padding),
                )
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
                    modifier = Modifier.padding(padding),
                )
                StudyDestination.BOOKMARKS -> BookmarksScreen(
                    questions = savedQuestions,
                    isLoading = savedLoading,
                    onOpen = { question -> startQuestionFlow(questionId = question.id) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}
@Composable
private fun HomeScreen(
    onBrowse: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 20.dp)) {
        Text("MedStudy", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        Text(
            "Make today count.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            "A clear place to practise, review, and build consistency.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
        )
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MEDMCQA DATA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    Text("1,618 real medical questions from NEET PG & AIIMS.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Text("Your study", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 26.dp, bottom = 10.dp))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Start practice") }
        OutlinedButton(
            onClick = onBrowse,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("Browse subjects") }
        Text(
            "Your question history stays on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp),
        )
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
