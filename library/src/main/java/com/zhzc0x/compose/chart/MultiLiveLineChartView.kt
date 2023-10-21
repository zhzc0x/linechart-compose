package com.zhzc0x.compose.chart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface

private class Point(var value: Float) {
    var marker: Boolean = false
}

private var viewWidth = 0f
private var viewHeight = 0f
private var pointSpace = 0f//折线点间距
private var screenMaxPointCount = 0
private var drawSingleHeight = 0f
private var lineChartSpace = 0f//每条折线上下间距，避免绘制重合
private var lineChartWidth = 0f
private var lineChartStroke = Stroke()
private val lineChartPath = Path()
private val dashedColor = Color.Gray.copy(0.4f)
private val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
private val textPaint = org.jetbrains.skia.Paint().apply {
    isAntiAlias = true
}
private val textFont = Font().apply {
    setTypeface(Typeface.makeFromName(null, FontStyle.NORMAL))
}
private const val INIT_X = 24f
private var startX: Float = 0f
private var startY: Float = 0f

private var pointListArray: Array<MutableList<Point>> = emptyArray()
private var updateLineChart by mutableStateOf(false)
private var refreshPointIndex = 0
private var staticRefresh: Boolean = false//静态折线刷新模式

/**
 * 实时多折线图
 * @param modifier Modifier
 * @param lineChartDataFlow 折线数据flow
 * @param lineChartNum 折线数
 * @param lineChartColor 折线颜色
 * @param yMaxValue Y轴区间点最大值
 * @param yMinValue Y轴区间点最小值
 * @param isStaticRefresh 静态波形刷新模式
 * @param showSerialNum 绘制通道序列号
 * @param numColor 序列号颜色
 * @param numSize 序列号size
 *
 * */
@Composable
fun LiveMultiLineChartView(modifier: Modifier = Modifier, lineChartDataFlow: Flow<FloatArray>, lineChartNum: Int,
                           lineChartColor: Color = Color.LightGray, yMaxValue: Float, yMinValue: Float,
                           isStaticRefresh: Boolean = false, showSerialNum: Boolean = true,
                           numColor: Color = Color.DarkGray, numSize: TextUnit = 18.sp) {
    Canvas(modifier) {
        if (viewWidth != size.width || viewHeight != size.height) {
            initDrawData(lineChartNum)
            textPaint.color = numColor.toArgb()
            textFont.size = numSize.toPx()
        }
        if (staticRefresh != isStaticRefresh) {
            switchRefreshMode(isStaticRefresh)
        }
        updateLineChart
        lineChartPath.reset()
        pointListArray.forEachIndexed { channelIndex, pointList ->
            val basicLineY = getDrawY(0f, yMaxValue, yMinValue, channelIndex)
            drawLine(dashedColor, Offset(INIT_X, basicLineY), Offset(viewWidth, basicLineY), 1f,
                pathEffect = dashPathEffect)
            if (showSerialNum) {
                val serialNum = "${channelIndex + 1}"
                drawIntoCanvas {
                    it.nativeCanvas.drawString(serialNum, 0f, basicLineY + 10f, textFont, textPaint)
                }
            }
            pointList.forEachIndexed { index, point ->
                startX = getDrawX(index)
                startY = getDrawY(point.value, yMaxValue, yMinValue, channelIndex)
                //绘制折线
                if (index == 0) {
                    lineChartPath.moveTo(startX, startY)
                } else {
                    lineChartPath.lineTo(startX, startY)
                }
                if (point.marker && channelIndex == lineChartNum - 1) {
                    drawLine(Color.Red, Offset(startX, 0f), Offset(startX, viewHeight), lineChartWidth)
                }
            }
        }
        drawPath(lineChartPath, lineChartColor, style = lineChartStroke)
    }
    LaunchedEffect(Unit){
        lineChartDataFlow.onCompletion {
            release()
        }.collect{ dataArray ->
            addToPoints(dataArray)
        }
    }
}

private fun DrawScope.initDrawData(lineChartNum: Int) {
    viewWidth = size.width
    viewHeight = size.height
    lineChartWidth = 1.2f.dp.toPx()
    lineChartStroke = Stroke(lineChartWidth, join = StrokeJoin.Round)
    pointSpace = 1f.dp.toPx()
    screenMaxPointCount = ((viewWidth - INIT_X) / pointSpace).toInt()
    lineChartSpace = 8.dp.toPx()
    drawSingleHeight = (size.height - lineChartSpace * (lineChartNum - 1)) / lineChartNum
    if (lineChartNum != pointListArray.size) {
        pointListArray = Array(lineChartNum) {
            return@Array ArrayList<Point>()
        }
    }
//    LogUtil.d("size=$size, lineChartWidth=$lineChartWidth, screenMaxPointCount=$screenMaxPointCount")
    switchRefreshMode(staticRefresh)//强制切换刷新模式，防止静态刷新状态下绘制错误
}

private fun switchRefreshMode(isStaticRefresh: Boolean) {
//    LogUtil.d("switchRefreshMode: isStaticRefresh=$isStaticRefresh")
    staticRefresh = isStaticRefresh
    if (staticRefresh) {
        refreshPointIndex = 0
        pointListArray.forEach { pointList ->
            while (pointList.size - screenMaxPointCount >= 1) {
                pointList.removeAt(0)
            }
        }
    }
}

private fun getDrawX(pointIndex: Int): Float {
    return pointSpace * pointIndex + INIT_X
}

private fun getDrawY(point: Float, yMax: Float, yMin: Float, lineChartIndex: Int): Float {
    val temp = when {
        point > yMax -> yMax
        point < yMin -> yMin
        else -> point
    }
    //处理负值的情况，但是point的y值必须在最大值和最小值之间: drawSingleHeight - drawSingleHeight * ((temp - yMin) / (yMax - yMin)) + lineChartIndex * drawSingleHeight
    return drawSingleHeight * (1 - (temp - yMin) / (yMax - yMin) + lineChartIndex) + lineChartSpace * lineChartIndex
}

private fun addToPoints(dataArray: FloatArray) {
    pointListArray.forEachIndexed { channelIndex, pointList ->
        if (staticRefresh && pointList.size >= screenMaxPointCount) {
            pointList[refreshPointIndex].value = dataArray[channelIndex]
            pointList[refreshPointIndex].marker = false
        } else {
            while (pointList.size - screenMaxPointCount >= 1) {
                pointList.removeAt(0)
            }
            pointList.add(Point(dataArray[channelIndex]))
        }
    }
    if (staticRefresh) {
        refreshPointIndex++
        if (refreshPointIndex >= screenMaxPointCount) {
            refreshPointIndex = 0
        }
    }
    updateLineChart = !updateLineChart
}

fun setMarkerToPoints() {
    if (staticRefresh) {
        val markerPointIndex = if (refreshPointIndex > 0) {
            refreshPointIndex - 1
        } else {
            0
        }
        pointListArray.forEach { points ->
            points[markerPointIndex].marker = true
        }
    } else {
        pointListArray.forEach { points ->
            points.last().marker = true
        }
    }
}

private fun release() {
    println("MultiLiveLineChartView release")
    refreshPointIndex = 0
    pointListArray.forEach { it.clear() }
}