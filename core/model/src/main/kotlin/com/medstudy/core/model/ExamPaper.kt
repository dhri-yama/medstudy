package com.medstudy.core.model

enum class PaperKind {
    /** The exam's own question paper. */
    OFFICIAL,

    /** Questions reconstructed from candidates' memory; not the official paper. */
    RECALL,
}

/** One sitting of an exam. [id] is the tag stored on each question, for example "NEETPG2025". */
data class ExamPaper(
    val id: String,
    val examId: ExamId,
    val year: Int,
    val title: String,
    val kind: PaperKind = PaperKind.OFFICIAL,
    val sourceName: String? = null,
)
