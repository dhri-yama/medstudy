package com.medstudy.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.medstudy.core.designsystem.StudyTheme

/** "Privacy policy" and "Terms" links. The addresses are PRIVACY_URL and TERMS_URL in gradle.properties; a blank one is left out. */
@Composable
fun LegalLinks(modifier: Modifier = Modifier, color: Color = StudyTheme.colors.muted) {
    val links = listOf("Privacy policy" to BuildConfig.PRIVACY_URL, "Terms" to BuildConfig.TERMS_URL).filter { it.second.isNotBlank() }
    if (links.isEmpty()) return
    val context = LocalContext.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        links.forEach { (label, url) ->
            Text(
                label, style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline), color = color,
                modifier = Modifier.clip(CircleShape)
                    .clickable(role = Role.Button) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}
