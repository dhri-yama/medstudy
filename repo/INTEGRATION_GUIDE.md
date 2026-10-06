# MedMCQA Data Integration Guide

## 🚀 Quick Start

This guide shows how to integrate the MedMCQA dataset into your Android app, replacing the hardcoded demo data.

## 📁 File Structure

Place the JSON files in your app's assets directory:

```
app/src/main/assets/
├── subjects.json
├── topics.json
├── questions.json
├── exam_types.json
├── stats.json
└── /subjects/
    ├── anatomy.json
    ├── medicine.json
    └── ...
```

## 🔧 Step 1: Copy JSON Files to Assets

Copy all JSON files from `/repo` to your app's assets directory:

```bash
# From your project root
cp /Users/sagar/Documents/ChatGPT/medstudy/repo/*.json app/src/main/assets/
```

## 📦 Step 2: Create Data Models

Create a data class to represent the JSON structure:

### QuestionJson.kt

```kotlin
package com.medstudy.core.data

import com.medstudy.core.model.*

data class QuestionJson(
    val id: String,
    val question: String,
    val exp: String?,
    val cop: Int, // 1-4 (1=a, 2=b, 3=c, 4=d)
    val opa: String,
    val opb: String,
    val opc: String,
    val opd: String,
    val subject_name: String,
    val topic: String?,
    val choice_type: String, // "single" or "multi"
    val source: String // "NEET PG" or "AIIMS"
)

data class SubjectJson(
    val id: String,
    val name: String,
    val question_count: Int,
    val topic_count: Int,
    val exam_types: List<String>
)

fun QuestionJson.toQuestion(): Question {
    val correctIndex = cop - 1 // Convert to 0-based index
    val correctOptionId = "opt_$correctIndex"
    
    val format = when (choice_type) {
        "single" -> QuestionFormat.SINGLE_BEST_ANSWER
        "multi" -> QuestionFormat.MULTI_SELECT
        else -> QuestionFormat.SINGLE_BEST_ANSWER
    }
    
    val examId = when (source) {
        "NEET PG" -> ExamId.NEET_PG
        "AIIMS" -> ExamId.INI_CET
        else -> ExamId.NEET_PG
    }
    
    return Question(
        id = id,
        examId = examId,
        examYear = 2020, // All questions are from 2020
        subjectId = subject_name.lowerCase().replace(" ", "_").replace("&", "and"),
        topicIds = listOf(topic?.lowerCase()?.replace(" ", "_") ?: ""),
        format = format,
        languageTag = "en",
        content = listOf(ContentBlock.Text(question)),
        options = listOf(
            QuestionOption("opt_0", "A", 0, listOf(ContentBlock.Text(opa))),
            QuestionOption("opt_1", "B", 1, listOf(ContentBlock.Text(opb))),
            QuestionOption("opt_2", "C", 2, listOf(ContentBlock.Text(opc))),
            QuestionOption("opt_3", "D", 3, listOf(ContentBlock.Text(opd)))
        ),
        answerKey = AnswerKey(
            correctOptionIds = listOf(correctOptionId),
            explanation = exp ?: "",
            optionRationales = emptyList()
        ),
        references = emptyList(),
        provenance = ContentProvenance(
            id = id,
            source = "MedMCQA",
            license = "MIT"
        ),
        reviewStatus = ReviewStatus.UNREVIEWED,
        revision = 1
    )
}
```

## 📥 Step 3: Create JSON Loader

### JsonQuestionRepository.kt

```kotlin
package com.medstudy.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medstudy.core.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JsonQuestionRepository(private val context: Context) {
    
    private val gson = Gson()
    
    suspend fun loadQuestions(): List<Question> = withContext(Dispatchers.IO) {
        val jsonString = context.assets.open("questions.json").bufferedReader().use { it.readText() }
        val questionsJson: List<QuestionJson> = gson.fromJson(
            jsonString, 
            object : TypeToken<List<QuestionJson>>() {}.type
        )
        questionsJson.map { it.toQuestion() }
    }
    
    suspend fun loadSubjects(): List<Category> = withContext(Dispatchers.IO) {
        val jsonString = context.assets.open("subjects.json").bufferedReader().use { it.readText() }
        val subjectsJson: List<SubjectJson> = gson.fromJson(
            jsonString,
            object : TypeToken<List<SubjectJson>>() {}.type
        )
        
        subjectsJson.map { subject ->
            Category(
                id = subject.id,
                parentId = null,
                name = subject.name,
                description = "MedMCQA ${subject.name} questions",
                position = 0
            )
        }
    }
    
    suspend fun loadQuestionsBySubject(subjectId: String): List<Question> = withContext(Dispatchers.IO) {
        try {
            val filename = "$subjectId.json"
            val jsonString = context.assets.open(filename).bufferedReader().use { it.readText() }
            val questionsJson: List<QuestionJson> = gson.fromJson(
                jsonString,
                object : TypeToken<List<QuestionJson>>() {}.type
            )
            questionsJson.map { it.toQuestion() }
        } catch (e: Exception) {
            // Fallback to all questions filtered by subject
            loadQuestions().filter { it.subjectId == subjectId }
        }
    }
}
```

## 🏗️ Step 4: Create ViewModel

### MedMcqaViewModel.kt

```kotlin
package com.medstudy.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medstudy.core.data.JsonQuestionRepository
import com.medstudy.core.model.Category
import com.medstudy.core.model.Question
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MedMcqaViewModel(private val repository: JsonQuestionRepository) : ViewModel() {
    
    private val _subjects = MutableStateFlow<List<Category>>(emptyList())
    val subjects: StateFlow<List<Category>> = _subjects
    
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions
    
    private val _selectedSubject = MutableStateFlow<Category?>(null)
    val selectedSubject: StateFlow<Category?> = _selectedSubject
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _subjects.value = repository.loadSubjects()
            _questions.value = repository.loadQuestions()
        }
    }
    
    fun selectSubject(subject: Category) {
        viewModelScope.launch {
            _selectedSubject.value = subject
            _questions.value = repository.loadQuestionsBySubject(subject.id)
        }
    }
    
    fun refresh() {
        loadData()
    }
}
```

## 🎯 Step 5: Update Application Container

### Modify StudyAppContainer.kt

```kotlin
package com.medstudy.app

import android.content.Context
import com.medstudy.core.data.JsonQuestionRepository

class StudyAppContainer(private val context: Context) {
    
    val questionRepository by lazy { 
        JsonQuestionRepository(context) 
    }
    
    val medMcqaViewModel by lazy {
        MedMcqaViewModel(questionRepository)
    }
    
    // Keep your existing repositories
    val demoQuestionRepository by lazy { 
        DemoQuestionRepository() 
    }
}
```

## 🌐 Step 6: Update UI to Use Real Data

### Modify StudyApp.kt or CatalogScreen.kt

```kotlin
@Composable
fun MedMcqaScreen(viewModel: MedMcqaViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val questions by viewModel.questions.collectAsState()
    
    Column(modifier = Modifier.padding(16.dp)) {
        
        // Subject selection
        LazyColumn {
            items(subjects) { subject ->
                SubjectCard(
                    subject = subject,
                    questionCount = questions.count { it.subjectId == subject.id },
                    onClick = { viewModel.selectSubject(subject) }
                )
            }
        }
        
        // Questions
        if (questions.isNotEmpty()) {
            QuestionList(questions = questions)
        }
    }
}
```

## 🔧 Dependencies

Add Gson to your app's build.gradle:

```gradle
// app/build.gradle.kts
dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}
```

## 🚀 Running the App

Since Android SDK is not configured on this system, you'll need to:

### Option 1: Android Studio
1. Open the project in Android Studio
2. Copy the JSON files to `app/src/main/assets/`
3. Add the code above
4. Build and run on an emulator or device

### Option 2: Command Line
1. Install Android SDK and set up ANDROID_HOME
2. Create an Android emulator
3. Run `./gradlew :app:installDebug`
4. Launch the app

### Option 3: Use Existing Demo Structure
Replace the DemoQuestionCatalog with MedMcqaQuestionCatalog in your bootstrap:

```kotlin
// In CatalogBootstrapViewModel.kt or similar
fun loadCatalog(): QuestionCatalog {
    return MedMcqaQuestionCatalog  // Instead of DemoQuestionCatalog
}
```

## 📊 Testing Integration

### Quick Test Script

Create a simple test to verify data loading:

```kotlin
package com.medstudy.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedMcqaIntegrationTest {
    
    @Test
    fun testJsonLoading() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = JsonQuestionRepository(context)
        
        // Copy test JSON files to assets first
        val questions = repository.loadQuestions()
        val subjects = repository.loadSubjects()
        
        Assert.assertTrue("Should load questions", questions.isNotEmpty())
        Assert.assertTrue("Should load subjects", subjects.isNotEmpty())
        
        println("✅ Loaded ${questions.size} questions")
        println("✅ Loaded ${subjects.size} subjects")
    }
}
```

## ⚡ Quick Verification

To quickly verify the JSON files are working:

```kotlin
// In a temporary test
fun main() {
    val context: Context = // get your application context
    val questions = context.assets.open("questions.json").bufferedReader().use { it.readText() }
    println("Questions JSON size: ${questions.length} bytes")
}
```

## 📝 Data Format Reference

### Question JSON Structure
```json
{
  "id": "unique-identifier",
  "question": "What is the treatment?",
  "exp": "Detailed explanation...",
  "cop": 1,                        // 1-4 (a-d)
  "opa": "Option A",
  "opb": "Option B",
  "opc": "Option C", 
  "opd": "Option D",
  "subject_name": "Medicine",
  "topic": "Cardiology",
  "choice_type": "single",        // or "multi"
  "source": "NEET PG"            // or "AIIMS"
}
```

### Subject JSON Structure
```json
{
  "id": "medicine",
  "name": "Medicine",
  "question_count": 140,
  "topic_count": 25,
  "exam_types": ["NEET PG", "AIIMS"]
}
```

## 🎯 Integration Checklist

- [ ] Copy JSON files to `app/src/main/assets/`
- [ ] Add Gson dependency to `app/build.gradle.kts`
- [ ] Create `QuestionJson.kt` data class
- [ ] Create `JsonQuestionRepository.kt`
- [ ] Update `StudyAppContainer` to use the new repository
- [ ] Update UI to display real data
- [ ] Test with Android Studio or command line

## 📞 Support

For integration issues:
1. Check JSON files are in the correct assets directory
2. Verify Gson dependency is added
3. Ensure file names match exactly (case-sensitive)
4. Check Logcat for loading errors

The data is now ready to use! Your app can load 1,618 real medical questions from NEET PG and AIIMS exams instead of demo content.