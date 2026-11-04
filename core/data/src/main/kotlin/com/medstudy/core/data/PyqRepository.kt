package com.medstudy.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medstudy.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class PyqPaperJson(
    val id: String,
    val examId: String,
    val year: Int,
    val title: String,
    val kind: String,
    val count: Int,
    val sourceName: String,
)

data class PyqTopicJson(val id: String, val subjectId: String, val name: String)

data class PyqIndexJson(val papers: List<PyqPaperJson>, val topics: List<PyqTopicJson>)

data class PyqImageJson(val id: String, val path: String, val width: Int, val height: Int, val sha256: String)

data class PyqQuestionJson(
    val id: String,
    val number: Int,
    val subjectId: String,
    val topicId: String?,
    val stem: String,
    val options: List<String>,
    val answer: Int,
    val images: List<PyqImageJson>,
    val sourceFile: String,
    val sourcePage: Int,
)

/** Everything one load of the previous-year papers produces, ready for [QuestionCatalogRepository.saveCatalog]. */
data class PyqCatalog(
    val papers: List<ExamPaper>,
    val topics: List<Category>,
    val mediaAssets: List<MediaAsset>,
    val questions: List<Question>,
)

/**
 * Previous-year exam papers bundled under assets/pyq/ (index.json plus one JSON per paper and its images),
 * produced by tools/pyq-import. Add a paper by running the tool again; nothing here is exam specific.
 */
class PyqRepository(private val context: Context) {
    private val gson = Gson()

    suspend fun load(): PyqCatalog = withContext(Dispatchers.IO) {
        try {
            val index = read("pyq/index.json", PyqIndexJson::class.java)
            val topics = index.topics.mapIndexed { i, t ->
                Category(t.id, t.subjectId, t.name, "Topic: ${t.name}", position = 1000 + i)
            }
            val media = mutableListOf<MediaAsset>()
            val questions = mutableListOf<Question>()
            for (paper in index.papers) {
                val items: List<PyqQuestionJson> = gson.fromJson(
                    open("pyq/${paper.id}.json"), object : TypeToken<List<PyqQuestionJson>>() {}.type,
                )
                for (q in items) {
                    media += q.images.map { MediaAsset(it.path, it.path, "image/webp", it.sha256, it.width, it.height, ContentKind.OFFICIAL, RIGHTS) }
                    questions += q.toQuestion(paper)
                }
            }
            val papers = index.papers.map {
                ExamPaper(it.id, ExamId.valueOf(it.examId), it.year, it.title, PaperKind.valueOf(it.kind), it.sourceName)
            }
            PyqCatalog(papers, topics, media, questions)
        } catch (e: IOException) {
            PyqCatalog(emptyList(), emptyList(), emptyList(), emptyList())
        }
    }

    private fun <T> read(path: String, type: Class<T>): T = gson.fromJson(open(path), type)

    private fun open(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }

    private fun PyqQuestionJson.toQuestion(paper: PyqPaperJson): Question {
        val content = buildList<ContentBlock> {
            add(ContentBlock.Text(stem))
            images.forEachIndexed { i, img -> add(ContentBlock.Image(img.path, "Image $number.${i + 1} from ${paper.title}")) }
        }
        return Question(
            id = id,
            examId = ExamId.valueOf(paper.examId),
            examYear = paper.year,
            subjectId = subjectId,
            topicIds = listOfNotNull(topicId),
            format = QuestionFormat.SINGLE_BEST_ANSWER,
            languageTag = "en",
            content = content,
            options = options.mapIndexed { i, text ->
                QuestionOption("opt_$i", ('A' + i).toString(), i, listOf(ContentBlock.Text(text)))
            },
            answerKey = AnswerKey(listOf("opt_${answer - 1}"), explanation = "", optionRationales = emptyList()),
            references = emptyList(),
            provenance = ContentProvenance(
                contentKind = ContentKind.OFFICIAL,
                sourceName = paper.sourceName,
                sourceReference = "$sourceFile, page $sourcePage",
                rightsStatus = RIGHTS,
            ),
            reviewStatus = ReviewStatus.PENDING_REVIEW,
            examPaperId = paper.id,
            sourceNumber = number,
        )
    }

    companion object {
        /** Recalled papers compiled by a coaching provider: usable in development, but release builds must decide first. */
        val RIGHTS = RightsStatus.REVIEW_REQUIRED
    }
}
