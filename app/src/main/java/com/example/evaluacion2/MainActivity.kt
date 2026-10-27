package com.example.evaluacion2

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.evaluacion2.ui.theme.BorderLight
import com.example.evaluacion2.ui.theme.Evaluacion2Theme
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var dispositivoEncontrado = false
    private var esEscaneoActivo = false
    private val handler = Handler(Looper.getMainLooper())

    private var rssiBtSuavizado: Double? = null
    private val factorSuavizado = 0.3

    // Evita el spam en el historial
    private var ultimoDispositivoGuardado: String? = null

    // Estado que actualiza la pantalla automáticamente
    private val bluetoothStatusText = mutableStateOf("Estado: Esperando conexión...")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // CONECTAMOS LA INTERFAZ DE JETPACK COMPOSE
        setContent {
            Evaluacion2Theme {
                MainDashboard(
                    statusText = bluetoothStatusText.value,
                    onRefresh = { actualizarEstado() },
                    onGoInfo = { startActivity(Intent(this, InfoActivity::class.java)) },
                    onGoHistorial = { startActivity(Intent(this, HistorialActivity::class.java)) },
                    onGoSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                )
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(receiver, filter)

        solicitarPermisos()
    }

    @Composable
    fun MainDashboard(
        statusText: String,
        onRefresh: () -> Unit,
        onGoInfo: () -> Unit,
        onGoHistorial: () -> Unit,
        onGoSettings: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "DISPOSITIVO CONECTADO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Evaluación 2",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Tarjeta de estado principal
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = statusText,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Accesos rápidos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Botones estilo tarjetas
            Row(modifier = Modifier.fillMaxWidth()) {
                GridCard("🔄", "Verificar", "Estado en vivo", onRefresh, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                GridCard("📖", "Guía", "Cómo conectar", onGoInfo, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                GridCard("📋", "Historial", "Eventos", onGoHistorial, Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                GridCard("⚙️", "Ajustes", "Configuración", onGoSettings, Modifier.weight(1f))
            }
        }
    }

    @Composable
    fun GridCard(
        icon: String, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderLight),
            modifier = modifier.clickable { onClick() }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = icon, fontSize = 22.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
        }
    }

    private fun actualizarEstado() {
        esEscaneoActivo = false
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothAdapter?.cancelDiscovery()
        }
        ultimoDispositivoGuardado = null
        iniciarBusquedaBluetooth()
    }

    private fun iniciarBusquedaBluetooth() {
        esEscaneoActivo = true
        dispositivoEncontrado = false
        bluetoothStatusText.value = "Calibrando señal..."

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothStatusText.value = "Faltan permisos."
            return
        }

        if (bluetoothAdapter?.isEnabled == true) {
            if (bluetoothAdapter?.isDiscovering == true) bluetoothAdapter?.cancelDiscovery()
            bluetoothAdapter?.startDiscovery()
        } else {
            bluetoothStatusText.value = "Bluetooth Apagado."
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                val rssi: Short = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                if (device != null && rssi != Short.MIN_VALUE && device.bondState == BluetoothDevice.BOND_BONDED) {
                    dispositivoEncontrado = true
                    val rssiActual = rssi.toDouble()
                    rssiBtSuavizado = if (rssiBtSuavizado == null) rssiActual else (factorSuavizado * rssiActual) + ((1.0 - factorSuavizado) * rssiBtSuavizado!!)

                    val txPower = -60.0
                    val n = 2.5
                    var distance = 10.0.pow((txPower - rssiBtSuavizado!!) / (10.0 * n))

                    if (distance < 0.05) distance = 0.05

                    val nombreDisp = if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        device.name ?: "Dispositivo Vinculado"
                    } else "Dispositivo Vinculado"

                    val textoDistancia = if (distance < 1.0) "${(distance * 100).toInt()} cm" else String.format("%.2f metros", distance)

                    bluetoothStatusText.value = "📡 Detectando: $nombreDisp\n📏 Distancia aprox: $textoDistancia"

                    // REGISTRO EN LA NUBE (FIREBASE FIRESTORE)
                    if (nombreDisp != ultimoDispositivoGuardado) {
                        ultimoDispositivoGuardado = nombreDisp

                        GestorHistorial.guardarEvento(
                            nombreDispositivo = nombreDisp,
                            descripcionEvento = "Dispositivo vinculado detectado a $textoDistancia"
                        )
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                if (!dispositivoEncontrado && esEscaneoActivo) bluetoothStatusText.value = "Buscando dispositivo..."
                if (esEscaneoActivo) {
                    handler.postDelayed({
                        if (esEscaneoActivo && bluetoothAdapter?.isEnabled == true) {
                            dispositivoEncontrado = false
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                bluetoothAdapter?.startDiscovery()
                            }
                        }
                    }, 500)
                }
            }
        }
    }

    private fun solicitarPermisos() {
        val permisos = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permisos.add(Manifest.permission.BLUETOOTH_SCAN)
            permisos.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        ActivityCompat.requestPermissions(this, permisos.toTypedArray(), 1)
    }

    override fun onResume() {
        super.onResume()
        actualizarEstado()
    }

    override fun onPause() {
        super.onPause()
        esEscaneoActivo = false
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        esEscaneoActivo = false
        handler.removeCallbacksAndMessages(null)
        unregisterReceiver(receiver)
    }
}