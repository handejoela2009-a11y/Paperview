package com.paperview.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paperview.app.data.PaperViewPresets
import com.paperview.app.data.PaperViewSettings

private data class CalibrationOption(val label: String, val description: String, val settings: PaperViewSettings)

private val calibrationOptions = listOf(
    CalibrationOption("A — Papel blanco", "Fondo claro y neutro, ideal si prefieres el mínimo cambio.", PaperViewPresets.PAPEL_BLANCO),
    CalibrationOption("B — Papel crema", "Cálido y suave, similar al papel de libro.", PaperViewPresets.PAPEL_CREMA),
    CalibrationOption("C — Libro", "Pensado para lectura prolongada.", PaperViewPresets.LIBRO),
    CalibrationOption("D — E-Ink simulado", "Colores muy poco saturados, apariencia mate.", PaperViewPresets.EINK_SIMULADO),
    CalibrationOption("E — Personalizado", "Empieza desde Papel Natural y ajusta todo a mano.", PaperViewPresets.PAPEL_NATURAL),
)

@Composable
fun CalibrationScreen(onSelected: (PaperViewSettings) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("ENCUENTRA TU CONFIGURACIÓN MÁS CÓMODA", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Elige la opción que visualmente te resulte más cómoda. Podrás cambiarla o " +
                    "ajustarla en cualquier momento.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }
        items(calibrationOptions) { option ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(option.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(option.description, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    androidx.compose.material3.Button(onClick = { onSelected(option.settings) }) {
                        Text("Elegir esta")
                    }
                }
            }
        }
    }
}
