package com.example.bluetooth_le_gatt_sborka.device_scan_appcompatactivity;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class DialogFragment extends AppCompatDialogFragment {
    private final PermissionsProcessing pp;

    public DialogFragment(PermissionsProcessing pp) {
        this.pp = pp;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Не предоставлены все необходимые разрешения!")
                .setMessage("Предоставить необходимые разрешения?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Да", (dialog, id) -> {
                    // Закрываем окно
                    pp.checkPermissions();
                    dialog.cancel();
                })
                .setNegativeButton("Отмена", (dialog, which) -> dialog.cancel());
        return builder.create();
    }
}
