package com.example.bluetooth_le_gatt_sborka.device_scan_appcompatactivity;

import static com.example.bluetooth_le_gatt_sborka.device_scan_appcompatactivity.DeviceScanAppCompatActivity.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetooth_le_gatt_sborka.R;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
    private final Scanning scanning;

    private final ArrayList<BluetoothDevice> list = new ArrayList<>();

    public RecyclerViewAdapter(Scanning scanning) {
        this.scanning = scanning;
    }

    public boolean isNotEmpty() {
        return !list.isEmpty();
    }

    public void addItem(BluetoothDevice device) {
        if (!list.contains(device)) {
            list.add(device);
            notifyItemInserted(list.size()-1);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clearList() {
        list.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(list.get(position), position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView deviceName,
                deviceAddress;
        private final LinearLayout rootView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceAddress = itemView.findViewById(R.id.device_address);
            rootView = itemView.findViewById(R.id.rootView);
        }

        public void bind(BluetoothDevice item, int position) {

            String deviceNameS = item.getName();
            if (deviceNameS != null && deviceNameS.length() > 0) deviceName.setText(item.getName());
            else deviceName.setText(R.string.unknown_device);


            deviceAddress.setText(item.getAddress());
            rootView.setOnClickListener(v -> {
                Log.d(TAG, "onListItemClick getDeviceByPosition " + position);
                scanning.connectWithDevice(item);
            });
        }
    }
}
