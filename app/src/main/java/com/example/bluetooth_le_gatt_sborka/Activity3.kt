package com.example.bluetooth_le_gatt_sborka

import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bluetooth_le_gatt_sborka.support.AcResult

class Activity3 : AppCompatActivity(), View.OnClickListener {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.result)
        val mAcResult = intent.getSerializableExtra(EXTRA_ACRESULT) as AcResult
        initializeView(mAcResult)
        findViewById<Button>(R.id.button_back).setOnClickListener(this)
    }

    private fun initializeView(acResult: AcResult) {
        (findViewById<View>(R.id.ac_date) as TextView).text = acResult.acDate
        (findViewById<View>(R.id.ac_time) as TextView).text = acResult.acTime
        val viewValue = findViewById<TextView>(R.id.ac_value)
        viewValue.text = acResult.acValue
        val value = acResult.acValue.substring(0, 5).toFloat()
        if (value >= 0.16f) {
            viewValue.setTextColor(ContextCompat.getColor(this, R.color.red))
        } else {
            viewValue.setTextColor(ContextCompat.getColor(this, R.color.green))
        }
        if (supportActionBar != null) supportActionBar!!.title = acResult.deviceName
        // ((TextView) findViewById(R.id.device_name)).setText(acResult.getDeviceName());
    }

    override fun onClick(v: View) {
        if (v.id == R.id.button_back) finish()
    }

    companion object {
        const val EXTRA_ACRESULT = "acresult"
    }
}