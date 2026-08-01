package com.ethan.orbitlab.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tipografia base — corpo em Hanken (o padrão do app), títulos ganham Bricolage
// explicitamente nas telas. Ver OrbitType.kt.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Hanken,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)