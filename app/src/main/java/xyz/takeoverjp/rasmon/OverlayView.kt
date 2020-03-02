package xyz.takeoverjp.rasmon

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
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
    private val mHandler: Handler = Handler()

    // ram,7088992,4611060,1789196
    private val REGEX_MEM_RAM = Regex("""ram,(\d+),(\d+),\d+""")

    // proc,native,android.hardware.audio@2.0-service,601,6529,N/A,e
    private val REGEX_MEM_PROC = Regex("""proc,[^,]+,([^,]+),(\d+),(\d+),[^,]*,[^,]*""")

    data class ProcessMeminfo(val name: String, val pid: Int, val pssKb: Int) {
        override fun toString(): String {
            return "%-30s(%5d) %4dM".format(name.take(30), pid, pssKb/1024)
        }
    }

    fun updateMeminfo() {
        val processBuilder = ProcessBuilder("dumpsys", "meminfo", "-c")
        val process = processBuilder.start()
        val isReader = InputStreamReader(process.inputStream)
        val bufferedReader = BufferedReader(isReader)

        process.waitFor()

        val procList: MutableList<ProcessMeminfo> = mutableListOf()
        var totalKb: Int? = null
        var freeKb: Int? = null
        var totalUsedPercent: Int? = null
        while (true) {
            val line = bufferedReader.readLine()
            if (line == null) {
                break
            }

            if (line.startsWith("ram,")) {
                val result = REGEX_MEM_RAM.find(line)
                if (result == null) {
                    Log.e(TAG, "invalid mem ram line = " + line)
                    continue
                }
                val (total, free) = result.destructured
                totalKb = Integer.parseInt(total)
                freeKb = Integer.parseInt(free)
                totalUsedPercent = (totalKb - freeKb) * 100 / totalKb
            } else if (line.startsWith("proc,")) {
                val result = REGEX_MEM_PROC.find(line)
                if (result == null) {
                    Log.e(TAG, "invalid mem proc line = " + line)
                    continue
                }
                val (name, pid, pssKb) = result.destructured
                val proc = ProcessMeminfo(name, Integer.parseInt(pid), Integer.parseInt(pssKb))
                procList.add(proc)
            }
        }

        procList.sortBy {it.pssKb * -1}

        val totalView = findViewById<TextView>(R.id.memTotalView)
        totalView.text = "$totalUsedPercent% (${((totalKb?:0) - (freeKb?:0))/1024}M)"
        val bar = findViewById<ProgressBar>(R.id.memBar)
        bar.progress = totalUsedPercent?:0
        val rank1 = " 1:${procList[0]}"
        val rank2 = " 2:${procList[1]}"
        val rank3 = " 3:${procList[2]}"
        val rankView = findViewById<TextView>(R.id.memUsageRankView)
        rankView.text = "$rank1\n$rank2\n$rank3"
    }

    //  50% 1301/system_server: 33% user + 16% kernel / faults: 103 minor
    private val REGEX_CPU_PROC = Regex("""\s*(\d+)%\s*(\d+)/([^:]+):.*""")

    // 37% TOTAL: 18% user + 15% kernel + 0.2% iowait + 2.1% irq + 0.6% softirq
    private val REGEX_CPU_TOTAL = Regex("""(\d+)% TOTAL: .*""")

    data class ProcessCpuinfo(val name: String, val pid: Int, val usedPercent: Int) {
        override fun toString(): String {
            return "%-30s(%5d) %4d%%".format(name.take(30), pid, usedPercent)
        }
    }

    fun updateCpuinfo() {
        val processBuilder = ProcessBuilder("dumpsys", "cpuinfo", "-c")
        val process = processBuilder.start()
        val isReader = InputStreamReader(process.inputStream)
        val bufferedReader = BufferedReader(isReader)

        process.waitFor()

        val procList: MutableList<ProcessCpuinfo> = mutableListOf()
        var totalUsedPercent: Int? = null
        while (true) {
            val line = bufferedReader.readLine()
            if (line == null) {
                break
            }

            if (line.startsWith("Load:")) {
            } else if (line.startsWith("CPU usage from")) {
            } else if (line.contains("TOTAL:")) {
                val result = REGEX_CPU_TOTAL.find(line)
                if (result == null) {
                    Log.e(TAG, "invalid cpu total line = " + line)
                    continue
                }
                val (total) = result.destructured
                totalUsedPercent = Integer.parseInt(total)
            } else {
                val result = REGEX_CPU_PROC.find(line)
                if (result == null) {
                    Log.e(TAG, "invalid cpu proc line = " + line)
                    continue
                }
                val (usedPercent, pid, name) = result.destructured
                val proc = ProcessCpuinfo(name, Integer.parseInt(pid), Integer.parseInt(usedPercent))
                procList.add(proc)
            }
        }

        procList.sortBy {it.usedPercent * -1}

        val totalView = findViewById<TextView>(R.id.cpuTotalView)
        totalView.text = "$totalUsedPercent%"
        val bar = findViewById<ProgressBar>(R.id.cpuBar)
        bar.progress = totalUsedPercent?:0
        val rankView = findViewById<TextView>(R.id.cpuUsageRankView)
        val rank1 = " 1:${procList[0]}"
        val rank2 = " 2:${procList[1]}"
        val rank3 = " 3:${procList[2]}"
        rankView.text = "${rank1}\n${rank2}\n${rank3}"
    }


    /** Settings for overlay view */
    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
    )

    private fun updateView() {
        updateMeminfo()
        updateCpuinfo()
    }

    val mUpdateRunnable = object : Runnable {
        override fun run() {
            updateView()
            handler.postDelayed(this, 5000)
        }
    }

    /** Starts displaying this view as overlay. */
    @Synchronized
    fun show() {
        if (!this.isShown) {
            updateView()
            windowManager.addView(this, layoutParams)
            mHandler.postDelayed(mUpdateRunnable, 5000)
        }
    }

    /** Hide this view. */
    @Synchronized
    fun hide() {
        if (this.isShown) {
            mHandler.removeCallbacks(mUpdateRunnable)
            windowManager.removeView(this)
        }
    }
}
