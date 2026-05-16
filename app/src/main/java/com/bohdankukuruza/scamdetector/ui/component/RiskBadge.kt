package com.bohdankukuruza.scamdetector.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bohdankukuruza.scamdetector.data.model.RiskLevel

/**
 * Small coloured pill showing the assessed [RiskLevel].
 *
 * Colour is taken from [RiskLevel.colorHex] so the data layer remains
 * the single source of truth for risk presentation — the UI never
 * hardcodes a colour per level.
 */
@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val color = Color(android.graphics.Color.parseColor(riskLevel.colorHex))

    Text(
        text = riskLevel.displayName.uppercase(),
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = modifier
            .background(color = color, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}