package xyz.takeoverjp.rasmon

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    companion object {
        /** ID for the runtime permission dialog */
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1
    }

    private val spinnerItems =
        arrayOf("BottomRight", "BottomLeft", "TopRight", "TopLeft")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        requestOverlayPermission()

        // Show/hide overlay view with a toggle button.
        findViewById<ToggleButton>(R.id.toggle_button).apply {
            isChecked = OverlayService.isActive
            setOnCheckedChangeListener { _, isChecked ->
                val prefs = getSharedPreferences("Config", MODE_PRIVATE)
                val e = prefs.edit()
                if (isChecked) {
                    e.putBoolean("AUTO_START", true)
                    e.apply()
                    OverlayService.start(this@MainActivity)
                } else {
                    e.putBoolean("AUTO_START", false)
                    e.apply()
                    OverlayService.stop(this@MainActivity)
                }
            }
        }

        val spinner = findViewById<Spinner>(R.id.positionSpinner)
        val adapter = ArrayAdapter(this,
                    android.R.layout.simple_spinner_item, spinnerItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.setAdapter(adapter)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?, position: Int, id: Long
            ) {
                Log.d("rasmon", "item=" + spinner.selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    /** Requests an overlay permission to the user if needed. */
    private fun requestOverlayPermission() {
        if (isOverlayGranted()) return
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }

    /** Terminates the app if the user does not accept an overlay. */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!isOverlayGranted()) {
                finish()  // Cannot continue if not granted
            }
        }
    }

    /** Checks if the overlay is permitted. */
    private fun isOverlayGranted() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
}
