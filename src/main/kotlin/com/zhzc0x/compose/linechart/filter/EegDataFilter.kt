package com.zhzc0x.compose.linechart.filter

import java.util.*
import javax.swing.SwingUtilities

class EegDataFilter {

    private var delayTime = 20L
    private var samplingRate = 0
    private var groups = 0
    private var channels = 0
    private val dataBufferLock = Any()
    private var filterTimer: Timer? = null
    private lateinit var filteredCallback: (FloatArray) -> Unit//回调每个通道的单个采样点
    /**
     * 回调每个通道的fft频域点的坐标和bandPower(delta theta alpha bate gamma 各频带能量)
     * */
    var fftResultsCallback: ((Array<Array<DataHandler.Point>>, Array<DoubleArray>) -> Unit)? = null

    private var startTime = 0L
    private val dataHandler = DataHandler()
    private var dataIndex = 0
    private var originalBuffer: Array<DoubleArray> = emptyArray()
    private var filteredBuffer: Array<LinkedList<Double>> = emptyArray()
    private var fftPlotPointBuffer: Array<Array<DataHandler.Point>> = emptyArray()
    private var bandPowerBuffer: Array<DoubleArray> = emptyArray()
    private var filterArr = DoubleArray(0)
    private var readyArr = DoubleArray(0)
    private lateinit var groupRange: IntRange

    companion object{
        const val ORDER = 4
        const val RIPPLE = 0.0

        private const val FILTER_WINDOW_WIDTH = 512//滑窗窗宽
        private const val FILTER_STEP_SIZE = 75//滑窗步长，300ms
        private const val FILTER_INIT_SIZE = FILTER_WINDOW_WIDTH - FILTER_STEP_SIZE//滑窗初始大小

    }

    var notch = 0.0
    var startFreq = 0.0
    var stopFreq = 100.0
    var fftMaxFreq = 75//fft返回的最高频域值

    fun start(samplingRate: Int, groups: Int, channels: Int, filteredCallback: (FloatArray) -> Unit){
        this.samplingRate = samplingRate
        this.groups = groups
        this.channels = channels
        this.filteredCallback = filteredCallback
        groupRange = 0 until groups
        delayTime = 1000L / samplingRate
        originalBuffer = Array(channels){
            return@Array DoubleArray(FILTER_WINDOW_WIDTH)
        }
        filteredBuffer = Array(channels){
            return@Array LinkedList<Double>()
        }
        fftPlotPointBuffer = Array(channels){
            @Suppress("UNCHECKED_CAST")
            return@Array DataHandler.Point().toArray(FILTER_WINDOW_WIDTH / 2 + 1) as Array<DataHandler.Point>
        }
        bandPowerBuffer = Array(channels){
            return@Array DoubleArray(5)
        }
        filterArr = DoubleArray(FILTER_WINDOW_WIDTH)
        readyArr = DoubleArray(FILTER_STEP_SIZE)
        println("delayTime=$delayTime")
        dataIndex = FILTER_INIT_SIZE
        executeFilteredCallbackTask()
    }

    private fun executeFilteredCallbackTask(){
        filterTimer?.cancel()
        filterTimer = Timer().apply {
            scheduleAtFixedRate(object: TimerTask(){
                override fun run() {
                    synchronized(dataBufferLock){
                        if(filteredBuffer[0].isEmpty()){
                            return
                        }
                        val dataArray = FloatArray(channels){ channel ->
                            filteredBuffer[channel].pop().toFloat()
                        }
                        SwingUtilities.invokeLater {
                            filteredCallback(dataArray)
                        }
                    }
                }
            }, 0, delayTime)
        }
    }

    fun addData(dataList: List<Float>, offset: Int = 1){
        if(filterTimer == null){
            return
        }
        synchronized(dataBufferLock){
            groupRange.forEach{ groupIndex ->
                val startIndex = groupIndex * channels + offset
                val dataArray = FloatArray(channels){ channelIndex ->
                    return@FloatArray dataList[startIndex + channelIndex]
                }
                addToFilter(dataArray)
            }
        }
    }

    private fun addToFilter(channelsData: FloatArray) {
        startTime = System.currentTimeMillis()
        originalBuffer.forEachIndexed { channelIndex, originalArr ->
            originalArr[dataIndex] = channelsData[channelIndex].toDouble()
        }
        dataIndex++
        if(dataIndex >= FILTER_WINDOW_WIDTH){
            originalBuffer.forEachIndexed { channelIndex, originalArr ->
                System.arraycopy(originalArr, 0, filterArr, 0, filterArr.size)
                if(notch > 0){
                    dataHandler.bandstop(filterArr, samplingRate, notch - 1, notch + 1, ORDER,
                        DataHandler.FilterTypes.BUTTERWORTH, RIPPLE
                    )
                }
                if(startFreq >= 0 && stopFreq > startFreq){
                    dataHandler.bandpass(filterArr, samplingRate, startFreq , stopFreq, ORDER,
                        DataHandler.FilterTypes.BUTTERWORTH, RIPPLE
                    )
                }
                System.arraycopy(filterArr, FILTER_INIT_SIZE, readyArr, 0, FILTER_STEP_SIZE)
                filteredBuffer[channelIndex].addAll(readyArr.toList())

                dataHandler.fftWithBandPower(filterArr, DataHandler.WindowOperations.NO_WINDOW,
                    fftPlotPointBuffer[channelIndex], bandPowerBuffer[channelIndex])

                System.arraycopy(originalArr, FILTER_STEP_SIZE, originalArr, 0, FILTER_INIT_SIZE)
            }
            fftResultsCallback?.invoke(fftPlotPointBuffer, bandPowerBuffer)
            println("EegDataFilter --> filtered size:${filteredBuffer[0].size} " +
                    "perform duration:${System.currentTimeMillis() - startTime}")
            dataIndex = FILTER_INIT_SIZE
        }
    }

    fun stop(){
        filterTimer?.cancel()
        filterTimer = null
    }

}