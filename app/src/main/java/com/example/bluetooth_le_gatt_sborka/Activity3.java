package com.example.bluetooth_le_gatt_sborka;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetooth_le_gatt_sborka.support.AcResult;

public class Activity3 extends AppCompatActivity implements View.OnClickListener {
    public static final String EXTRA_ACRESULT = "acresult";

    public static final String ACTION_FINISH_UPDATING = "ACTION_ASYNC_FINISH_UPDATING";
    private static final String TAG = "A3";
    private Button buttonBack;
    private AcResult mAcResult;
    private TextView
            viewTime,
            viewValue,
            viewDate,
            viewDeviceType,
            viewDevice,
            viewDriver;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.result);
        Intent intent = getIntent();
        mAcResult = (AcResult) intent.getSerializableExtra(EXTRA_ACRESULT);
        initializeView(mAcResult);
        buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(this);
    }

    private void initializeView(AcResult acResult) {
        viewDate = findViewById(R.id.ac_date);
        viewDate.setText(acResult.getAcDate());
        viewTime = findViewById(R.id.ac_time);
        viewTime.setText(acResult.getAcTime());
        viewValue = findViewById(R.id.ac_value);
        viewValue.setText(acResult.getAcValue());

        float value = Float.parseFloat(viewValue.getText().toString().substring(0, 5));
        if (value >= 0.16f) {
            viewValue.setTextColor(getResources().getColor(R.color.red));
        } else {
            viewValue.setTextColor(getResources().getColor(R.color.green));
        }

        viewDriver = findViewById(R.id.driver);
        // if(isEnabled){
        //     viewDriver.setText(acResult.getUserText() + "(Водитель был удален!)");
        //     viewDriver.setBackgroundColor(res.getColor(R.color.red));
        // }
        // else
        viewDriver.setText("Z");//acResult.getUserText());

        viewDeviceType = findViewById(R.id.deviceType);
        switch (acResult.getTestType()) {
            case "alco_test":
                viewDeviceType.setText(R.string.caption_device);
                break;
            case "pres_test":
                viewDeviceType.setText(R.string.caption_device2);
                break;
            case "temp_test":
                viewDeviceType.setText(R.string.caption_device3);
                break;
            default:
                break;
        }
        viewDevice = findViewById(R.id.device_name);
        viewDevice.setText(acResult.getDeviceName());
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_back) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
