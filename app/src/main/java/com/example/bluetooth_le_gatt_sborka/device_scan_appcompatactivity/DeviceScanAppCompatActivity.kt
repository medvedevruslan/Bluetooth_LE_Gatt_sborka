package com.example.bluetooth_le_gatt_sborka.device_scan_appcompatactivity

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluetooth_le_gatt_sborka.*
import com.example.bluetooth_le_gatt_sborka.support.Ac015
import com.example.bluetooth_le_gatt_sborka.support.AcResult
import com.example.bluetooth_le_gatt_sborka.support.MyDate
import java.util.*

/**
 * Для сканирования и отображения доступных устройств Bluetooth LE.
 */
class DeviceScanAppCompatActivity : AppCompatActivity(), PermissionsProcessing {
    /**
     * Обработка запроса на включение Bluetooth-модуля устройства
     */
    private val mStartForResult =
        registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_CANCELED) Toast.makeText(
                this,
                "Приложение не сможет подключаться к устройствам с отключенным Bluetooth-модулем!",
                Toast.LENGTH_SHORT
            ).show()
        }

    /**
     * Контракт, выполняющийся когда закрываются Activity выше в стеке
     */
    private val deviceControlActivity =
        registerForActivityResult(StartActivityForResult()) {
            openedNextActivity = false
        }

    /**
     * Объект для использования API сканирования Bluetooth LE устройств
     */
    private var bluetoothLeScanner: BluetoothLeScanner? = null

    /**
     * Настройки сканирования Bluetooth LE устройств
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var bleScanSettings: ScanSettings =
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    private lateinit var viewProgress: TextView
    private lateinit var viewStatus: TextView
    private var mAc015 = Ac015()
    private var mDialog: AlertDialog? = null
    private var mResult = AcResult()
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) { //обработчик сообщений принятых от BTService
            when (msg.what) {
                BluetoothService.MESSAGE_STATE_CHANGE -> {
                    when (val state = msg.arg1) {
                        BluetoothService.STATE_NONE -> {
                            Log.d(TAG, "MESSAGE_STATE_CHANGE/none")
                            setViewStatus(R.string.bth_sts_none, null)
                        }
                        BluetoothService.STATE_LISTEN -> {
                            Log.d(TAG, "MESSAGE_STATE_CHANGE/listen")
                            setViewStatus(R.string.bth_sts_listen, null)
                        }
                        else -> when (state) {
                            BluetoothService.STATE_CONNECTING -> {
                                Log.d(TAG, "MESSAGE_STATE_CHANGE/connecting")
                                setViewStatus(R.string.bth_sts_connecting, mAc015.device.name)
                            }
                            BluetoothService.STATE_CONNECTED -> {
                                Log.d(TAG, "MESSAGE_STATE_CHANGE/connected")
                                setViewStatus(R.string.bth_sts_connected, mAc015.device.name)
                                mAc015.initializeBuffer()
                            }
                        }
                    }
                }
                BluetoothService.MESSAGE_READ -> {
                    val data = String((msg.obj as ByteArray), 0, msg.arg1)
                    Log.d(
                        TAG,
                        "MESSAGE_READ[" + data.replace("\r".toRegex(), "x0D")
                            .replace("\n".toRegex(), "x0A") + "]"
                    )
                    dataReceived(data)
                }
                BluetoothService.MESSAGE_DEVICE_NAME -> Log.d(
                    TAG,
                    "MESSAGE_DEVICE_NAME[" + msg.data.getString(BluetoothService.DEVICE_NAME) + "]"
                )
                BluetoothService.MESSAGE_CONNECTION_FAILED -> {
                    Log.d(TAG, "MESSAGE_CONNECTION_FAILED sts=" + bluetoothService.state)
                    connectionFailed()
                }
                BluetoothService.MESSAGE_CONNECTION_LOST -> {
                    Log.d(TAG, "MESSAGE_CONNECTION_LOST sts=" + bluetoothService.state)
                    Toast.makeText(
                        this@DeviceScanAppCompatActivity,
                        R.string.msg_connection_lost,
                        Toast.LENGTH_SHORT
                    ).show()
                    clear()
                }
            }
        }
    }
    private lateinit var bluetoothService: BluetoothService
    private var device = "error"

    /**
     * Флаг разрешённости всех опасных разрешений приложения пользователем.
     */
    private var allPermissionsGranted = false
    private var checkScanning = false
    private var openedNextActivity = false

    private lateinit var bluetoothAdapter: BluetoothAdapter

    /**
     * Обратный вызов сканирования при API 20 и ниже
     */
    private val lowEnergyScanCallback = LeScanCallback { bluetoothDevice, _, _ ->

        /**
         * @param bluetoothDevice название устройства
         */
        Log.d(TAG, " onLeScan LeScanCallback lowEnergyScanCallback")
        runOnUiThread {
            if (bluetoothDevice.address == device && !openedNextActivity) connectWithDevice(
                bluetoothDevice
            )
            Log.d(TAG, "lowEnergyScanCallback")
        }
    }

    /**
     * Обратный вызов сканирования при API выше 21
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, scanResult: ScanResult) {
            super.onScanResult(callbackType, scanResult)
            if (scanResult.device.name != null && scanResult.device.address != null) {
                runOnUiThread {
                    val bluetoothDevice = scanResult.device

                    // заметки себе: записать все адреса bluetooth устройств в класс SampleGattAttributes
                    // заметки себе: подключаться может по названию устройства, а не по мак адресам?
                    // заметки себе: сделать Тоаст с уведомлением о автоматическом подсоединении с конкретным устройством

                    // автоматическое соединение при сопряжении с Манометром u пирометром
                    if (bluetoothDevice.address == device && !openedNextActivity) connectWithDevice(
                        bluetoothDevice
                    )
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            Log.d(TAG, "onBatchScanResults, results size:" + results.size)
            for (sr in results) {
                Log.d(TAG, "onBatchScanResults results:  $sr")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "onScanFailed, code is : $errorCode")
        }
    }

    /**
     * Функция обрабатывает @param data и отправляет результат теста в другие активити для сохранения в базе данных.
     */
    private fun dataReceived(data: String) {
        val xtime = Date()
        val data2 = mAc015.receive(data)
        if (data2.isNotEmpty()) {
            val msgId = mAc015.analyze(data2)
            setViewProgress(xtime, msgId)
            if (mAc015.isResultReceived) {
                bluetoothService.stop()
                mResult.acTime(xtime)
                mResult.acValue = data2
                val intent = Intent(application, Activity3::class.java)
                intent.putExtra(Activity3.EXTRA_ACRESULT, mResult)
                startActivity(intent)
            } else if (mAc015.isErrorReceived) {
                bluetoothService.stop()
                val intent4 = Intent()
                intent4.putExtra("errm", getString(msgId))
                setResult(RESULT_CANCELED, intent4)
                finish()
            }
        }
    }

    private fun setViewProgress(date: Date, resId: Int) {
        viewProgress.text = MyDate.toTimeString(date)
        viewProgress.append(">")
        viewProgress.append(getString(resId))
    }

    private fun connectionFailed() {
        val adb = AlertDialog.Builder(this)
        adb.setTitle(mAc015.device.name)
        adb.setMessage(R.string.msg_confirm_power)
        adb.setCancelable(false)
        adb.setPositiveButton(R.string.connect) { dialog: DialogInterface, _: Int ->
            bluetoothService.connect(mAc015.device)
            dialog.dismiss()
            mDialog = null
        }
        adb.setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
            mDialog = null
            bluetoothService.stop()
            clear()
        }
        mDialog = adb.show()
    }

    private fun connectPairedDevice() {
        val device = bluetoothService.getPairedDeviceByName("E-200B")
        if (device == null) {
            showToast(R.string.msg_notfound_paired_device)
            return
        }
        mResult.deviceName = device.name
        mAc015.setConnectStart(device)
        bluetoothService.connect(device)
    }

    private fun showToast(resId: Int) {
        if (resId > 0) {
            Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setViewStatus(resId: Int, device: String?) {
        viewStatus.setText(resId)
        if (device != null) {
            viewStatus.append(":")
            viewStatus.append(device)
            mResult.deviceName = device
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Scan Bluetooth Low Energy Devices"
        setContentView(R.layout.device_scan_appcompatactivity)
        checkBluetoothModule()

        //Инициализирует адаптер Bluetooth.
        // Для уровня API 18 и выше получите ссылку на BluetoothAdapter через BluetoothManager
        bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bluetoothService = BluetoothService(handler, bluetoothAdapter)

        findViewById<View>(R.id.next_alco_test).setOnClickListener {
            if (allPermissionsGranted) {
                if (bluetoothService.state == 0) {
                    bluetoothService.start()
                    connectPairedDevice()
                }
            } else DialogFragment(this).show(
                supportFragmentManager, "AlertDialog"
            )
        }
        findViewById<View>(R.id.next_term_test).setOnClickListener {
            device = SampleGattAttributes.MICROLIFE_THERMOMETER_ADDRESS
            Log.d(TAG, "Нажата кнопка начала измерения температуры")
            if (allPermissionsGranted) {
                scanLowEnergyDevice(true)
            } else DialogFragment(this).show(
                supportFragmentManager, "AlertDialog"
            )
        }
        findViewById<View>(R.id.next_pres_test).setOnClickListener {
            device = SampleGattAttributes.MANOMETER_ADDRESS
            Log.d(TAG, "Нажата кнопка начала измерения давления")
            if (allPermissionsGranted) {
                scanLowEnergyDevice(true)
            } else DialogFragment(this).show(
                supportFragmentManager, "AlertDialog"
            )
        }
        viewStatus = findViewById(R.id.bth_status)
        viewProgress = findViewById(R.id.bth_progress)
        checkPermissions()
    }

    /**
     * Функция проверки наличия Bluetooth-модуля и его возможности использования технологии
     * поключения Bluetooth LE
     *
     *
     * Завершает приложение при отсутствии необходимого на устройстве
     *
     */
    private fun checkBluetoothModule() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(
                this,
                "На данном устройстве отсутствует Bluetooth-модуль! Приложение не будет функционировать!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(
                this,
                "Данное устройство не поддерживает возможность подключения Bluetooth LE! Приложение не будет функционировать!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun clear() {
        viewStatus.text = ""
        viewProgress.text = ""
    }

    override fun checkPermissions() {
        val permissionsList = ArrayList<String>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION) else {
            permissionsList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionsList.addAll(
                    listOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        }
        // Очищаем список опасных разрешений от тех, разрешение для которых уже дано
        var i = 0
        while (i < permissionsList.size) {
            val permission = permissionsList[i]
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                permissionsList.removeAt(i)
                i--
            }
            i++
        }
        if (permissionsList.size > 0) {
            ActivityCompat.requestPermissions(this, permissionsList.toTypedArray(), 13)
        } else allPermissionsGranted = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 13) {
            if (grantResults.isNotEmpty()) {
                allPermissionsGranted = true
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false
                        break
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Убедитесь, что на устройстве включен Bluetooth. Если Bluetooth в настоящее время не включен,
        // активируйте намерение отобразить диалоговое окно с просьбой предоставить пользователю разрешение
        // на его включение
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            mStartForResult.launch(enableBtIntent)
        }
    }

    override fun onPause() {
        super.onPause()
        if (checkScanning) scanLowEnergyDevice(false)
        bluetoothService.stop()
        if (mDialog != null) {
            mDialog!!.dismiss()
            mDialog = null
        }
    }

    fun connectWithDevice(device: BluetoothDevice) {
        // deviceFound = true;
        setViewStatus(R.string.bth_sts_connected, device.name)
        val intent = Intent(this, DeviceControlActivity::class.java)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.name)
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.address)
        if (checkScanning) {
            //останавливается поиск
            startOrStopScanBle(true)
            checkScanning = false
        }
        deviceControlActivity.launch(intent)
        openedNextActivity = true
    }

    /**
     * ???Функция сканирования Bluetooth LE устройств
     *
     * @param check true - запускает сканирование, false - останавливает
     */
    private fun scanLowEnergyDevice(check: Boolean) {
        var name = ""
        if (device == SampleGattAttributes.MANOMETER_ADDRESS) name += "UA-911BT-C" else if (device == SampleGattAttributes.MICROLIFE_THERMOMETER_ADDRESS) name += "NC150 BT"
        setViewStatus(R.string.bth_sts_connecting, name)
        // если true то сканирование идёт, а после запускается handler.postDelayed с остановкой сканирования
        if (check) {
            // Останавливает сканирование по истечении заданного периода сканирования.
            // PostDelayed - задержка по времени
            handler.postDelayed({
                checkScanning = false
                startOrStopScanBle(true)
                Log.d(TAG, " scanLowEnergyDevice.stopLEScan: " + true)
            }, SCAN_PERIOD)
            checkScanning = true
            startOrStopScanBle(false)
            Log.d(TAG, " scanLowEnergyDevice.startLeScan " + true)
        } else {
            checkScanning = false
            startOrStopScanBle(true)
            Log.d(TAG, " scanLowEnergyDevice.stopLeScan" + false + "№2")
        }
    }

    private fun startOrStopScanBle(stopTask: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "VERSION.SDK_INT: " + Build.VERSION.SDK_INT + " check#1")
            if (bluetoothLeScanner == null) {
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            }
            if (stopTask) {
                Log.d(TAG, "stop to scan bluetooth devices.")
                checkScanning = false
                try {
                    bluetoothLeScanner!!.stopScan(scanCallback)
                } catch (e2: Exception) {
                    Log.e(TAG, "stopScan error." + e2.message)
                }
            } else {
                Log.d(TAG, "begin to scan bluetooth devices...")
                checkScanning = true
                try {
                    // bluetoothLeScanner!!.startScan(scanCallback)
                    bluetoothLeScanner!!.startScan(null, bleScanSettings, scanCallback)
                } catch (e: Exception) {
                    Log.e(TAG, "startScan error." + e.message)
                }
            }
        } else {
            Log.d(TAG, "VERSION.SDK_INT: " + Build.VERSION.SDK_INT + " check#2")
            if (stopTask) {
                bluetoothAdapter.stopLeScan(lowEnergyScanCallback)
            } else {
                bluetoothAdapter.startLeScan(lowEnergyScanCallback)
            }
        }
    }

    companion object {
        val TAG: String = DeviceScanAppCompatActivity::class.java.simpleName

        private const val SCAN_PERIOD: Long = 10000
    }
}