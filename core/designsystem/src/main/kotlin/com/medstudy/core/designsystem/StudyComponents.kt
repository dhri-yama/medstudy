package com.medstudy.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.layout
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
    /** Minimum card height; with [verticalArrangement] = SpaceBetween the content spreads top and bottom. */
    minHeight: Dp = 0.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    /** Soft shapes drawn behind the content (e.g. translucent circles). */
    decoration: (@Composable BoxScope.() -> Unit)? = null,
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
        decoration?.invoke(this)
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = (minHeight - 32.dp).coerceAtLeast(0.dp)),
            verticalArrangement = verticalArrangement,
            content = content,
        )
    }
}

/** Thin progress bar. [progress] is 0..1; [description] is read by screen readers ("2 of 10 questions"). */
@Composable
fun StudyProgressBar(
    progress: Float,
    description: String,
    modifier: Modifier = Modifier,
    fill: Color = StudyTheme.colors.accent,
    track: Color = StudyTheme.colors.track,
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
    /** Fill for the Primary style; pass a card's `deep` shade to match the card it sits in. */
    containerColor: Color = StudyTheme.colors.accent,
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
                containerColor = containerColor, contentColor = StudyTheme.colors.onAccent,
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

/**
 * Floating black pill with four icons. The active item is a white circle with an ink icon; the others are
 * white icons on the black. Labels are not shown but are each item's accessibility description.
 */
@Composable
fun StudyBottomBar(
    items: List<BottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, CircleShape, ambientColor = Color(0x3314142B), spotColor = Color(0x3314142B))
                .clip(CircleShape)
                .background(StudyTheme.colors.accent)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                val circle by animateColorAsState(if (selected) Color.White else Color.Transparent, tween(200), label = "navCircle")
                val tint by animateColorAsState(if (selected) StudyTheme.colors.accent else Color.White.copy(alpha = 0.9f), tween(200), label = "navTint")
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(circle)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                        .semantics { contentDescription = item.label },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                }
            }
        }
    }
}

/**
 * Top tabs in the same language as the week pills and filter pills: separate pills, the selected one solid ink with white text,
 * the others white with a hairline border and ink text.
 */
@Composable
fun SegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val fill by animateColorAsState(if (selected) StudyTheme.colors.accent else StudyTheme.colors.surface, tween(200), label = "tabFill")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .border(1.dp, if (selected) StudyTheme.colors.accent else StudyTheme.colors.hairline, CircleShape)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, style = MaterialTheme.typography.labelLarge,
                    color = if (selected) StudyTheme.colors.onAccent else StudyTheme.colors.ink, maxLines = 1,
                )
            }
        }
    }
}

/** White circular icon button (back, bookmark). 44dp, comfortably tappable. */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = StudyTheme.colors.surface,
    tint: Color = StudyTheme.colors.ink,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Large subject-colored header for detail screens: back button, title, then caller content. */
@Composable
fun HeroCard(
    gradient: SubjectGradient,
    onBack: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(gradient.brush)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconCircleButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
        Text(title, style = MaterialTheme.typography.headlineLarge, color = StudyTheme.colors.ink)
        content()
    }
}

/** Rounded text field: white fill, hairline border, ink border when focused. */
@Composable
fun StudyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    /** Show [label] as a hint inside the box instead of a floating label. Use it on colored or gradient backgrounds. */
    hintInside: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (hintInside) null else ({ Text(label) }),
        placeholder = if (hintInside) ({ Text(label) }) else null,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = StudyTheme.colors.surface,
            unfocusedContainerColor = StudyTheme.colors.surface,
            focusedBorderColor = StudyTheme.colors.ink,
            unfocusedBorderColor = StudyTheme.colors.hairline,
            focusedLabelColor = StudyTheme.colors.ink,
            unfocusedLabelColor = StudyTheme.colors.muted,
            cursorColor = StudyTheme.colors.ink,
            focusedPlaceholderColor = StudyTheme.colors.muted,
            unfocusedPlaceholderColor = StudyTheme.colors.muted,
        ),
        modifier = modifier.fillMaxWidth().then(if (hintInside) Modifier.semantics { contentDescription = label } else Modifier),
    )
}

/** Selectable pill for filters (subjects, weekdays): accent when selected, white with a hairline otherwise. */
@Composable
fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(if (selected) StudyTheme.colors.accent else StudyTheme.colors.surface)
            .border(1.dp, if (selected) StudyTheme.colors.accent else StudyTheme.colors.hairline, CircleShape)
            .selectable(selected = selected, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge, maxLines = 1,
            color = (if (selected) StudyTheme.colors.onAccent else StudyTheme.colors.ink).copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

@Composable
fun studySliderColors() = SliderDefaults.colors(
    thumbColor = StudyTheme.colors.accent,
    activeTrackColor = StudyTheme.colors.accent,
    inactiveTrackColor = StudyTheme.colors.track,
    activeTickColor = StudyTheme.colors.onAccent,
    inactiveTickColor = StudyTheme.colors.muted,
)

/** White rounded card for grouping settings or form fields on the gray canvas. */
@Composable
fun StudyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(StudyTheme.colors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Dim a control that has nothing to do right now, keeping it visible. */
fun Modifier.dimmedIf(dimmed: Boolean): Modifier = if (dimmed) alpha(0.5f) else this

/**
 * Soft layered arcs that fade from a deeper tone at the top edge to the lightest tone nearest the content,
 * in a subject gradient's ramp. Drawn in the canvas background color below, so the screen continues cleanly.
 * Geometry is authored for 196dp and scales with [height].
 */
@Composable
fun ArcBackdrop(gradient: SubjectGradient, modifier: Modifier = Modifier) {
    val bands = listOf(
        androidx.compose.ui.graphics.lerp(gradient.deep, gradient.top, 0.45f),
        gradient.top,
        androidx.compose.ui.graphics.lerp(gradient.top, gradient.bottom, 0.5f),
        gradient.bottom,
    )
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth()) {
        val scale = size.height / 196.dp.toPx()
        val rise = 160.dp.toPx() * scale
        // lightest band reaches lowest; darker bands sit on top, higher up
        val sides = listOf(112, 138, 164, 190).map { it.dp.toPx() * scale }
        for (k in 3 downTo 0) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, sides[k])
                quadraticTo(size.width / 2f, sides[k] - rise, 0f, sides[k])
                close()
            }
            drawPath(path, bands[k])
        }
    }
}

/** A full-width header: the arc backdrop with [content] laid over it (use [BoxScope] alignment for the title row). */
@Composable
fun ArcHeader(
    modifier: Modifier = Modifier,
    gradient: SubjectGradient = StudyTheme.subjectGradient(5),
    height: Dp = 196.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.fillMaxWidth().height(height)) {
        ArcBackdrop(gradient, Modifier.fillMaxSize())
        content()
    }
}

/** Lets one item of a padded list draw edge to edge by growing [horizontal] on each side. */
fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx()
    val width = constraints.maxWidth + 2 * extra
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
}
