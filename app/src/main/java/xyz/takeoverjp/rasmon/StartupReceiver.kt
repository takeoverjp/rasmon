package xyz.takeoverjp.rasmon;

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context?, itt: Intent?) {
        if (ctx != null) {
            val prefs = ctx.getSharedPreferences("Config", AppCompatActivity.MODE_PRIVATE)
            val autoStart = prefs.getBoolean("AUTO_START", false)
            if (autoStart) {
                OverlayService.start(ctx)
            }
        }
    }
}
