package com.zhzc0x.compose.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun <T> DropdownMenuList(
    expanded: MutableState<Boolean>, dataList: List<T>,
    onItemClick: (Int, T) -> Unit, textSuffix: String = "",
    item: @Composable (Int, T) -> Unit = @Composable { _, data ->
        Text("${data}$textSuffix")
    }
) {
    DropdownMenu(expanded.value, {
        expanded.value = false
    }) {
        dataList.forEachIndexed { index, data ->
            DropdownMenuItem({
                onItemClick(index, data)
                expanded.value = false
            }) {
                item(index, data)
            }
        }
    }
}

@Composable
fun <T> DropdownMenuListBox(
    modifier: Modifier, dataList: List<T>, expanded: MutableState<Boolean> = remember { mutableStateOf(false) },
    defaultSelected: T = dataList[0], onItemClick: (Int, T) -> Unit, textSuffix: String = "",
    textColor: Color = Color.Black, fontSize: TextUnit = 16.sp, fontWeight: FontWeight = FontWeight.Bold,
    paddingValues: PaddingValues = PaddingValues(start = 9.dp, top = 6.dp, bottom = 6.dp),
    item: @Composable (Int, T) -> Unit = @Composable { _, data ->
        Text("${data}$textSuffix", color=textColor, fontSize=fontSize, fontWeight=FontWeight.Bold)
    }
) = Box(modifier.clickable {
    expanded.value = true
}.padding(paddingValues)) {
    var selected by remember(defaultSelected) { mutableStateOf(defaultSelected) }
    Text("${selected}$textSuffix",Modifier.align(Alignment.CenterStart),textColor,fontSize,fontWeight=fontWeight)
    Icon(Icons.Filled.ArrowDropDown, "", Modifier.align(Alignment.CenterEnd).padding(end = 6.dp), textColor)
    DropdownMenu(expanded.value, {
        expanded.value = false
    }) {
        dataList.forEachIndexed { index, data ->
            DropdownMenuItem({
                selected = data
                expanded.value = false
            }) {
                item(index, data)
            }
        }
    }
    LaunchedEffect(selected){
        onItemClick(dataList.indexOf(selected), selected)
    }
}