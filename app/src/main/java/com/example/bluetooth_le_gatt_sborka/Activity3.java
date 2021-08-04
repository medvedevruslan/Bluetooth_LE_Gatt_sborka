package com.example.bluetooth_le_gatt_sborka;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bluetooth_le_gatt_sborka.support.AcResult;

public class Activity3 extends AppCompatActivity implements View.OnClickListener {

    public static final String EXTRA_ACRESULT = "acresult";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.result);
        Intent intent = getIntent();
        AcResult mAcResult = (AcResult) intent.getSerializableExtra(EXTRA_ACRESULT);
        initializeView(mAcResult);
        findViewById(R.id.button_back).setOnClickListener(this);
    }

    private void initializeView(AcResult acResult) {
        ((TextView) findViewById(R.id.ac_date)).setText(acResult.getAcDate());
        ((TextView) findViewById(R.id.ac_time)).setText(acResult.getAcTime());
        TextView viewValue = findViewById(R.id.ac_value);
        viewValue.setText(acResult.getAcValue());

        float value = Float.parseFloat(viewValue.getText().toString().substring(0, 5));
        if (value >= 0.16f) {
            viewValue.setTextColor(getResources().getColor(R.color.red));
        } else {
            viewValue.setTextColor(getResources().getColor(R.color.green));
        }

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(acResult.getDeviceName());
        // ((TextView) findViewById(R.id.device_name)).setText(acResult.getDeviceName());
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_back) finish();
    }
}
