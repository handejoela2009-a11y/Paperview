package com.paperview.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paperview.app.data.PaperViewPresets
import com.paperview.app.ui.components.LabeledSlider
import com.paperview.app.viewmodel.PaperViewModel
import com.paperview.app.viewmodel.PaperViewUiState

@Composable
fun MainScreen(
    state: PaperViewUiState,
    viewModel: PaperViewModel,
    onRequestOverlayPermission: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("PaperView", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

        if (!state.hasOverlayPermission) {
            item { PermissionBanner(onRequestOverlayPermission) }
        }

        item { StatusCard(state, viewModel) }

        item { PresetDropdown(state, viewModel) }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ajustes finos", style = MaterialTheme.typography.titleMedium)
                    LabeledSlider("Intensidad", "Suave", "Fuerte", state.settings.intensity, viewModel::updateIntensity)
                    LabeledSlider("Temperatura", "Fría", "Cálida", state.settings.temperature, viewModel::updateTemperature)
                    LabeledSlider("Componente azul", "Normal", "Reducida", state.settings.blueReduction, viewModel::updateBlueReduction)
                    LabeledSlider("Saturación", "Suave", "Natural", state.settings.saturation, viewModel::updateSaturation)
                    LabeledSlider("Contraste", "Bajo", "Alto", state.settings.contrast, viewModel::updateContrast)
                    LabeledSlider("Oscurecimiento visual", "Ninguno", "Máximo", state.settings.overlayDimming, viewModel::updateOverlayDimming)
                    OutlinedButton(onClick = { viewModel.restoreRecommended() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("RESTAURAR CONFIGURACIÓN RECOMENDADA")
                    }
                }
            }
        }

        item { AutoAdaptationCard(state, viewModel) }

        item { ComfortCard() }

        item { BreakReminderCard(state, viewModel) }
    }
}

@Composable
private fun PermissionBanner(onRequestOverlayPermission: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Falta un permiso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "PaperView necesita el permiso \"Mostrar sobre otras apps\" para dibujar el filtro visual. " +
                    "Sin él, la app no puede aplicar ningún efecto: no se simulará que está activo.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Button(onClick = onRequestOverlayPermission) { Text("Conceder permiso") }
        }
    }
}

@Composable
private fun StatusCard(state: PaperViewUiState, viewModel: PaperViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row {
                Icon(
                    Icons.Filled.Circle,
                    contentDescription = null,
                    tint = if (state.isActive) Color(0xFF4C8C4A) else Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                )
                Column {
                    Text(if (state.isActive) "PaperView activo" else "PaperView inactivo", fontWeight = FontWeight.Bold)
                    Text(
                        "Sensor de luz: " + if (state.lightSensorAvailable) "disponible" else "no disponible",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Switch(checked = state.isActive, onCheckedChange = { viewModel.toggleActive() }, enabled = state.hasOverlayPermission)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDropdown(state: PaperViewUiState, viewModel: PaperViewModel) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = state.settings.displayName,
            onValueChange = {},
            label = { Text("Perfil") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PaperViewPresets.ALL.forEach { preset ->
                DropdownMenuItem(
                    text = { Text(preset.displayName) },
                    onClick = {
                        viewModel.selectPreset(preset)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AutoAdaptationCard(state: PaperViewUiState, viewModel: PaperViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Adaptación automática", fontWeight = FontWeight.Bold)
                Text(
                    "Ajusta temperatura y oscurecimiento según la hora y la luz ambiental, " +
                        "siempre con transiciones progresivas.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = state.settings.autoAdaptationEnabled, onCheckedChange = { viewModel.setAutoAdaptation(it) })
        }
    }
}

@Composable
private fun ComfortCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Comodidad", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            val tips = listOf(
                "Mantén una distancia adecuada de la pantalla.",
                "Evita usar brillo excesivo en habitaciones oscuras.",
                "Realiza descansos periódicos durante sesiones largas.",
                "Ajusta el perfil si notas incomodidad visual.",
                "Si el malestar persiste (dolor ocular, visión borrosa, dolores de cabeza), consulta a un profesional.",
            )
            tips.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
            Text(
                "Esta sección ofrece recomendaciones generales de comodidad visual, no un diagnóstico médico.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BreakReminderCard(state: PaperViewUiState, viewModel: PaperViewModel) {
    val options = listOf(0L to "Desactivado", 20L to "20 min", 30L to "30 min", 45L to "45 min", 60L to "60 min")
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Recordatorio de descanso", fontWeight = FontWeight.Bold)
            Row(Modifier.padding(top = 8.dp)) {
                options.forEach { (minutes, label) ->
                    OutlinedButton(
                        onClick = { viewModel.setBreakReminder(minutes) },
                        modifier = Modifier.padding(end = 6.dp),
                    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
