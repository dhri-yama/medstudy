package com.medstudy.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medstudy.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * JSON data classes that match the MedMCQA JSON structure
 */
data class MedMcqaQuestionJson(
    val id: String,
    val question: String,
    val exp: String?,
    val cop: Int,
    val opa: String,
    val opb: String,
    val opc: String,
    val opd: String,
    val subject_name: String,
    val topic: String?,
    val choice_type: String,
    val source: String
)

data class MedMcqaSubjectJson(
    val id: String,
    val name: String,
    val question_count: Int,
    val topic_count: Int,
    val exam_types: List<String>
)

data class MedMcqaTopicJson(
    val name: String,
    val subjects: List<String>,
    val question_count: Int
)

/**
 * Repository for loading MedMCQA questions from JSON assets.
 * 
 * This is the production implementation that replaces DemoQuestionCatalog.
 * Falls back to demo data if JSON loading fails.
 */
class MedMcqaRepository(private val context: Context) {

    private val gson = Gson()

    /**
     * Load all MedMCQA questions from assets/questions.json
     * 
     * @return List of Question objects ready for use in the app
     */
    suspend fun getAllQuestions(): List<Question> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("questions.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<MedMcqaQuestionJson>>() {}.type
            val questionsJson: List<MedMcqaQuestionJson> = gson.fromJson(jsonString, type)

            // Load topics for topic ID mapping
            val topicsJsonString = context.assets.open("topics.json")
                .bufferedReader()
                .use { it.readText() }
            val topicsType = object : TypeToken<List<MedMcqaTopicJson>>() {}.type
            val topicsJson: List<MedMcqaTopicJson> = gson.fromJson(topicsJsonString, topicsType)

            // Load subjects for subject ID mapping
            val subjectsJsonString = context.assets.open("subjects.json")
                .bufferedReader()
                .use { it.readText() }
            val subjectsType = object : TypeToken<List<MedMcqaSubjectJson>>() {}.type
            val subjectsJson: List<MedMcqaSubjectJson> = gson.fromJson(subjectsJsonString, subjectsType)

            val subjectNameToId = subjectsJson.associate { subject ->
                subject.name to subject.id
            } + ("Unknown" to "unknown")

            questionsJson.toQuestions(topicsJson, subjectNameToId)

        } catch (e: IOException) {
            e.printStackTrace()
            // No fallback - we want only MedMCQA data
            emptyList()
        }
    }

    /**
     * Load questions for a specific subject from subject-specific JSON files
     * 
     * @param subjectId The subject ID (e.g., "medicine", "surgery")
     * @return List of Question objects for the specified subject
     */
    suspend fun getQuestionsBySubject(subjectId: String): List<Question> = withContext(Dispatchers.IO) {
        try {
            // Map of subject IDs to JSON filenames
            val subjectToFilename = mapOf(
                "medicine" to "medicine.json",
                "surgery" to "surgery.json", 
                "pathology" to "pathology.json",
                "pharmacology" to "pharmacology.json",
                "forensic_medicine" to "forensic_medicine.json",
                "anatomy" to "anatomy.json",
                "physiology" to "physiology.json",
                "biochemistry" to "biochemistry.json",
                "microbiology" to "microbiology.json",
                "gynaecology_and_obstetrics" to "gynaecology_and_obstetrics.json",
                "pediatrics" to "pediatrics.json",
                "social_and_preventive_medicine" to "social_and_preventive_medicine.json",
                "dental" to "dental.json",
                "ent" to "ent.json",
                "ophthalmology" to "ophthalmology.json",
                "radiology" to "radiology.json",
                "anaesthesia" to "anaesthesia.json",
                "psychiatry" to "psychiatry.json",
                "orthopaedics" to "orthopaedics.json",
                "skin" to "skin.json"
            )
            
            val filename = subjectToFilename[subjectId] ?: "questions.json"
            
            val jsonString = context.assets.open(filename)
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<MedMcqaQuestionJson>>() {}.type
            val questionsJson: List<MedMcqaQuestionJson> = gson.fromJson(jsonString, type)

            // Load topics for topic ID mapping
            try {
                val topicsJsonString = context.assets.open("topics.json")
                    .bufferedReader()
                    .use { it.readText() }
                val topicsType = object : TypeToken<List<MedMcqaTopicJson>>() {}.type
                val topicsJson: List<MedMcqaTopicJson> = gson.fromJson(topicsJsonString, topicsType)

                // Load subjects for subject ID mapping
                val subjectsJsonString = context.assets.open("subjects.json")
                    .bufferedReader()
                    .use { it.readText() }
                val subjectsType = object : TypeToken<List<MedMcqaSubjectJson>>() {}.type
                val subjectsJson: List<MedMcqaSubjectJson> = gson.fromJson(subjectsJsonString, subjectsType)
                val subjectNameToId = subjectsJson.associate { subject -> subject.name to subject.id } + ("Unknown" to "unknown")

                questionsJson.toQuestions(topicsJson, subjectNameToId)
            } catch (e: IOException) {
                // If topics/subjects fail to load, skip topic IDs in questions
                questionsJson.toQuestions(emptyList(), emptyMap())
            }

        } catch (e: IOException) {
            e.printStackTrace()
            // No fallback - return empty list
            emptyList()
        }
    }

    /**
     * Load all subjects from assets/subjects.json
     * 
     * @return List of Category objects for subject navigation
     */
    suspend fun getAllSubjects(): List<Category> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("subjects.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<MedMcqaSubjectJson>>() {}.type
            val subjectsJson: List<MedMcqaSubjectJson> = gson.fromJson(jsonString, type)

            subjectsJson.map { subject ->
                Category(
                    id = subject.id,
                    parentId = null,
                    name = subject.name,
                    description = "MedMCQA ${subject.name} questions (${subject.question_count})",
                    position = 0
                )
            } + Category(
                id = "unknown",
                parentId = null,
                name = "Unclassified",
                description = "MedMCQA questions without a subject label (12)",
                position = subjectsJson.size,
            )

        } catch (e: IOException) {
            e.printStackTrace()
            // No fallback - return empty list
            emptyList()
        }
    }

    /**
     * Clean string to create a valid ID
     */
    private fun cleanId(input: String): String {
        return input.lowercase()
            .replace(" ", "_")
            .replace("&", "and")
            .replace("/", "_")
            .replace("-", "_")
            .replace(".", "")
            .replace(",", "")
            .replace("'", "")
            .replace("\"", "")
    }

    /**
     * Load all subjects and topics as categories
     * 
     * @return List of Category objects including both subjects and topics
     */
    suspend fun getAllCategories(): List<Category> = withContext(Dispatchers.IO) {
        try {
            // Load subjects
            val subjectsJsonString = context.assets.open("subjects.json")
                .bufferedReader()
                .use { it.readText() }
            val subjectsType = object : TypeToken<List<MedMcqaSubjectJson>>() {}.type
            val subjectsJson: List<MedMcqaSubjectJson> = gson.fromJson(subjectsJsonString, subjectsType)

            // Create subject name to ID mapping for parent mapping
            val subjectNameToId = subjectsJson.associate { subject -> subject.name to subject.id } + ("Unknown" to "unknown")

            // Load topics
            val topicsJsonString = context.assets.open("topics.json")
                .bufferedReader()
                .use { it.readText() }
            val topicsType = object : TypeToken<List<MedMcqaTopicJson>>() {}.type
            val topicsJson: List<MedMcqaTopicJson> = gson.fromJson(topicsJsonString, topicsType)

            // Create categories for subjects
            val subjectCategories = subjectsJson.map { subject ->
                Category(
                    id = subject.id,
                    parentId = null,
                    name = subject.name,
                    description = "MedMCQA ${subject.name} questions (${subject.question_count})",
                    position = 0
                )
            } + Category(
                id = "unknown",
                parentId = null,
                name = "Unclassified",
                description = "MedMCQA questions without a subject label (12)",
                position = subjectsJson.size,
            )

            // Create categories for topics - use index to ensure unique IDs
            val topicCategories = topicsJson.mapIndexed { index, topic ->
                // Map topic to its primary subject (first in subjects list) as parent
                val primarySubjectName = topic.subjects.firstOrNull()
                val primarySubjectId = primarySubjectName?.let { subjectNameToId[it] }
                
                // Use index prefix to ensure unique IDs even if names collide after cleaning
                val topicId = "topic_${index}_${cleanId(topic.name)}"
                
                Category(
                    id = topicId,
                    parentId = primarySubjectId,
                    name = topic.name,
                    description = "Topic: ${topic.name} (${topic.question_count} questions)",
                    position = index
                )
            }

            subjectCategories + topicCategories

        } catch (e: IOException) {
            e.printStackTrace()
            // Fallback to subjects only
            getAllSubjects()
        }
    }

    /**
     * Load exam types from assets/exam_types.json
     */
    suspend fun getExamTypes(): List<MedMcqaExamTypeJson> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("exam_types.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<MedMcqaExamTypeJson>>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Load stats from assets/stats.json
     */
    suspend fun getStats(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("stats.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<Map<String, Any>>() {}.type
            gson.fromJson(jsonString, type)
        } catch (e: IOException) {
            e.printStackTrace()
            emptyMap()
        }
    }

    /**
     * Convert JSON question to the app's Question model
     * 
     * Handles:
     * - Option mapping (1-4 to 0-3)
     * - Question format (single/multi)
     * - Exam ID (NEET_PG, INI_CET)
     * - Subject/topic ID formatting
     */
    private fun List<MedMcqaQuestionJson>.toQuestions(
        topicsJson: List<MedMcqaTopicJson>,
        subjectNameToId: Map<String, String>,
        usedIds: MutableSet<String> = mutableSetOf()
    ): List<Question> {
        // Create a map of topic names to topic category IDs
        val topicNameToId = topicsJson.mapIndexed { index, topic ->
            topic.name.lowercase() to "topic_${index}_${cleanId(topic.name)}"
        }.toMap()
        
        // Track used IDs to ensure uniqueness
        val usedIds = mutableSetOf<String>()
        
        return mapIndexed { index, json -> 
            json.toQuestion(index, topicNameToId, subjectNameToId, usedIds)
        }
    }
    
    private fun MedMcqaQuestionJson.toQuestion(
        index: Int, 
        topicNameToId: Map<String, String>,
        subjectNameToId: Map<String, String>,
        usedIds: MutableSet<String>
    ): Question {
        // Handle missing cop field - default to first option if not present
        val safeCop = cop.takeIf { it in 1..4 } ?: 1
        
        // For multi-select questions, we need to handle multiple correct options
        val correctOptionIds = if (choice_type.lowercase() == "multi") {
            // For multi-select, we'll use the cop value as the primary correct answer
            // but in multi-select, multiple options can be correct
            // Since we don't have multi-cop info, use only the cop option for now
            listOf("opt_${safeCop - 1}")
        } else {
            // For single-select, just use the cop option
            listOf("opt_${safeCop - 1}")
        }

        val format = when (choice_type.lowercase()) {
            "single" -> QuestionFormat.SINGLE_BEST_ANSWER
            "multi" -> QuestionFormat.MULTI_SELECT
            else -> QuestionFormat.SINGLE_BEST_ANSWER
        }

        val examId = when (source.uppercase()) {
            "NEET PG" -> ExamId.NEET_PG
            "AIIMS" -> ExamId.INI_CET
            else -> ExamId.NEET_PG
        }

        // Map subject name to category ID using predefined mapping
        val subjectId = subjectNameToId[subject_name] ?: run {
            // Handle "Unknown" and other unrecognized subjects by defaulting to medicine
            subjectNameToId["Medicine"] ?: "medicine"
        }

        // Clean topic for ID using same cleaning function
        val topicIds = topic?.takeIf { it.isNotBlank() }?.let { topicName ->
            // Try to find the topic in our topics list
            val topicKey = topicName.lowercase().trim()
            topicNameToId[topicKey]?.let { topicId -> listOf(topicId) } ?: emptyList()
        } ?: emptyList()

        // Generate unique ID using index and original ID to ensure uniqueness
        var uniqueId = "medmcqa_${index}_${id.hashCode().toString().replace("-", "")}"
        var counter = 0
        while (uniqueId in usedIds) {
            counter++
            uniqueId = "medmcqa_${index}_${id.hashCode().toString().replace("-", "")}_${counter}"
        }
        usedIds.add(uniqueId)

        return Question(
            id = uniqueId,
            examId = examId,
            examYear = 2020,
            subjectId = subjectId,
            topicIds = topicIds,
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
                correctOptionIds = correctOptionIds,
                explanation = exp?.takeIf { it.isNotBlank() } ?: "",
                optionRationales = emptyList()
            ),
            references = emptyList(),
            provenance = ContentProvenance(
                contentKind = ContentKind.ORIGINAL,
                sourceName = "MedMCQA",
                rightsStatus = RightsStatus.LICENSED,
                attribution = "https://github.com/medmcqa/medmcqa"
            ),
            reviewStatus = ReviewStatus.PENDING_REVIEW,
            revision = 1
        )
    }
}

/**
 * JSON data class for exam types
 */
data class MedMcqaExamTypeJson(
    val id: String,
    val name: String,
    val description: String,
    val question_count: Int,
    val subjects_covered: Int,
    val topics_covered: Int
)
