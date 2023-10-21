package com.zhzc0x.compose.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class AxisInfo(val scaleInfoList: List<ScaleInfo>,
                    val title: String = "",
                    val titleColor: Color = Color.Black,
                    val titleSize: TextUnit = 14.sp,
                    val axisColor: Color = Color.Black,
                    val axisWidth: Dp = 1.4.dp,
                    val scaleTextColor: Color = titleColor,
                    val scaleTextSize: TextUnit = 14.sp,
                    val scaleTextPadding: Dp = 4.dp,
                    val showDashLine: Boolean = true,
                    val showScaleLine: Boolean = true,
                    val scaleLineColor: Color = axisColor,
                    val scaleLineWidth: Dp = 1.2.dp,
                    val scaleLineLength: Dp = 6.dp)

data class ScaleInfo(val value: Float,
                     val text: String){
    var drawTextOffset = Offset.Zero
    var drawScaleLineStart = Offset.Zero
    var drawScaleLineEnd = Offset.Zero
    var drawDashLineStart = Offset.Zero
    var drawDashLineEnd = Offset.Zero
}

data class PointInfo(val x: Float, val y: Float)

private var viewSize = Size.Zero
private var xAxisPaddingBottom = 0f
private var yAxisPaddingStart = 0f
private var yAxisPaddingEnd = 0f
private var drawLineWidth = 0f
private var drawLineHeight = 0f
private val lineChartPath = Path()
private var lineChartStroke = Stroke()
private val dashLineColor = Color.Gray.copy(0.4f)
private val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
private var xMaxValue = 0f
private var xMinValue = 0f
private var yMaxValue = 0f
private var yMinValue = 0f
private var startX: Float = 0f
private var startY: Float = 0f

private var xTextStyle = TextStyle()
private var yTextStyle = TextStyle()

/**
 * 静态折线图
 * @param modifier Modifier
 * @param pointLists 多折线点List数据
 * @param xAxisInfo X轴信息
 * @param yAxisInfo Y轴信息
 * @see com.zhzc0x.compose.chart.AxisInfo
 * @param colors 对应每个折线颜色，设置colors.size必须等于pointLists.size，默认每个折线颜色Color.Black
 * @param lineChartWidth 折线宽度
 *
 * */
@Composable
fun LineChartView(modifier: Modifier, pointLists: List<List<PointInfo>>, xAxisInfo: AxisInfo, yAxisInfo: AxisInfo,
                  colors: Array<Color> = Array(pointLists.size){ Color.Black }, lineChartWidth: Dp = 1.8.dp)
= Column(modifier){
    Text(yAxisInfo.title, Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), yAxisInfo.titleColor, yAxisInfo.titleSize)
    val textMeasurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().weight(1f)){
        if(viewSize != size){
            initDrawData(xAxisInfo, yAxisInfo, lineChartWidth, textMeasurer)
        }
        drawAxisInfo(xAxisInfo, Offset(yAxisPaddingStart, drawLineHeight),
            Offset(viewSize.width - yAxisPaddingEnd, drawLineHeight), textMeasurer)
        drawAxisInfo(yAxisInfo, Offset(yAxisPaddingStart, 0f), Offset(yAxisPaddingStart, drawLineHeight), textMeasurer)
        pointLists.forEachIndexed { index, pointList ->
            drawLineChart(pointList, colors[index])
        }
    }
    Text(xAxisInfo.title, Modifier.padding(top=12.dp, bottom = 12.dp).align(Alignment.CenterHorizontally), xAxisInfo.titleColor, xAxisInfo.titleSize)
}

private fun DrawScope.initDrawData(xAxisInfo: AxisInfo, yAxisInfo: AxisInfo, lineChartWidth: Dp, textMeasurer: TextMeasurer) {
    viewSize = size
    xMaxValue = xAxisInfo.scaleInfoList.last().value
    xMinValue = xAxisInfo.scaleInfoList.first().value
    yMaxValue = yAxisInfo.scaleInfoList.last().value
    yMinValue = yAxisInfo.scaleInfoList.first().value
    xTextStyle = TextStyle(xAxisInfo.scaleTextColor, xAxisInfo.scaleTextSize, FontWeight.Medium)
    yTextStyle = TextStyle(yAxisInfo.scaleTextColor, yAxisInfo.scaleTextSize, FontWeight.Medium)
    val xScaleTextPadding = xAxisInfo.scaleTextPadding.toPx() + xAxisInfo.scaleLineLength.toPx()
    val yScaleTextPadding = yAxisInfo.scaleTextPadding.toPx() + yAxisInfo.scaleLineLength.toPx()
    val xTextSize = textMeasurer.measure("-$xMaxValue", xTextStyle).size
    val yTextSize = textMeasurer.measure("-$yMaxValue", yTextStyle).size
    xAxisPaddingBottom = xTextSize.height + xScaleTextPadding
    yAxisPaddingStart = yTextSize.width + yScaleTextPadding
    yAxisPaddingEnd = xTextSize.width / 2f
    drawLineWidth = viewSize.width - yAxisPaddingStart - yAxisPaddingEnd
    drawLineHeight = viewSize.height - xAxisPaddingBottom
    lineChartStroke = Stroke(lineChartWidth.toPx(), join = StrokeJoin.Round)
    xAxisInfo.scaleInfoList.forEach { scaleInfo ->
        val x = getDrawX(scaleInfo.value)
        val textWidth = textMeasurer.measure(scaleInfo.text, xTextStyle).size.width
        scaleInfo.drawTextOffset = Offset(getDrawX(scaleInfo.value) - textWidth / 2, drawLineHeight + xScaleTextPadding)
        scaleInfo.drawScaleLineStart = Offset(x, drawLineHeight)
        scaleInfo.drawScaleLineEnd = Offset(x, drawLineHeight + xAxisInfo.scaleLineLength.toPx())
        scaleInfo.drawDashLineStart = Offset(x, 0f)
        scaleInfo.drawDashLineEnd = Offset(x, drawLineHeight)
    }
    yAxisInfo.scaleInfoList.forEach { scaleInfo ->
        val y = getDrawY(scaleInfo.value)
        val measureSize = textMeasurer.measure(scaleInfo.text, yTextStyle).size
        scaleInfo.drawTextOffset = Offset(yAxisPaddingStart - measureSize.width - yScaleTextPadding, y - measureSize.height / 2)
        scaleInfo.drawScaleLineStart = Offset(yAxisPaddingStart - yAxisInfo.scaleLineLength.toPx(), y)
        scaleInfo.drawScaleLineEnd = Offset(yAxisPaddingStart, y)
        scaleInfo.drawDashLineStart = Offset(yAxisPaddingStart, y)
        scaleInfo.drawDashLineEnd = Offset(viewSize.width - yAxisPaddingEnd, y)
    }
}

private fun DrawScope.drawAxisInfo(axisInfo: AxisInfo, start: Offset, end: Offset, textMeasurer: TextMeasurer) {
    drawLine(axisInfo.axisColor, start, end, axisInfo.axisWidth.toPx())
    axisInfo.scaleInfoList.forEach { scaleInfo ->
        drawText(textMeasurer, scaleInfo.text, scaleInfo.drawTextOffset)
        if(axisInfo.showDashLine){
            drawLine(dashLineColor, scaleInfo.drawDashLineStart, scaleInfo.drawDashLineEnd, 1.2f,
                pathEffect= dashPathEffect)
        }
        if(axisInfo.showScaleLine){
            drawLine(axisInfo.scaleLineColor, scaleInfo.drawScaleLineStart, scaleInfo.drawScaleLineEnd,
                axisInfo.scaleLineWidth.toPx())
        }
    }
}

private fun DrawScope.drawLineChart(pointList: List<PointInfo>, color: Color) {
    lineChartPath.reset()
    pointList.forEachIndexed { index, point ->
        startX = getDrawX(point.x)
        startY = getDrawY(point.y)
        //绘制折线
        if(index == 0){
            lineChartPath.moveTo(startX, startY)
        } else {
            lineChartPath.lineTo(startX, startY)
        }
    }
    drawPath(lineChartPath, color, style = lineChartStroke)
}

private fun getDrawX(value: Float): Float{
    return drawLineWidth * (value / (xMaxValue - xMinValue)) + yAxisPaddingStart
}

private fun getDrawY(point: Float): Float {
    val temp = when {
        point > yMaxValue -> yMaxValue
        point < yMinValue -> yMinValue
        else -> point
    }
    //处理负值的情况，但是point的y值必须在最大值和最小值之间: drawSingleHeight - drawSingleHeight * ((temp - yMin) / (yMax - yMin))
    return drawLineHeight * (1 - (temp - yMinValue) / (yMaxValue - yMinValue))
}