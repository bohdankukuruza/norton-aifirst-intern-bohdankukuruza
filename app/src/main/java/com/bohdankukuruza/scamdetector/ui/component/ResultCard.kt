package com.bohdankukuruza.scamdetector.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bohdankukuruza.scamdetector.data.model.AnalysisResult

/**
 * Card showing a completed [AnalysisResult]: the risk badge, the
 * confidence percentage, and a bulleted list of contributing signals.
 *
 * When the result has no signals (a clean SAFE verdict) the body
 * shows a brief reassurance message instead of an empty list.
 */
@Composable
fun ResultCard(
    result: AnalysisResult,
    modifier: Modifier = Modifier
) {
    val confidencePct = (result.confidence * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RiskBadge(riskLevel = result.riskLevel)
                Text(
                    text = "Confidence: $confidencePct%",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (result.signals.isEmpty()) {
                Text(
                    text = "No scam indicators detected. The message looks clean.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Why we flagged this:",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                result.signals.forEach { signal ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "•  ",
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = signal.explanation,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}