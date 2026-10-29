package com.medstudy.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Space the floating bottom bar covers, so scrolling screens can leave room for it at the end of their content. */
val LocalNavBarInset = compositionLocalOf { 0.dp }

fun PaddingValues20(bottomInset: Dp) = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp + bottomInset)
