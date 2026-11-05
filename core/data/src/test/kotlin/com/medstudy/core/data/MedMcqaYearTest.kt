package com.medstudy.core.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class MedMcqaYearTest {
    @Test
    fun readsTheExamYearFromTheStem() {
        assertEquals(2019, examYearFromStem("Which finger has two dorsal interossei (AIIMS May 2019)"))
        assertEquals(2013, examYearFromStem("Not used for iron poison in: FMGE 09; NEET 13"))
        assertNull(examYearFromStem("Type of collagen maximum in skin:-"))
    }

    @Test
    fun readsTheExamYearFromTheLabel() {
        assertEquals(2019, examYearOf("NEET 2019", "q"))
        assertEquals(2020, examYearOf("NEET Jan 2020", "q"))
        assertEquals(2018, examYearOf("AIIMS 2018", "q"))
        assertNull(examYearOf("Upper limb", "Type of collagen maximum in skin:-"))
        assertEquals(2019, examYearOf(null, "Which finger (AIIMS May 2019)"))
    }

    @Test
    fun bundledQuestionsFrom2018OrLaterAreCountedByLabel() {
        val all: List<MedMcqaQuestionJson> = Gson().fromJson(
            File("../../app/src/main/assets/questions.json").readText(), object : TypeToken<List<MedMcqaQuestionJson>>() {}.type,
        )
        assertEquals(862, all.count { (examYearOf(it.topic_name, it.question) ?: 0) >= MIN_EXAM_YEAR })
    }
}
