package com.medstudy.app

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.dhriyama.blobmates.BlobmateOptions
import io.github.dhriyama.blobmates.Shape
import io.github.dhriyama.blobmates.android.LiveBlobmateView
import io.github.dhriyama.blobmates.runtime.ReactOptions

/** What the blob is generated from: the account id when signed in (unique and stable), else empty so [ProfileBlob] falls back to the name. */
val LocalAvatarSeed = staticCompositionLocalOf { "" }

/**
 * The user's avatar: the same seed always gives the same blob (an avatar maker can later pass its own pins).
 * It blinks, its eyes follow your touch and it bounces when tapped; the library stops all motion when "Remove animations" is on.
 */
@Composable
fun ProfileBlob(name: String, size: Dp, modifier: Modifier = Modifier) {
    // The blob labels itself with its seed, which is the account id; screen readers get the learner's name instead.
    val label = if (name.isBlank()) "Your avatar" else "$name's avatar"
    Box(modifier.clearAndSetSemantics { contentDescription = label }) {
        LiveBlobmateView(
            LocalAvatarSeed.current.ifBlank { name }.ifBlank { "MedStudy" }, size = size, mood = "happy",
            pins = BlobmateOptions(shape = Shape.Circle), react = ReactOptions(),
        )
    }
}
