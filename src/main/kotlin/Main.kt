
import androidx.compose.ui.window.application
import com.zhzc0x.compose.linechart.LineChartDemoPage


fun main() = application {
    LineChartDemoPage(onClose = ::exitApplication)
}
