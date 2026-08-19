package com.paperview.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Slider con etiquetas de extremo (p.ej. "Fría" .. "Cálida") como pide la
 *  maqueta de la sección 19. */
@Composable
fun LabeledSlider(
    title: String,
    leftLabel: String,
    rightLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(leftLabel, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(rightLabel, style = MaterialTheme.typography.bodySmall)
        }
    }
}
