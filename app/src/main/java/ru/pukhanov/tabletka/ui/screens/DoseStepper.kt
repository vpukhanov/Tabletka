package ru.pukhanov.tabletka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants

@Composable
fun DoseStepper(
    doses: Double,
    onDosesChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onDosesChange(maxOf(0.5, doses - 0.5))
            },
            enabled = doses > 0.5,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease doses",
                tint = if (doses > 0.5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(18.dp)
            )
        }

        val dosesText = if (doses % 1.0 == 0.0) {
            doses.toInt().toString()
        } else {
            doses.toString()
        }

        Text(
            text = "$dosesText ${if (doses == 1.0) "dose" else "doses"}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp)
        )

        IconButton(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onDosesChange(doses + 0.5)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase doses",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
