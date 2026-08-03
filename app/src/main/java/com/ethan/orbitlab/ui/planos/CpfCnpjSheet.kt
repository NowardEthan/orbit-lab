package com.ethan.orbitlab.ui.planos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.data.PrefsRepository
import com.ethan.orbitlab.data.billing.cpfCnpjValido
import com.ethan.orbitlab.data.billing.formatarCpfCnpj
import com.ethan.orbitlab.data.billing.normalizarCpfCnpj
import com.ethan.orbitlab.ui.theme.Bricolage
import com.ethan.orbitlab.ui.theme.OrbitFills
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable

/**
 * Pede CPF/CNPJ antes do checkout Asaas (obrigatório no Brasil). Porta do
 * `CpfCnpjSheet.tsx` do RN: máscara ao digitar, valida por tamanho (11/14), salva
 * pra pré-preencher da próxima. Sheet de fundo SÓLIDO (nada de Glass).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpfCnpjSheet(
    onDismiss: () -> Unit,
    onConfirmar: (digitos: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var texto by remember {
        mutableStateOf(PrefsRepository.cpfCnpjSalvo?.let { formatarCpfCnpj(it) } ?: "")
    }
    var erro by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OrbitTokens.graphiteSurf,
        contentColor = OrbitTokens.textHiN,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(OrbitTokens.textLowN.copy(alpha = 0.45f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = OrbitMetrics.pagePadding)
                .padding(top = 4.dp, bottom = 20.dp),
        ) {
            Text(
                "Dados para pagamento",
                color = OrbitTokens.textHiN,
                fontSize = 20.sp,
                fontFamily = Bricolage,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Precisamos do seu CPF ou CNPJ para emitir a cobrança com segurança.",
                color = OrbitTokens.textMidN,
                fontSize = 13.5.sp,
                lineHeight = 19.sp,
            )

            Spacer(Modifier.height(16.dp))
            BasicTextField(
                value = texto,
                onValueChange = { novo ->
                    texto = formatarCpfCnpj(novo)
                    erro = null
                },
                singleLine = true,
                cursorBrush = SolidColor(OrbitTokens.bluePastel),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = TextStyle(color = OrbitTokens.textHiN, fontSize = 17.sp),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(OrbitTokens.graphiteRaised)
                            .border(1.dp, OrbitTokens.graphiteHair, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                    ) {
                        if (texto.isEmpty()) {
                            Text("000.000.000-00", color = OrbitTokens.textLowN, fontSize = 17.sp)
                        }
                        inner()
                    }
                },
            )

            if (erro != null) {
                Spacer(Modifier.height(8.dp))
                Text(erro!!, color = OrbitTokens.danger, fontSize = 12.5.sp)
            }

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(OrbitFills.accent.brush)
                    .orbitPressable {
                        val digitos = normalizarCpfCnpj(texto)
                        if (!cpfCnpjValido(digitos)) {
                            erro = "Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido."
                        } else {
                            PrefsRepository.cpfCnpjSalvo = digitos
                            onConfirmar(digitos)
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Continuar para pagamento",
                    color = OrbitFills.accent.onFill,
                    fontSize = 14.5.sp,
                    fontFamily = Bricolage,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .orbitPressable(onClick = onDismiss)
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Cancelar", color = OrbitTokens.textMidN, fontSize = 13.5.sp)
            }
        }
    }
}
