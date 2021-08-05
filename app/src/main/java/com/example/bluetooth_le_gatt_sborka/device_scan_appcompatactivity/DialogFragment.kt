package com.example.bluetooth_le_gatt_sborka.device_scan_appcompatactivity

import androidx.appcompat.app.AppCompatDialogFragment
import android.os.Bundle
import android.app.Dialog
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

class DialogFragment(private val pp: PermissionsProcessing) : AppCompatDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Не предоставлены все необходимые разрешения!")
            .setMessage("Предоставить необходимые разрешения?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Да") { dialog: DialogInterface, _: Int ->
                // Закрываем окно
                pp.checkPermissions()
                dialog.cancel()
            }
            .setNegativeButton("Отмена") { dialog: DialogInterface, _: Int -> dialog.cancel() }
        return builder.create()
    }
}