package xyz.takeoverjp.rasmon

import android.content.Context
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader

class OverlayView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(ctx, attrs, defStyle) {
    companion object {
        /** Creates an instance of [OverlayView]. */
        fun create(context: Context) =
            View.inflate(context, R.layout.overlay_view, null) as OverlayView
    }

    private val TAG = """OverlayView"""
    private val windowManager: WindowManager =
        ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** Settings for overlay view */
    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    private fun updateMemRank() {
        val processBuilder = ProcessBuilder("dumpsys", "meminfo")
        val process = processBuilder.start()
        val isReader = InputStreamReader(process.inputStream)
        val bufferedReader = BufferedReader(isReader)

        val view = findViewById<TextView>(R.id.memUsageRankView)
        var line: String?
        while (true) {
            line = bufferedReader.readLine()
            if (line == null) {
                break
            }
            if (line.contains("TOTAL")) {
                break
            }
            Log.d(TAG, line)
        }
        Log.d(TAG, "exitValue=" + process.exitValue())
        view.text = line
    }

    private fun updateCpuRank() {
        val processBuilder = ProcessBuilder("dumpsys", "cpuinfo")
        val process = processBuilder.start()
        val isReader = InputStreamReader(process.inputStream)
        val bufferedReader = BufferedReader(isReader)

        val view = findViewById<TextView>(R.id.cpuUsageRankView)

        var totalLine = ""
        while (true) {
            val line = bufferedReader.readLine()
            if (line == null) {
                break
            }
            if (line.contains("TOTAL")) {
                totalLine = line
                break
            }
            Log.d(TAG, line)
        }
        Log.d(TAG, "exitValue=" + process.exitValue())
        view.text = totalLine
    }

    /** Starts displaying this view as overlay. */
    @Synchronized
    fun show() {
        if (!this.isShown) {
            updateMemRank()
            updateCpuRank()
            windowManager.addView(this, layoutParams)
        }
    }

    /** Hide this view. */
    @Synchronized
    fun hide() {
        if (this.isShown) {
            windowManager.removeView(this)
        }
    }
}
