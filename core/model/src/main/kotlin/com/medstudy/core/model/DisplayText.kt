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

private val SourceTagBody = Regex("""[A-Z][A-Za-z0-9 ;,()\-]{2,80}""")

/**
 * MedMCQA appends the exam sources to the stem ("Bruxism is: NEET 13", "...in: AP 07; MP 09; NEET 13").
 * Returns the stem without that tag plus the tag itself, or the text unchanged with a null tag.
 * Only splits after a final ':', '?' or '.', and only when the tail ends in a two-digit year, so stems that merely end in a number are left alone.
 */
fun splitSourceTag(text: String): Pair<String, String?> {
    val trimmed = text.trimEnd()
    val cut = trimmed.indexOfLast { it == ':' || it == '?' || it == '.' }
    if (cut < 0) return text to null
    val tail = trimmed.substring(cut + 1).trim()
    val looksLikeTag = tail.isNotEmpty() && SourceTagBody.matches(tail) && Regex("""\d{2}$""").containsMatchIn(tail)
    return if (looksLikeTag) trimmed.substring(0, cut + 1).trimEnd() to tail else text to null
}
