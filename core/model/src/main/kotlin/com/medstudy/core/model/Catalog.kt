package com.medstudy.core.model

data class Category(
    val id: String,
    val parentId: String?,
    val name: String,
    val description: String? = null,
    val position: Int = 0,
)

data class MediaAsset(
    val id: String,
    val relativePath: String,
    val mimeType: String,
    val sha256: String,
    val widthPx: Int,
    val heightPx: Int,
    val contentKind: ContentKind,
    val rightsStatus: RightsStatus,
)

enum class ContentKind {
    DEMO,
    ORIGINAL,
    LICENSED,
    OFFICIAL,
}

enum class RightsStatus {
    DEMO_ONLY,
    REVIEW_REQUIRED,
    LICENSED,
    PUBLIC_DOMAIN,
    NOT_FOR_DISTRIBUTION,
}

data class ContentProvenance(
    val contentKind: ContentKind,
    val sourceName: String,
    val sourceReference: String? = null,
    val rightsStatus: RightsStatus,
    val attribution: String? = null,
)

enum class ReviewStatus {
    DEMO_NOT_CLINICALLY_REVIEWED,
    PENDING_REVIEW,
    APPROVED,
}

data class Bookmark(
    val questionId: String,
    val savedAtEpochMillis: Long,
)
