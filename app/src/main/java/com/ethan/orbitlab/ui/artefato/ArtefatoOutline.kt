package com.ethan.orbitlab.ui.artefato

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.artefato.BlocoArtefato
import com.ethan.orbitlab.data.artefato.headingsOutline
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.OrbitType
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Outline lateral/sheet a partir dos headings — N3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtefatoOutlineSheet(
    blocos: List<BlocoArtefato>,
    onDismiss: () -> Unit,
    onIrPara: (BlocoArtefato) -> Unit,
) {
    val headings = headingsOutline(blocos)
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = OrbitTokens.graphiteRaised,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                "Índice",
                color = OrbitTokens.textHiN,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OrbitType.display,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (headings.isEmpty()) "Nenhum título nesta página ainda."
                else "${headings.size} seção(ões)",
                color = OrbitTokens.textMidN,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(14.dp))

            if (headings.isEmpty()) {
                Text(
                    "Adicione um Título 1–3 (digite / no editor) pra montar o índice.",
                    color = OrbitTokens.textLowN,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(headings, key = { it.id }) { h ->
                        val nivel = h.props?.level ?: 1
                        val shape = RoundedCornerShape(12.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(OrbitTokens.graphiteSurf)
                                .border(1.dp, OrbitTokens.graphiteHair, shape)
                                .orbitPressable {
                                    onIrPara(h)
                                    onDismiss()
                                }
                                .padding(
                                    start = (12 + (nivel - 1) * 14).dp,
                                    end = 14.dp,
                                    top = 12.dp,
                                    bottom = 12.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "H$nivel",
                                color = OrbitTokens.bluePastel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                h.text.ifBlank { "(sem título)" },
                                color = OrbitTokens.textHiN,
                                fontSize = 15.sp,
                                fontWeight = if (nivel == 1) FontWeight.SemiBold else FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
