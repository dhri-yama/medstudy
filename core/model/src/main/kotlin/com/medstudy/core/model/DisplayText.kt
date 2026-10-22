package com.medstudy.core.model

/** "subject.forensic_medicine" -> "Forensic Medicine". */
fun subjectDisplayName(subjectId: String): String =
    subjectId.substringAfterLast('.').split('_', ' ').filter(String::isNotEmpty)
        .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }

fun pluralize(count: Int, singular: String, plural: String = singular + "s"): String =
    "$count ${if (count == 1) singular else plural}"

/**
 * MedMCQA explanations were stripped of markup, gluing words together ("EstersAmidesAll").
 * Re-insert the space at lowercase->Capital joins; the 3-letter lookbehind leaves "McBurney"/"DiGeorge" alone.
 */
fun readableExplanation(text: String): String =
    text.replace(Regex("(?<=[a-z]{3})(?=[A-Z][a-z])"), " ")
