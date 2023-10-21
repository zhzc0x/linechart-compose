package com.zhzc0x.compose.linechart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import com.zhzc0x.compose.base.ComposeBaseWindow
import com.zhzc0x.compose.chart.*
import com.zhzc0x.compose.linechart.filter.DataHandler
import com.zhzc0x.compose.linechart.filter.EegDataFilter
import com.zhzc0x.compose.widgets.CustomTextField
import com.zhzc0x.compose.widgets.DropdownMenuListBox
import com.zhzc0x.compose.widgets.HoveredStateBoxButton
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Timer
import java.util.TimerTask
import kotlin.math.floor
import kotlin.math.sin

private const val channels = 8
private const val samplingRate = 250
private val lineChartDataFlow = MutableStateFlow(FloatArray(channels))

private const val FFT_PLOT_CHANNEL_ALL = "All"
private var startState by mutableStateOf(false)
private var yMaxValue by mutableStateOf(100f)
private var lineChartStaticRefresh by mutableStateOf(false)
private var fftResultViewAnimated = mutableStateOf(false)
private var fftPlotChannelIndex = -1
private val eegDataFilter = EegDataFilter()
private val surfaceLineColor = Color(0xFF0E0E10)
private val fftResultXAxisInfoList = ArrayList<FFtResultAxisInfo>()
private val channelsPointList = ArrayList<List<PointInfo>>()
private val histogramInfoList = ArrayList<HistogramInfo>().apply {
    add(HistogramInfo("DELTA", Color(0xFF8199C8)))
    add(HistogramInfo("THETA", Color(0xFFB58FBB)))
    add(HistogramInfo("ALPHA", Color(0xFF77AA99)))
    add(HistogramInfo("BETA", Color(0xFFEDCF5B)))
    add(HistogramInfo("GAMMA", Color(0xFFFA7F7C)))
}

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
fun LineChartDemoPage(lineChartColor: Color = Color.DarkGray, resizable: Boolean = true, onClose: () -> Unit)
= ComposeBaseWindow(onClose, WindowPlacement.Maximized, title = "", resizable=resizable, alwaysOnTop=true){
    Box(Modifier.fillMaxSize().background(MaterialTheme.colors.background)){
        Box(Modifier.padding(start=80.dp).size(172.dp, 68.dp).background(MaterialTheme.colors.primary,
            RoundedCornerShape(bottomStart=11.dp, bottomEnd=11.dp))){
            Text("linechart-compose", Modifier.padding(top=10.dp).align(Alignment.TopCenter), MaterialTheme.colors.onPrimary, 14.sp)
            Text("折线图表", Modifier.padding(bottom=10.dp).align(Alignment.BottomCenter),
                MaterialTheme.colors.onPrimary, 22.sp, fontWeight=FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RealTimeBrainwavesLayout(Modifier.padding(start=40.dp, top=120.dp, bottom=80.dp).fillMaxHeight()
                .weight(0.73f), lineChartColor)
            Column(Modifier.padding(start=16.dp, top=120.dp, end=40.dp, bottom=80.dp).fillMaxHeight().weight(0.27f)) {
                FftResultLayout()
                Box(Modifier.padding(top=16.dp).width(320.dp).aspectRatio(1.4f)){
                    Text("LineChartView", Modifier.align(Alignment.TopCenter).padding(start=12.dp, top=32.dp),
                        Color.LightGray, 36.sp, fontWeight=FontWeight.Bold)
                    LineChartView(Modifier.fillMaxSize(), listOf(pointLists), xAxisInfo, yAxisInfo, lineChartColors)
                }
                Box(Modifier.padding(16.dp)) {
                    Button(onClick = {
                        setMarkerToPoints()
                    }){
                        Text("showMark")
                    }
                }
            }
        }
        DisposableEffect(Unit){
            start()
            startMockLineChartData()
            onDispose {
                stopMockLineChartData()
                stop()
            }
        }
    }
}

@Composable
private fun RealTimeBrainwavesLayout(modifier: Modifier, lineChartColor: Color)
= Box(modifier.background(MaterialTheme.colors.surface, RoundedCornerShape(16.dp))) {
    Row(Modifier.height(52.dp).padding(start=16.dp, top=10.dp), verticalAlignment = Alignment.CenterVertically) {
        val textColor = MaterialTheme.colors.onSurface
        val hintColor = MaterialTheme.colors.onSurface.copy(0.6f)
        Box {
            Text("静态刷新模式", Modifier.padding(end=40.dp).align(Alignment.CenterStart), textColor, 14.sp)
            Checkbox(
                lineChartStaticRefresh, {
                lineChartStaticRefresh = !lineChartStaticRefresh
            }, Modifier.align(Alignment.CenterEnd),
                colors = CheckboxDefaults.colors(MaterialTheme.colors.secondary, MaterialTheme.colors.onSurface))
        }
        Text("幅值范围(μV) ±", Modifier.padding(start = 2.dp), textColor, 14.sp)
        var maxValueText by remember { mutableStateOf("$yMaxValue") }
        var maxValueError by remember { mutableStateOf(false) }
        CustomTextField(maxValueText, { inputText ->
            maxValueText = inputText
            maxValueError = try {
                val value = maxValueText.toFloat()
                if (value > 0) {
                    yMaxValue = value
                    false
                } else {
                    true
                }
            } catch (ex: NumberFormatException){
                true
            }
        }, Modifier.size(72.dp, 24.dp).padding(start=4.dp), textStyle=TextStyle(textColor, 14.sp,
            textAlign= TextAlign.Center), placeholder={
            Text("请输入", Modifier.fillMaxSize(), hintColor, 13.sp, textAlign = TextAlign.Center)
        }, isError=maxValueError, keyboardOptions=KeyboardOptions(keyboardType= KeyboardType.Number), maxLines=1,
            contentPadding = PaddingValues(top=4.dp))
        Row(Modifier.weight(1f), Arrangement.Center, Alignment.CenterVertically) {
            Text("Notch", Modifier, textColor, 14.sp)
            var notchText by remember { mutableStateOf(eegDataFilter.notch.string(false)) }
            var notchTextError by remember { mutableStateOf(false) }
            CustomTextField(notchText, { inputText ->
                notchText = inputText
                if(notchText.isEmpty()){
                    eegDataFilter.notch = -1.0
                    notchTextError = false
                    return@CustomTextField
                }
                notchTextError = try {
                    val value = notchText.toDouble()
                    if (value > 0) {
                        eegDataFilter.notch = value
                        false
                    } else {
                        true
                    }
                } catch (ex: NumberFormatException){
                    true
                }
            }, Modifier.size(72.dp, 24.dp).padding(start=4.dp), textStyle= TextStyle(textColor, 14.sp,
                textAlign= TextAlign.Center), placeholder={
                Text("请输入", Modifier.fillMaxSize(), hintColor, 13.sp, textAlign = TextAlign.Center)
            }, isError=notchTextError, keyboardOptions=KeyboardOptions(keyboardType= KeyboardType.Number), maxLines=1,
                contentPadding = PaddingValues(top=4.dp))
            Text("Bandpass", Modifier.padding(start = 16.dp), textColor, 14.sp)
            var startFreqText by remember { mutableStateOf(eegDataFilter.startFreq.string(true)) }
            var startFreqError by remember { mutableStateOf(false) }
            CustomTextField(startFreqText, { inputText ->
                startFreqText = inputText
                if(startFreqText.isEmpty()){
                    eegDataFilter.startFreq = -1.0
                    startFreqError = false
                    return@CustomTextField
                }
                startFreqError = try {
                    val value = startFreqText.toDouble()
                    if (value >= 0) {
                        eegDataFilter.startFreq = value
                        false
                    } else {
                        true
                    }
                } catch (ex: NumberFormatException){
                    true
                }
            }, Modifier.size(88.dp, 24.dp).padding(start=4.dp), textStyle= TextStyle(textColor, 14.sp,
                textAlign= TextAlign.Center), placeholder={
                Text("startFreq", Modifier.fillMaxSize(), hintColor, 14.sp, textAlign = TextAlign.Center)
            }, isError=startFreqError, keyboardOptions=KeyboardOptions(keyboardType= KeyboardType.Number), maxLines=1,
                contentPadding = PaddingValues(top=4.dp))
            var stopFreqText by remember { mutableStateOf(eegDataFilter.stopFreq.string(false)) }
            var stopFreqError by remember { mutableStateOf(false) }
            CustomTextField(stopFreqText, { inputText ->
                stopFreqText = inputText
                if(stopFreqText.isEmpty()){
                    eegDataFilter.stopFreq = -1.0
                    stopFreqError = false
                    return@CustomTextField
                }
                stopFreqError = try {
                    val value = stopFreqText.toDouble()
                    if(value > 0 && value > eegDataFilter.startFreq){
                        eegDataFilter.stopFreq = value
                        false
                    } else {
                        true
                    }
                } catch (ex: NumberFormatException){
                    true
                }
            }, Modifier.size(88.dp, 24.dp).padding(start=4.dp), textStyle=TextStyle(textColor, 14.sp,
                textAlign= TextAlign.Center), placeholder={
                Text("stopFreq", Modifier.fillMaxSize(), hintColor, 14.sp, textAlign = TextAlign.Center)
            }, isError=stopFreqError, keyboardOptions=KeyboardOptions(keyboardType= KeyboardType.Number), maxLines=1,
                contentPadding = PaddingValues(top=4.dp))
            Text("FFT Plot Channel:", Modifier.padding(start = 16.dp), textColor, 14.sp)
            val channelOptions = remember {
                ArrayList<String>().apply{
                    add(FFT_PLOT_CHANNEL_ALL)
                    addAll((1..channels).map { it.toString() })
                }
            }
            DropdownMenuListBox(Modifier.size(60.dp, 24.dp).padding(start=4.dp)
                .background(MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.BackgroundOpacity),
                    RoundedCornerShape(4.dp)), channelOptions, textColor=textColor, fontSize = 14.sp,
                fontWeight = FontWeight.Normal, paddingValues = PaddingValues(9.dp, 2.dp, bottom = 2.dp),
                onItemClick = { _, data ->
                    fftPlotChannelIndex = if(data == FFT_PLOT_CHANNEL_ALL){
                        -1
                    } else {
                        data.toInt() - 1
                    }
                })
        }
    }
    LiveMultiLineChartView(lineChartColor)
}

@Composable
private fun LiveMultiLineChartView(lineChartColor: Color)
= Box(Modifier.padding(start=16.dp,top=52.dp,end=16.dp, bottom=16.dp).fillMaxSize()) {
    Text("LiveMultiLineChartView", Modifier.align(Alignment.Center),
        Color.LightGray, 48.sp, fontWeight=FontWeight.Bold)
    LiveMultiLineChartView(Modifier.fillMaxSize(),
        lineChartDataFlow, channels, lineChartColor, yMaxValue, -yMaxValue, lineChartStaticRefresh)
}

private fun Double.string(allowedZero: Boolean): String{
    return if(this <= 0.0){
        if(allowedZero && this == 0.0){
            "0"
        } else {
            ""
        }
    } else {
        this.toString()
    }
}

@Composable
private fun FftResultLayout(){
    Surface(Modifier.padding(top=16.dp).fillMaxWidth().aspectRatio(1.4f), RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxSize()) {
            var fftPlotSelected by remember { mutableStateOf(true) }
            val unselectTextColor = MaterialTheme.colors.onSurface.copy(0.4f)
            Row(Modifier.fillMaxWidth().fillMaxHeight(0.14f)) {
                var fftPlotTextColor by remember { mutableStateOf(unselectTextColor) }
                var bandPowerTextColor by remember { mutableStateOf(unselectTextColor) }
                HoveredStateBoxButton({
                    releaseFftResultView()
                    fftPlotSelected = true
                }, Modifier.fillMaxSize().weight(1f), RoundedCornerShape(topStart = 16.dp)){ hovered ->
                    fftPlotTextColor = if(hovered){
                        MaterialTheme.colors.onSecondary
                    } else if(fftPlotSelected){
                        MaterialTheme.colors.onSurface
                    } else {
                        MaterialTheme.colors.onSurface.copy(0.4f)
                    }
                    Text("FFT Plot", Modifier.align(Alignment.Center), fftPlotTextColor, 14.sp,
                        fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(1.dp).fillMaxHeight().background(surfaceLineColor))
                HoveredStateBoxButton({
                    releaseFftResultView()
                    fftPlotSelected = false
                }, Modifier.fillMaxSize().weight(1f), RoundedCornerShape(topEnd = 16.dp)){hovered ->
                    bandPowerTextColor = if(hovered){
                        MaterialTheme.colors.onSecondary
                    } else if(!fftPlotSelected){
                        MaterialTheme.colors.onSurface
                    } else {
                        MaterialTheme.colors.onSurface.copy(0.4f)
                    }
                    Text("Band Power", Modifier.align(Alignment.Center), bandPowerTextColor, 14.sp,
                        fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).background(surfaceLineColor))
            if(fftPlotSelected){
                FftPlotLineChartView(Modifier.fillMaxSize(), MaterialTheme.colors.primary,
                    MaterialTheme.colors.onBackground, fftResultXAxisInfoList, if(yMaxValue < 30){ 30f } else { yMaxValue },
                    channelsPointList, fftResultViewAnimated
                )
            } else {
                BandPowerHistogramView(Modifier.fillMaxSize(), MaterialTheme.colors.onBackground, 30f,
                    histogramInfoList, fftResultViewAnimated
                )
            }
        }
    }
}

private var mockPointCount = 0
private var mockTimer: Timer? = null
//模拟250采样率, 50Hz工频数据
private fun startMockLineChartData(){
    mockTimer = Timer()
    mockTimer!!.scheduleAtFixedRate(object: TimerTask(){
        override fun run() {
            eegDataFilter.addData(FloatArray(channels){
                (sin(8 * Math.PI * 50.0 * (mockPointCount / 250.0)) * 20).toFloat()
            }.toList(), 0)
            mockPointCount++
        }
    }, 4, 4)
}

private fun stopMockLineChartData(){
    mockTimer?.cancel()
    mockTimer = null
}

private fun start(){
    startState = true
    startDataFilter()
    updateFftPlotXAxisInfo()
}

private fun stop(){
    startState = false
    stopDataFilter()
    channelsPointList.clear()
    histogramInfoList.forEach { it.value = 0f }
    releaseFftResultView()
}

private fun startDataFilter() {
    eegDataFilter.start(samplingRate, 1, channels){
        lineChartDataFlow.tryEmit(it)
    }
    eegDataFilter.fftResultsCallback = ::callFftResults
}

private fun stopDataFilter() {
    eegDataFilter.stop()
    eegDataFilter.fftResultsCallback = null
}

private fun updateFftPlotXAxisInfo() {
    fftResultXAxisInfoList.clear()
    val fftFreqSize = floor(eegDataFilter.fftMaxFreq / 10.0).toInt()
    (0 .. fftFreqSize).forEach { i ->
        val value = i * 10f
        fftResultXAxisInfoList.add(FFtResultAxisInfo(value, value.toInt().toString()))
    }
    fftResultXAxisInfoList.add(FFtResultAxisInfo(fftFreqSize * 10f + 5, (fftFreqSize * 10f + 5).toInt().toString()))
}

private var startCallResultTm = System.currentTimeMillis()
private fun callFftResults(fftPlotPointsArr: Array<Array<DataHandler.Point>>, bandPowersArr: Array<DoubleArray>){
    println("callFftResults: cost=${System.currentTimeMillis() - startCallResultTm}")
    if(fftResultViewAnimated.value){
        fftResultViewAnimated.value = false
        return
    }
    //FFT Plot
    channelsPointList.clear()//reset
    fftPlotPointsArr.mapIndexed { index, fftPlotPointArr ->
        if(fftPlotChannelIndex == -1 || fftPlotChannelIndex == index){
            channelsPointList.add(fftPlotPointArr.filter { point ->
                point.x <= 75
            }.map {
                PointInfo(it.x.toFloat(), it.y.toFloat())
            })
        }
    }
    //Band Power
    histogramInfoList.forEach{ it.value = 0f }//reset
    if(fftPlotChannelIndex == -1){
        bandPowersArr.forEach { bandPowerArr ->
            bandPowerArr.forEachIndexed { index, bandPower ->
                if(bandPower.isNaN()){
                    histogramInfoList[index].value += 0f
                } else {
                    histogramInfoList[index].value += bandPower.toFloat()
                }
            }
        }
        histogramInfoList.forEach{ it.value /= bandPowersArr.size }
    } else {
        bandPowersArr[fftPlotChannelIndex].forEachIndexed { index, bandPower ->
            histogramInfoList[index].value = bandPower.toFloat()
        }
    }
    //防止出现负数绘制异常
    histogramInfoList.forEach { histogramInfo ->
        if(histogramInfo.value < 0){
            histogramInfo.value = 0f
        }
    }
    startAnimateTm = System.currentTimeMillis()
    fftResultViewAnimated.value = true
    startCallResultTm = System.currentTimeMillis()
}
