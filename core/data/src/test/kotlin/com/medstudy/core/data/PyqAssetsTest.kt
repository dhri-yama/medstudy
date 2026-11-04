package com.medstudy.core.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sanity checks on the bundled previous-year papers (assets/pyq), produced by tools/pyq-import. */
class PyqAssetsTest {
    private val assets = File("../../app/src/main/assets")
    private val gson = Gson()

    private val index: PyqIndexJson by lazy { gson.fromJson(File(assets, "pyq/index.json").readText(), PyqIndexJson::class.java) }

    private fun questions(paperId: String): List<PyqQuestionJson> =
        gson.fromJson(File(assets, "pyq/$paperId.json").readText(), object : TypeToken<List<PyqQuestionJson>>() {}.type)

    @Test
    fun everyPaperHasItsDeclaredCount() {
        assertTrue(index.papers.isNotEmpty())
        index.papers.forEach { assertEquals(it.id, it.count, questions(it.id).size) }
    }

    @Test
    fun questionsAreComplete() {
        val all = index.papers.flatMap { questions(it.id) }
        assertEquals("question ids are unique", all.size, all.map { it.id }.toSet().size)
        all.forEach {
            assertEquals(it.id, 4, it.options.size)
            assertTrue("${it.id} has a blank option", it.options.all(String::isNotBlank))
            assertEquals("${it.id} has duplicate options", 4, it.options.toSet().size)
            assertTrue(it.id, it.answer in 1..4)
            assertTrue(it.id, it.stem.isNotBlank())
        }
    }

    @Test
    fun imagesExistAndTopicsResolve() {
        val newTopics = index.topics.map { it.id }.toSet()
        val existing = gson.fromJson<List<MedMcqaTopicJson>>(
            File(assets, "topics.json").readText(), object : TypeToken<List<MedMcqaTopicJson>>() {}.type,
        ).size
        index.papers.flatMap { questions(it.id) }.forEach { q ->
            q.images.forEach { assertTrue("${q.id}: ${it.path} missing", File(assets, it.path).isFile) }
            q.topicId?.let { t ->
                assertTrue("${q.id}: unknown topic $t", t in newTopics || (t.startsWith("topic_") && t.removePrefix("topic_").substringBefore('_').toInt() < existing))
            }
        }
    }
}
