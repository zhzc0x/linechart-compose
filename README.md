# LineChartView-Compose

## Compose 折线图表，静态折线图表LineChartView，实时多通道波形LiveMultiLineChartView ，附带OpenBCI滤波功能

![](https://github.com/zhzc0x/linechart-compose/blob/master/demo.gif)

## 代码示例

LiveMultiLineChartView 

```kotlin
private const val channels = 8
private val lineChartDataFlow = MutableStateFlow(FloatArray(channels))
private var lineChartStaticRefresh by mutableStateOf(false)
private var yMaxValue by mutableStateOf(100f)

@Composable
fun LiveMultiLineChartView(){
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
    LiveMultiLineChartView(Modifier.fillMaxSize(), lineChartDataFlow, channels,
                           Color.DarkGray, yMaxValue, -yMaxValue, lineChartStaticRefresh)
}

fun start(){
    thread{
        while(true){
            Thread.sleep(4)
            val data = (0..1000).random() / 10f
            lineChartDataFlow.tryEmit(data)
        }
    }
}
```

LineChartView

```kotlin
private val pointLists = Array(250){ i ->
    PointInfo(i.toFloat() * 4 , (sin(2 * Math.PI * 50.0 * (i / 250.0))).toFloat() * 2)
}.toList()
private val xAxisInfo = AxisInfo(listOf(
    ScaleInfo(0f, "0"),
    ScaleInfo(200f, "200"),
    ScaleInfo(400f, "400"),
    ScaleInfo(600f, "600"),
    ScaleInfo(800f, "800"),
    ScaleInfo(1000f, "1000")
), "Time (ms)")
private val yAxisInfo = AxisInfo(listOf(
    ScaleInfo(-10f, "-10"),
    ScaleInfo(-5f, "-5"),
    ScaleInfo(0f, "0"),
    ScaleInfo(5f, "5"),
    ScaleInfo(10f, "10"),
), "Amplitude (μV)", showScaleLine = false)

private val lineChartColors = arrayOf(
    Color.Gray,
    Color(0xFFFFA500),
)

@Composable
fun LineChartView(){
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
    LineChartView(Modifier.fillMaxSize(), listOf(pointLists), xAxisInfo, yAxisInfo, lineChartColors)
}
```

