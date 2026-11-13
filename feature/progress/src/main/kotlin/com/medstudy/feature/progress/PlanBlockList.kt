package com.medstudy.feature.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.medstudy.core.designsystem.StudyTheme
import com.medstudy.core.designsystem.SubjectGradient
import com.medstudy.core.model.StudyBlock
import com.medstudy.core.model.pluralize
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** The plan's blocks in the order they are shown: by day, then by place within the day. */
fun List<StudyBlock>.inPlanOrder(): List<StudyBlock> = sortedWith(compareBy(StudyBlock::studyDateIso, StudyBlock::position))

/**
 * Moves one block to [toIndex] in the plan's order. The days themselves stay where they are: each block takes
 * the day (and place within it) of the row it ends up on, so dragging a block onto Friday's row makes it Friday's block.
 */
fun reorderPlanBlocks(blocks: List<StudyBlock>, blockId: String, toIndex: Int): List<StudyBlock> {
    val ordered = blocks.inPlanOrder()
    val from = ordered.indexOfFirst { it.id == blockId }
    val to = toIndex.coerceIn(0, (ordered.size - 1).coerceAtLeast(0))
    if (from < 0 || from == to) return blocks
    val moved = ordered.toMutableList().apply { add(to, removeAt(from)) }
    return moved.mapIndexed { index, block -> block.copy(studyDateIso = ordered[index].studyDateIso, position = ordered[index].position) }
}

private val RowSpacing = 12.dp
private val DeleteReveal = 72.dp

/**
 * The week's study blocks. Hold a row (or use its handle) and drag it up or down to give it another day;
 * swipe a row left to show its delete button.
 */
@Composable
fun PlanBlockList(
    blocks: List<StudyBlock>,
    gradientOf: @Composable (StudyBlock) -> SubjectGradient,
    onReorder: (blockId: String, toIndex: Int) -> Unit,
    onRemove: (blockId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val spacingPx = with(LocalDensity.current) { RowSpacing.toPx() }
    var rowHeightPx by remember { mutableIntStateOf(0) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    /** The row whose delete button is showing, if any. */
    var openId by remember { mutableStateOf<String?>(null) }

    val pitch = rowHeightPx + spacingPx
    val fromIndex = blocks.indexOfFirst { it.id == draggingId }
    val toIndex = if (fromIndex < 0 || pitch <= 0f) -1 else (fromIndex + (dragOffset / pitch).roundToInt()).coerceIn(0, blocks.lastIndex)
    val latestBlocks by rememberUpdatedState(blocks)
    val latestToIndex by rememberUpdatedState(toIndex)
    val latestPitch by rememberUpdatedState(pitch)

    fun startDrag(id: String) {
        openId = null
        dragOffset = 0f
        draggingId = id
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    fun drag(deltaY: Float) {
        val from = latestBlocks.indexOfFirst { it.id == draggingId }
        if (from < 0) return
        // The row cannot leave the list.
        dragOffset = (dragOffset + deltaY).coerceIn(-from * latestPitch, (latestBlocks.lastIndex - from) * latestPitch)
    }
    fun endDrag(commit: Boolean) {
        val id = draggingId
        val to = latestToIndex
        if (commit && id != null && to >= 0 && to != latestBlocks.indexOfFirst { it.id == id }) onReorder(id, to)
        draggingId = null
        dragOffset = 0f
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(RowSpacing)) {
        blocks.forEachIndexed { index, block ->
            key(block.id) {
                val dragging = block.id == draggingId
                // While a row is dragged, the rows it passes step aside, and every row shows the day it would get.
                val shownIndex = when {
                    fromIndex < 0 -> index
                    dragging -> toIndex
                    index in (fromIndex + 1)..toIndex -> index - 1
                    index in toIndex until fromIndex -> index + 1
                    else -> index
                }
                val settle by animateFloatAsState(if (dragging) 0f else (shownIndex - index) * pitch, label = "planRowShift")
                val canMoveUp = index > 0
                val canMoveDown = index < blocks.lastIndex
                PlanBlockRow(
                    block = block,
                    dateIso = blocks[shownIndex.coerceIn(0, blocks.lastIndex)].studyDateIso,
                    gradient = gradientOf(block),
                    lifted = dragging,
                    deleteShown = openId == block.id,
                    onDeleteShownChange = { shown -> openId = if (shown) block.id else if (openId == block.id) null else openId },
                    onRemove = { openId = null; onRemove(block.id) },
                    handleModifier = Modifier.pointerInput(block.id) {
                        detectDragGestures(
                            onDragStart = { startDrag(block.id) },
                            onDrag = { change, amount -> change.consume(); drag(amount.y) },
                            onDragEnd = { endDrag(commit = true) },
                            onDragCancel = { endDrag(commit = false) },
                        )
                    },
                    modifier = Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .offset { IntOffset(0, (if (dragging) dragOffset else settle).roundToInt()) }
                        .onSizeChanged { if (index == 0) rowHeightPx = it.height }
                        .pointerInput(block.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startDrag(block.id) },
                                onDrag = { change, amount -> change.consume(); drag(amount.y) },
                                onDragEnd = { endDrag(commit = true) },
                                onDragCancel = { endDrag(commit = false) },
                            )
                        }
                        // The same three things for people using a screen reader or a switch, who cannot drag or swipe.
                        .semantics {
                            customActions = listOfNotNull(
                                if (canMoveUp) CustomAccessibilityAction("Move to the day before") { onReorder(block.id, index - 1); true } else null,
                                if (canMoveDown) CustomAccessibilityAction("Move to the day after") { onReorder(block.id, index + 1); true } else null,
                                CustomAccessibilityAction("Remove from plan") { onRemove(block.id); true },
                            )
                        },
                )
            }
        }
    }
}

/** One study block: a colored date tile, the topic and its size, and a drag handle. The delete button sits behind it on the right. */
@Composable
private fun PlanBlockRow(
    block: StudyBlock,
    dateIso: String,
    gradient: SubjectGradient,
    lifted: Boolean,
    deleteShown: Boolean,
    onDeleteShownChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    handleModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val colors = StudyTheme.colors
    val date = LocalDate.parse(dateIso)
    val revealPx = with(LocalDensity.current) { DeleteReveal.toPx() }
    /** Where the finger has pulled the row sideways; null when it is not being swiped. */
    var swipe by remember { mutableStateOf<Float?>(null) }
    val rest by animateFloatAsState(if (deleteShown) -revealPx else 0f, label = "planRowSwipe")
    val x = swipe ?: rest

    Box(modifier.fillMaxWidth()) {
        if (x < 0f) {
            Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).size(56.dp).clip(CircleShape).background(colors.errorTint)
                    .clickable(role = Role.Button, onClickLabel = "Remove ${block.title} from plan", onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove from plan", tint = colors.error) }
        }
        Row(
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), 0) }
                .fillMaxWidth()
                .then(if (lifted) Modifier.shadow(10.dp, RoundedCornerShape(22.dp)) else Modifier)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .pointerInput(deleteShown) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipe = if (deleteShown) -revealPx else 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            swipe = ((swipe ?: 0f) + amount).coerceIn(-revealPx, 0f)
                        },
                        onDragEnd = { onDeleteShownChange((swipe ?: 0f) < -revealPx / 2); swipe = null },
                        onDragCancel = { swipe = null },
                    )
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier.size(width = 56.dp, height = 64.dp).clip(RoundedCornerShape(18.dp)).background(gradient.brush),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), style = MaterialTheme.typography.labelSmall, color = colors.ink)
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge, color = colors.ink)
            }
            Column(Modifier.weight(1f)) {
                Text(block.title, style = MaterialTheme.typography.titleMedium, color = colors.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${pluralize(block.questionTarget, "question")} · ${block.minuteTarget} min",
                    style = MaterialTheme.typography.bodySmall, color = colors.muted,
                )
            }
            Box(handleModifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DragIndicator, contentDescription = null, tint = colors.muted)
            }
        }
    }
}
