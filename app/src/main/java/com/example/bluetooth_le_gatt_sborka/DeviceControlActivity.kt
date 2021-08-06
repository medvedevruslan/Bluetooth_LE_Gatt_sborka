package com.example.bluetooth_le_gatt_sborka

import android.content.*
import android.os.*
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetooth_le_gatt_sborka.BluetoothLEService.LocalBinder
import com.example.bluetooth_le_gatt_sborka.support.MyDate.toDateString
import com.example.bluetooth_le_gatt_sborka.support.MyDate.toTimeString
import java.util.*

/**
 * Для данного устройства BLE это действие предоставляет пользовательский интерфейс для подключения,
 * отображения данных и отображения служб и характеристик GATT,
 * поддерживаемых устройством. Действие взаимодействует с `BluetoothLeService`,
 * который, в свою очередь, взаимодействует с Bluetooth LE API.
 */
class DeviceControlActivity : AppCompatActivity() {
    private lateinit var measurementsField: TextView

    /**
     * Обрабатывает различные события, инициированные Сервисом.
     *
     * ACTION_GATT_CONNECTED: подключен к серверу GATT.
     *
     * ACTION_GATT_DISCONNECTED: отключен от сервера GATT.
     *
     * ACTION_GATT_SERVICES_DISCOVERED: обнаружены службы GATT.
     *
     * ACTION_DATA_AVAILABLE: получены данные от устройства.
     *
     * Это может быть результатом операций чтения или уведомления.
     */
    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLEService.ACTION_GATT_CONNECTED -> {
                    updateConnectionState(R.string.connected)
                    invalidateOptionsMenu()
                }
                BluetoothLEService.ACTION_GATT_DISCONNECTED -> {
                    updateConnectionState(R.string.disconnected)
                    invalidateOptionsMenu()
                }
                BluetoothLEService.ACTION_DATA_AVAILABLE ->
                    intent.getStringExtra(BluetoothLEService.MEASUREMENTS_DATA)
                        ?.let { displayMeasurements(it) }
            }
        }
    }
    private lateinit var deviceAddress: String
    private var bluetoothLEService: BluetoothLEService? = null

    /**
     * Код для управления жизненным циклом службы
     */
    @ExperimentalUnsignedTypes
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            bluetoothLEService = (service as LocalBinder).service
            if (!bluetoothLEService!!.initialize()) {
                Log.d(TAG, "Невозможно инициализировать BluetoothManager")
                finish()
            }
            //Автоматически подключается к устройству при успешной инициализации запуска к блютуз параметрам устройства.
            bluetoothLEService!!.connect(deviceAddress)
        }

        override fun onServiceDisconnected(name: ComponentName) {
        }
    }

    @ExperimentalUnsignedTypes
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result)
        findViewById<View>(R.id.button_back).setOnClickListener { finish() }
        val intent = intent
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)!!
        measurementsField = findViewById(R.id.ac_value)
        supportActionBar?.title = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        val gattServiceIntent = Intent(this, BluetoothLEService::class.java)

        // BIND_AUTO_CREATE, означающий, что, если сервис,
        // к которому мы пытаемся подключиться, не работает, то он будет запущен
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    @ExperimentalUnsignedTypes
    override fun onResume() {
        super.onResume()
        // registerReceiver - регистрация приемника BroadcastReceiver
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        Log.d(TAG, "Connect request result = " + bluetoothLEService?.connect(deviceAddress))
    }

    override fun onPause() {
        super.onPause()
        //отключение приёмника BroadcastReceiver
        unregisterReceiver(gattUpdateReceiver)
    }

    @ExperimentalUnsignedTypes
    override fun onDestroy() {
        super.onDestroy()
        // отсоединение от сервиса
        unbindService(serviceConnection)
    }

    fun displayMeasurements(measurements: String) {
        val displayHandler: Handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                if (msg.obj != null) {
                    val time = Date()
                    findViewById<TextView>(R.id.ac_date).text = toDateString(time)
                    findViewById<TextView>(R.id.ac_time).text = toTimeString(time)
                    measurementsField.text = msg.obj.toString()
                }
            }
        }
        val message = Message()
        message.obj = measurements
        displayHandler.sendMessage(message)
    }

    private fun updateConnectionState(resourceId: Int) {
        Log.i(TAG, getString(resourceId))
    }

    /**
     * Функция создания фильтра канала широковещательных сообщений
     *
     * @return фильтр канала широковещательных сообщений
     */
    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE)
        return intentFilter
    }

    companion object {
        const val EXTRAS_DEVICE_NAME = "DEVICE_NAME"
        const val EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"
        val TAG: String = DeviceControlActivity::class.java.simpleName
    }
}