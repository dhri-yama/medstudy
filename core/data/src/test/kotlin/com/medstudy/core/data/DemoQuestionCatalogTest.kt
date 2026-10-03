package com.medstudy.core.data

import com.medstudy.core.model.ContentKind
import com.medstudy.core.model.ContentBlock
import com.medstudy.core.model.ExamId
import com.medstudy.core.model.RightsStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoQuestionCatalogTest {
    @Test
    fun `demo catalog validates and is clearly marked`() {
        assertTrue(DemoQuestionCatalog.validate().toString(), DemoQuestionCatalog.validate().isEmpty())
        assertTrue(DemoQuestionCatalog.questions.all { it.examId == ExamId.DEMO })
        assertTrue(DemoQuestionCatalog.questions.all { it.provenance.contentKind == ContentKind.DEMO })
        assertTrue(DemoQuestionCatalog.questions.all { it.provenance.rightsStatus == RightsStatus.DEMO_ONLY })
    }

    @Test
    fun `catalog includes an image-bearing demo fixture`() {
        val imageQuestion = DemoQuestionCatalog.questions.single { question ->
            question.content.any { it is ContentBlock.Image }
        }

        assertEquals("demo.media.placeholder-landscape", (imageQuestion.content[1] as ContentBlock.Image).assetId)
    }
}
