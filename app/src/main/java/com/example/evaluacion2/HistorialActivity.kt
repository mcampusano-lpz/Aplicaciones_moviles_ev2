package com.example.evaluacion2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evaluacion2.ui.theme.Evaluacion2Theme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistorialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Evaluacion2Theme {
                HistorialScreen(onBackClick = { finish() })
            }
        }
    }
}

@Composable
fun HistorialScreen(onBackClick: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }

    // Estados para manejar la información de la nube
    var listaHistorial by remember { mutableStateOf<List<RegistroHistorial>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Este bloque se ejecuta al abrir la pantalla y escucha la nube en tiempo real
    LaunchedEffect(Unit) {
        db.collection("historial")
            .orderBy("timestamp", Query.Direction.DESCENDING) // Ordena del más nuevo al más viejo
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null || snapshot == null) return@addSnapshotListener

                // Transforma los documentos de Firebase a nuestra lista de datos
                listaHistorial = snapshot.documents.mapNotNull {
                    it.toObject(RegistroHistorial::class.java)
                }
            }
    }

    // Da el formato al texto dependiendo del estado
    val textoMostrar = when {
        isLoading -> "Cargando historial desde la nube..."
        listaHistorial.isEmpty() -> "No hay sesiones registradas actualmente."
        else -> listaHistorial.joinToString(separator = "\n\n--------------------------\n\n") { registro ->
            "📱 Dispositivo: ${registro.dispositivo}\n" +
                    "📅 Fecha: ${registro.fecha} | ⏰ Hora: ${registro.hora}\n" +
                    "ℹ️ Evento: ${registro.evento}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Text(
            text = "Historial en la Nube",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Registro de sesiones Bluetooth guardadas en Firebase",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, bottom = 30.dp)
        )

        Text(
            text = "📋 Registro de Sesiones",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = textoMostrar,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedButton(
            onClick = {
                // Borra todos los documentos de la colección "historial" en Firebase
                db.collection("historial").get().addOnSuccessListener { snapshot ->
                    val batch = db.batch()
                    for (documento in snapshot.documents) {
                        batch.delete(documento.reference)
                    }
                    batch.commit()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(bottom = 12.dp)
        ) {
            Text("Limpiar Historial en la Nube", color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = onBackClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Volver al Menú Principal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// Modelo de datos
data class RegistroHistorial(
    val dispositivo: String = "",
    val fecha: String = "",
    val hora: String = "",
    val evento: String = "",
    val timestamp: Long = 0L
)

object GestorHistorial {

    fun guardarEvento(
        nombreDispositivo: String = "Módulo Bluetooth",
        descripcionEvento: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // La instancia se pide aquí adentro para evitar advertencias de memoria
        val db = FirebaseFirestore.getInstance()

        val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfHora = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val ahora = Date()

        val nuevoRegistro = RegistroHistorial(
            dispositivo = nombreDispositivo,
            fecha = sdfFecha.format(ahora),
            hora = sdfHora.format(ahora),
            evento = descripcionEvento,
            timestamp = System.currentTimeMillis()
        )

        db.collection("historial")
            .add(nuevoRegistro)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Error") }
    }
}