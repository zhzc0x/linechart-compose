package com.zhzc0x.compose.linechart.filter

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Structure

class DataHandler {

    enum class FilterTypes(val code: Int) {
        BUTTERWORTH(0),
        CHEBYSHEV_TYPE_1(1),
        BESSEL(2);
    }

    enum class WindowOperations(val code: Int) {
        NO_WINDOW(0),
        HANNING(1),
        HAMMING(2),
        BLACKMAN_HARRIS(3);
    }

    private val instance: OpenBciLibrary

    init {
        instance = Native.load("libdata_filter", OpenBciLibrary::class.java)
    }

    fun bandpass(data: DoubleArray, samplingRate: Int, startFreq: Double, stopFreq: Double, order: Int,
                 filterType: FilterTypes, ripple: Double){
        instance.perform_bandpass(data, data.size, samplingRate, startFreq, stopFreq, order, filterType.code, ripple)
    }

    fun bandstop(data: DoubleArray, samplingRate: Int, startFreq: Double, stopFreq: Double, order: Int,
                 filterType: FilterTypes, ripple: Double){
        instance.perform_bandstop(data, data.size, samplingRate, startFreq, stopFreq, order, filterType.code, ripple)
    }

    /**
     * @param data：eeg数据
     * @param window：WindowOperations
     * @param fftPlotPointArr：fft频域点的坐标数组 长度为data.size / 2 + 1，
     * @param bandPowerArr: bandPower(delta theta alpha bate gamma 各频带能量)
     *
     * */
    fun fftWithBandPower(data: DoubleArray, window: WindowOperations, fftPlotPointArr: Array<Point>, bandPowerArr: DoubleArray){
        instance.fft_plot_bandPower(data, data.size, window.code, fftPlotPointArr, bandPowerArr)
    }

    @Structure.FieldOrder("x", "y")
    open class Point : Structure() {
        class ByReference : Point(), Structure.ByReference // Need the stucture address as it a parameter of a particular wrapped method
        class ByValue : Point(), Structure.ByValue

        @JvmField var x = 0.0
        @JvmField var y = 0.0
    }

    interface OpenBciLibrary: Library{

        fun perform_bandstop(data: DoubleArray, len: Int, samplingRate: Int, startFreq: Double, stopFreq: Double, order: Int,
                             filterType: Int, ripple: Double): Int

        fun perform_bandpass(data: DoubleArray, len: Int, samplingRate: Int, startFreq: Double, stopFreq: Double, order: Int,
                             filterType: Int, ripple: Double): Int

        fun perform_fft(data: DoubleArray, len: Int, window: Int, outR: DoubleArray, outI: DoubleArray): Int

        fun fft_plot_bandPower(data: DoubleArray, len: Int, window: Int, fftPlotPointArr: Array<Point>, bandPowerArr: DoubleArray)

    }

}