package com.zhzc0x.compose.linechart

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhzc0x.compose.chart.PointInfo
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Typeface

internal data class FFtResultAxisInfo(val value: Float, val descText: String){
    var drawTextX: Float = 0f
    var drawTextY: Float = 0f
    var dashLineStartOffset = Offset.Zero
    var dashLineEndOffset = Offset.Zero
}

private var viewWidth = 0f
private var viewHeight = 0f
private var xAxisPadding = 0f
private var yAxisPadding = 0f
private var drawLineWidth = 0f
private var drawLineHeight = 0f
private val lineChartPath = Path()
private var lineChartStroke = Stroke()
private val axisColor = Color(0xFF494952)
private val textPaint = org.jetbrains.skia.Paint().apply {
    isAntiAlias = true
}
private val textFont = Font().apply {
    setTypeface(Typeface.makeFromName(null, FontStyle.NORMAL))
}
private var textPadding = 0f
private val dashLineColor = Color.Gray.copy(0.4f)
private val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
private val yFFtResultAxisInfoList = ArrayList<FFtResultAxisInfo>()
private var limitLineSpace = 0f
private var xMaxValue = 0f
private var yMaxValue = 0f
private var startX: Float = 0f
private var startY: Float = 0f
private val preChannelsPointList = ArrayList<List<PointInfo>>()
private var pointAnimator = Animatable(0f, Float.VectorConverter)
private val pointAnimationSpec = tween<Float>(200, easing = LinearEasing)
internal var startAnimateTm = 0L

@Composable
internal fun FftPlotLineChartView(modifier: Modifier, color: Color, textColor: Color, xFFtResultAxisInfoList: List<FFtResultAxisInfo>,
                                  yMax: Float, channelsPointList: List<List<PointInfo>>,
                                  startAnimate: MutableState<Boolean>) = Column(modifier){
    Text("Amplitude (μV)", Modifier.padding(start = 16.dp, top = 9.dp, bottom = 8.dp), textColor, 12.sp)
    Canvas(Modifier.padding(start = 16.dp, end=16.dp).fillMaxWidth().weight(1f)){
        if(viewWidth != size.width || yMax != yMaxValue){
            initDrawData(textColor, xFFtResultAxisInfoList.last().value, yMax)
            initLineChartData(xFFtResultAxisInfoList)
        }
        drawXAxis(xFFtResultAxisInfoList)
        drawYAxis(yFFtResultAxisInfoList)
        if(channelsPointList.isNotEmpty()){
            drawLineChart(channelsPointList, color, startAnimate.value)
        }
    }
    Text("Frequency (Hz)", Modifier.padding(top = 7.dp, bottom = 6.dp).align(Alignment.CenterHorizontally), textColor, 12.sp)
    LaunchedEffect(startAnimate.value){
        if(startAnimate.value){
            pointAnimator.animateTo(1f, pointAnimationSpec)
            preChannelsPointList.clear()
            channelsPointList.forEach(preChannelsPointList::add)
            startAnimate.value = false
            println("动画执行完成：cost=${System.currentTimeMillis() - startAnimateTm}")
            pointAnimator.snapTo(0f)
        }
    }
}

private fun DrawScope.initDrawData(textColor: Color, xMax: Float, yMax: Float) {
    textPaint.color = textColor.toArgb()
    textFont.size = 12.sp.toPx()
    xMaxValue = xMax
    yMaxValue = yMax
    viewWidth = size.width
    viewHeight = size.height
    textPadding = 4.dp.toPx()
    xAxisPadding = textFont.size + textPadding
    yAxisPadding = textFont.measureTextWidth("${yMaxValue.toInt()}") + textPadding
    drawLineWidth = viewWidth - yAxisPadding
    drawLineHeight = viewHeight - xAxisPadding

    yFFtResultAxisInfoList.clear()
    yFFtResultAxisInfoList.add(FFtResultAxisInfo(0f, "0.0"))
    yFFtResultAxisInfoList.add(FFtResultAxisInfo(1f, "1.0"))
    yFFtResultAxisInfoList.add(FFtResultAxisInfo(10f, "10"))

    if(yMaxValue > 100){
        limitLineSpace = drawLineHeight / 4
        yFFtResultAxisInfoList.add(FFtResultAxisInfo(100f, "100"))
    } else if(yMaxValue > 10){
        limitLineSpace = drawLineHeight / 3
    }
    yFFtResultAxisInfoList.add(FFtResultAxisInfo(yMaxValue, "${yMaxValue.toInt()}"))
    yFFtResultAxisInfoList.forEach { FFtResultAxisInfo ->
        val y = getDrawY(FFtResultAxisInfo.value)
        val textWidth = textFont.measureTextWidth(FFtResultAxisInfo.descText)
        FFtResultAxisInfo.drawTextX = yAxisPadding - textWidth - textPadding
        FFtResultAxisInfo.drawTextY = y + textFont.size / 2
        FFtResultAxisInfo.dashLineStartOffset = Offset(yAxisPadding, y)
        FFtResultAxisInfo.dashLineEndOffset = Offset(viewWidth, y)
    }
}

private fun DrawScope.initLineChartData(xFFtResultAxisInfoList: List<FFtResultAxisInfo>){
    lineChartStroke = Stroke(1.8f.dp.toPx(), join = StrokeJoin.Round)
    //计算文本绘制信息
    xFFtResultAxisInfoList.forEach { FFtResultAxisInfo ->
        val textWidth = textFont.measureTextWidth(FFtResultAxisInfo.descText)
        FFtResultAxisInfo.drawTextX = getDrawX(FFtResultAxisInfo.value) - textWidth / 2
        FFtResultAxisInfo.drawTextY = drawLineHeight + textFont.size + textPadding
    }
}

private fun DrawScope.drawXAxis(xFFtResultAxisInfoList: List<FFtResultAxisInfo>) {
    drawLine(
        axisColor, Offset(yAxisPadding, drawLineHeight),
        Offset(viewWidth, drawLineHeight))
    xFFtResultAxisInfoList.forEachIndexed { index, FFtResultAxisInfo ->
        if(index == 0){
            return@forEachIndexed
        }
        drawIntoCanvas {
            it.nativeCanvas.drawString(FFtResultAxisInfo.descText, FFtResultAxisInfo.drawTextX, FFtResultAxisInfo.drawTextY, textFont, textPaint)
        }
    }
}

private fun DrawScope.drawYAxis(yFFtResultAxisInfoList: List<FFtResultAxisInfo>) {
    drawLine(axisColor, Offset(yAxisPadding, 0f), Offset(yAxisPadding, drawLineHeight))
    yFFtResultAxisInfoList.asReversed().forEach { FFtResultAxisInfo ->
        drawIntoCanvas {
            it.nativeCanvas.drawString(FFtResultAxisInfo.descText, FFtResultAxisInfo.drawTextX, FFtResultAxisInfo.drawTextY, textFont, textPaint)
        }
        drawLine(
            dashLineColor, FFtResultAxisInfo.dashLineStartOffset, FFtResultAxisInfo.dashLineEndOffset, 1f,
            pathEffect= dashPathEffect
        )
    }
}

private fun DrawScope.drawLineChart(channelsPointList: List<List<PointInfo>>, color: Color, startAnimate: Boolean) {
    lineChartPath.reset()
    if(startAnimate && preChannelsPointList.size == channelsPointList.size){
        channelsPointList.forEachIndexed { channelIndex, pointList ->
            pointList.forEachIndexed{ pointIndex, pointInfo ->
                val curX = getDrawX(pointInfo.x)
                val curY = getDrawY(pointInfo.y)
                val preX = getDrawX(preChannelsPointList[channelIndex][pointIndex].x)
                val preY = getDrawY(preChannelsPointList[channelIndex][pointIndex].y)
                startX = preX - (preX - curX) * pointAnimator.value
                startY = preY - (preY - curY) * pointAnimator.value
                if(pointInfo.x == 0f){
                    lineChartPath.moveTo(startX, startY)
                } else {
                    lineChartPath.lineTo(startX, startY)
                }
            }
        }
    } else {
        preChannelsPointList.forEach { pointList ->
            pointList.forEach{ pointInfo ->
                startX = getDrawX(pointInfo.x)
                startY = getDrawY(pointInfo.y)
                if(pointInfo.x == 0f){
                    lineChartPath.moveTo(startX, startY)
                } else {
                    lineChartPath.lineTo(startX, startY)
                }
            }
        }
    }
    drawPath(lineChartPath, color, style = lineChartStroke)
}

private fun getDrawX(value: Float): Float{
    return drawLineWidth * (value / xMaxValue) + yAxisPadding
}

private fun getDrawY(point: Float): Float {
    return if(point > yMaxValue){
        0f
    } else if(point > 100){
//        drawLineHeight - (limitLineSpace * 3 + limitLineSpace * (point / yMaxValue))
        drawLineHeight - limitLineSpace * (3 + point / yMaxValue)
    } else if(point > 10){
        if(yMaxValue > 100){
            drawLineHeight - limitLineSpace * (2 + point / 100)
        } else {
            drawLineHeight - limitLineSpace * (2 + point / yMaxValue)
        }
    } else if(point > 1){
        drawLineHeight - limitLineSpace * (1 + point / 10)
    } else {
        drawLineHeight - limitLineSpace * (point / 1)
    }
}

/**********************************************************************************************************************/

internal data class HistogramInfo(val descText: String, val color: Color){
    var value: Float = 0f
    var drawTextX: Float = 0f
    var drawTextY: Float = 0f
}

private val preBandPowerList = ArrayList<Float>()
private var histogramWidth = 0f
private var histogramSpace = 0f

@Composable
internal fun BandPowerHistogramView(modifier: Modifier, textColor: Color, yMax: Float,
                                    histogramInfoList: List<HistogramInfo>,
                                    startAnimate: MutableState<Boolean>) = Column(modifier){
    Text("Power-(μV)/Hz", Modifier.padding(start = 16.dp, top = 9.dp, bottom = 8.dp), textColor, 12.sp)
    Canvas(Modifier.padding(start = 16.dp, end=16.dp).fillMaxWidth().weight(1f)){
        if(viewWidth != size.width || yMax != yMaxValue){
            initDrawData(textColor, xMaxValue, yMax)
            initHistogramData(histogramInfoList)
        }
        drawYAxis(yFFtResultAxisInfoList)
        drawHistogram(histogramInfoList, startAnimate.value)
    }
    Text("EEG Power Bands", Modifier.padding(top=6.dp, bottom = 7.dp).align(Alignment.CenterHorizontally),
        textColor, 12.sp)
    LaunchedEffect(startAnimate.value){
        if(startAnimate.value){
            pointAnimator.animateTo(1f, pointAnimationSpec)
            preBandPowerList.clear()
            histogramInfoList.forEach{ preBandPowerList.add(it.value) }
            startAnimate.value = false
            println("动画执行完成：cost=${System.currentTimeMillis() - startAnimateTm}")
            pointAnimator.snapTo(0f)
        }
    }
}

private fun DrawScope.initHistogramData(histogramInfoList: List<HistogramInfo>){
    histogramSpace = 7.dp.toPx()
    histogramWidth = (drawLineWidth - histogramSpace * 5) / 5
    //计算文本绘制信息
    histogramInfoList.forEachIndexed { index, histogramInfo ->
        val textWidth = textFont.measureTextWidth(histogramInfo.descText)
        val x = yAxisPadding + histogramSpace + index * (histogramWidth + histogramSpace)
        histogramInfo.drawTextX = x + (histogramWidth - textWidth) / 2
        histogramInfo.drawTextY = drawLineHeight + textFont.size + textPadding
    }
    if(preBandPowerList.isEmpty()){
        histogramInfoList.forEach{ preBandPowerList.add(it.value) }
    }
}
private fun DrawScope.drawHistogram(histogramInfoList: List<HistogramInfo>, startAnimate: Boolean) {
    //先绘制X轴线
    drawLine(axisColor, Offset(yAxisPadding, drawLineHeight), Offset(viewWidth, drawLineHeight))
    //再绘制描述文字和柱状图
    if(startAnimate){
        histogramInfoList.forEachIndexed { index, histogramInfo ->
            val curY = getDrawY(histogramInfo.value)
            val preY = getDrawY(preBandPowerList[index])
            startX = yAxisPadding + histogramSpace + index * (histogramWidth + histogramSpace)
            startY = preY - (preY - curY) * pointAnimator.value
            drawRect(histogramInfo.color, Offset(startX, startY), Size(histogramWidth, drawLineHeight - startY))
            drawIntoCanvas {
                it.nativeCanvas.drawString(histogramInfo.descText, histogramInfo.drawTextX, histogramInfo.drawTextY,
                    textFont, textPaint
                )
            }
        }
    } else {
        histogramInfoList.forEachIndexed { index, histogramInfo ->
            startX = yAxisPadding + histogramSpace + index * (histogramWidth + histogramSpace)
            startY = getDrawY(histogramInfo.value)
            drawRect(histogramInfo.color, Offset(startX, startY), Size(histogramWidth, drawLineHeight - startY))
            drawIntoCanvas {
                it.nativeCanvas.drawString(histogramInfo.descText, histogramInfo.drawTextX, histogramInfo.drawTextY,
                    textFont, textPaint
                )
            }
        }
    }
}

internal fun releaseFftResultView(){
    viewWidth = 0f
    viewHeight = 0f
    yFFtResultAxisInfoList.clear()
    preChannelsPointList.clear()
    preBandPowerList.clear()
}
