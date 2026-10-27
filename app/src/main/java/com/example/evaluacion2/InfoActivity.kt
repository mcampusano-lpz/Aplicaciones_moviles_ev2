package com.example.evaluacion2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evaluacion2.ui.theme.Evaluacion2Theme

class InfoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Evaluacion2Theme {
                InfoScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@Composable
fun InfoScreen(onBackClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Tu nuevo fondo celeste crema
            .verticalScroll(rememberScrollState())
            .padding(32.dp) // Márgenes más amplios siendo más orgánico
    ) {
        // Título
        Text(
            text = "Guía de Conexión Bluetooth",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, // El azul oscuro suave
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Instrucciones
        val instrucciones = """
            Para que la aplicación detecte tu dispositivo (Android/iPhone) y calcule la distancia en tiempo real, sigue estos pasos:

            1. VINCULACIÓN PREVIA (Importante)
            La app escanea dispositivos seguros. Ve a los "Ajustes de Bluetooth" de tu Android/iPhone y asegúrate de emparejar (vincular) tu dispositivo primero.

            2. CONSEJOS PARA USUARIOS DE iPHONE
            El sistema de Apple (iOS) bloquea las señales constantes de Bluetooth para ahorrar batería y por privacidad. Para que la app mida la distancia correctamente:
            
            • Mantén la pantalla encendida en "Ajustes > Bluetooth" en el iPhone mientras haces la prueba.
            
            • O activa la opción "Compartir Internet" en el iPhone para forzar una emisión de señal continua.

            3. CÓMO ACTUALIZAR LA DISTANCIA
            El botón "Verificar Estado de Circuito" en el menú principal funciona como un actualizador. Al presionarlo, la app fuerza un nuevo escaneo del entorno y recalcula la distancia al instante basándose en la fuerza de la señal actual.
        """.trimIndent()

        Text(
            text = instrucciones,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onBackground, // Texto oscuro grisáceo
            modifier = Modifier.padding(bottom = 40.dp)
        )

        // Botón
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