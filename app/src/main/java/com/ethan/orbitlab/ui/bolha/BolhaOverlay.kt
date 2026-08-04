package com.ethan.orbitlab.ui.bolha

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlin.math.roundToInt

/**
 * O que se vê no overlay: a bolha da Luna.
 *
 * - arrastar → move a janela (o serviço reposiciona o [android.view.WindowManager]);
 * - soltar → gruda na borda mais perto;
 * - tocar → abre a Luna;
 * - toque longo → guarda a bolha (fecha o serviço).
 *
 * O padding de 12dp em volta dá "folga" pra sombra não ser cortada pela janela WRAP_CONTENT.
 */
@Composable
fun BolhaOverlay(
    onArrastar: (dx: Int, dy: Int) -> Unit,
    onSoltar: () -> Unit,
    onTocar: () -> Unit,
    onFechar: () -> Unit,
) {
    val bordaGradiente = Brush.horizontalGradient(
        listOf(
            OrbitTokens.bluePastel,
            OrbitTokens.bluePastel.copy(alpha = 0.5f),
        ),
    )

    Box(modifier = Modifier.padding(12.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = OrbitTokens.bluePastel.copy(alpha = 0.45f),
                    spotColor = Color.Black.copy(alpha = 0.55f),
                )
                .clip(CircleShape)
                .background(OrbitTokens.graphiteRaised)
                .border(1.dp, bordaGradiente, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTocar() },
                        onLongPress = { onFechar() },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { onSoltar() },
                        onDrag = { change, drag ->
                            change.consume()
                            onArrastar(drag.x.roundToInt(), drag.y.roundToInt())
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OrbitTokens.bluePastel.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Nightlight,
                    contentDescription = "Falar com a Luna",
                    tint = OrbitTokens.bluePastel,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
