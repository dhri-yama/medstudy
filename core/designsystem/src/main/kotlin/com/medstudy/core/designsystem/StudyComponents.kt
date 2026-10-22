package com.medstudy.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val NotchWidthFraction = 0.38f
private val NotchHeight = 44.dp

/**
 * The lighter corner block of a notched card, with inverted-corner fillets where it meets the card's top and right edges.
 * Draw it over a card that has a 24dp corner radius.
 */
private class NotchTabShape(
    private val cardRadius: Dp = 24.dp,
    private val fillet: Dp = 16.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val r = with(density) { cardRadius.toPx() }
        val f = with(density) { fillet.toPx() }
        val nw = w * NotchWidthFraction
        val nh = with(density) { NotchHeight.toPx() }
        fun arc(cx: Float, cy: Float, start: Float, sweep: Float) =
            Rect(cx - f, cy - f, cx + f, cy + f) to (start to sweep)
        val path = Path().apply {
            moveTo(w - nw - f, 0f)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), -90f, 90f, false)
            lineTo(w, nh + f)
            arc(w - f, nh + f, 0f, -90f).let { (rect, a) -> arcTo(rect, a.first, a.second, false) }
            lineTo(w - nw + f, nh)
            arc(w - nw + f, nh - f, 90f, 90f).let { (rect, a) -> arcTo(rect, a.first, a.second, false) }
            lineTo(w - nw, f)
            arc(w - nw - f, f, 0f, -90f).let { (rect, a) -> arcTo(rect, a.first, a.second, false) }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Subject-colored card with a lighter corner tab (DESIGN.md "Notched card").
 * [tab] is drawn centered in the tab block, which is 38% of the card width and 44dp tall.
 */
@Composable
fun NotchedCard(
    gradient: SubjectGradient,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tab: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient.brush)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    ) {
        if (tab != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth(NotchWidthFraction)
                    .height(NotchHeight)
                    .background(Color.White.copy(alpha = 0.35f), NotchTabShape()),
                contentAlignment = Alignment.Center,
                content = tab,
            )
        }
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** Thin progress bar. [progress] is 0..1; [description] is read by screen readers ("2 of 10 questions"). */
@Composable
fun StudyProgressBar(
    progress: Float,
    description: String,
    modifier: Modifier = Modifier,
    fill: Color = StudyTheme.colors.accent,
    track: Color = StudyTheme.colors.accentSoft,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(track)
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(CircleShape).background(fill))
    }
}

enum class PillStyle { Primary, Secondary }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    val height = if (small) 32.dp else 44.dp
    val padding = PaddingValues(horizontal = if (small) 14.dp else 20.dp)
    val sized = modifier.heightIn(min = height)
    val label: @Composable () -> Unit = {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    when (style) {
        PillStyle.Primary -> Button(
            onClick = onClick, modifier = sized, enabled = enabled, shape = CircleShape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = StudyTheme.colors.accent, contentColor = StudyTheme.colors.onAccent,
            ),
        ) { label() }
        PillStyle.Secondary -> OutlinedButton(
            onClick = onClick, modifier = sized, enabled = enabled, shape = CircleShape, contentPadding = padding,
            border = BorderStroke(1.dp, StudyTheme.colors.hairline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = StudyTheme.colors.surface, contentColor = StudyTheme.colors.ink,
            ),
        ) { label() }
    }
}

@Composable
fun StudyChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = StudyTheme.colors.surface,
    contentColor: Color = StudyTheme.colors.ink,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier.clip(CircleShape).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = contentColor, maxLines = 1)
    }
}

/** Circle with the user's first initial; there are no photos in MedStudy. */
@Composable
fun StudyAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(StudyTheme.colors.accentSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = StudyTheme.colors.ink,
        )
    }
}

/** White row on the gray canvas: leading slot, title and caption, trailing slot. */
@Composable
fun TaskRow(
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = Color(0x0A14142B), spotColor = Color(0x0A14142B))
            .clip(shape)
            .background(StudyTheme.colors.surface)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leading != null) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(StudyTheme.colors.canvas),
                contentAlignment = Alignment.Center,
            ) { leading() }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = StudyTheme.colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = StudyTheme.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke()
    }
}

data class BottomBarItem(val label: String, val icon: ImageVector)

/** White bottom bar: icon above label, active item in the accent color, no indicator pill. */
@Composable
fun StudyBottomBar(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = StudyTheme.colors.surface, shadowElevation = 8.dp, modifier = modifier) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val color = if (selected) StudyTheme.colors.accent else StudyTheme.colors.muted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    Text(
                        item.label, style = MaterialTheme.typography.labelSmall, color = color,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Dim a control that has nothing to do right now, keeping it visible. */
fun Modifier.dimmedIf(dimmed: Boolean): Modifier = if (dimmed) alpha(0.5f) else this
