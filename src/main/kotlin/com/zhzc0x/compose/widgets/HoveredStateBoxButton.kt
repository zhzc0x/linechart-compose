package com.zhzc0x.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun HoveredStateBoxButton(onClick: () -> Unit, modifier: Modifier, shape: Shape = RoundedCornerShape(11.dp),
                                   normalColor: Color = MaterialTheme.colors.surface,
                                   hoveredColor: Color = MaterialTheme.colors.secondary,
                                   enabled: Boolean = true, content: @Composable BoxScope.(Boolean) -> Unit){
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val hoveredState = interactionSource.collectIsHoveredAsState()
    val bgColor = if (enabled && hoveredState.value) {
        hoveredColor
    } else {
        normalColor
    }

    Box(modifier.background(bgColor, shape).hoverable(interactionSource).clickable(enabled){
        onClick()
    }){
        content(enabled && hoveredState.value)
    }
}