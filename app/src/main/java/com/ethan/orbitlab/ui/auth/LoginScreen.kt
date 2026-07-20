package com.ethan.orbitlab.ui.auth

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.orbitlab.R
import com.ethan.orbitlab.data.AuthRepository
import com.ethan.orbitlab.ui.theme.OrbitMetrics
import com.ethan.orbitlab.ui.theme.OrbitMotion
import com.ethan.orbitlab.ui.theme.OrbitTokens
import com.ethan.orbitlab.ui.theme.orbitPressable
import kotlinx.coroutines.launch

private enum class LoginMode { Entrar, Criar }

private val AuraNight = Color(0xFF0D1119)
private val AuraBlue = Color(0xFF4D6FF7)
private val FieldShape = RoundedCornerShape(14.dp)
private val GoogleBlue = Color(0xFF4285F4)

/**
 * Portão de entrada — Conta Aura (email/senha Firebase) + Google.
 * Sem visitante / anônimo.
 */
@Composable
fun LoginScreen(
    onAutenticado: () -> Unit,
) {
    var mode by remember { mutableStateOf(LoginMode.Entrar) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mostrarSenha by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) {
        visible = true
    }

    fun runAuth(block: suspend () -> Result<*>) {
        if (busy) return
        erro = null
        busy = true
        scope.launch {
            val result = block()
            busy = false
            result.fold(
                onSuccess = { onAutenticado() },
                onFailure = { e ->
                    erro = e.message ?: "Não deu pra continuar."
                },
            )
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AuraNight),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        0f to AuraBlue.copy(alpha = 0.28f),
                        0.55f to AuraBlue.copy(alpha = 0.06f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(OrbitMotion.tweenMed) + slideInVertically { it / 12 },
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OrbitMetrics.pagePadding)
                    .padding(top = 28.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_aura_symbol),
                    contentDescription = "Aura",
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Aura",
                    color = OrbitTokens.textHigh,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Orbit · Luna",
                    color = AuraBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    when (mode) {
                        LoginMode.Entrar -> "Entre com sua Conta Aura para sincronizar conversas e mídia."
                        LoginMode.Criar -> "Crie sua Conta Aura — a casa do Orbit e da Luna."
                    },
                    color = OrbitTokens.textMid,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                Spacer(Modifier.height(28.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(OrbitTokens.surface.copy(alpha = 0.92f))
                        .border(1.dp, OrbitTokens.borderSoft, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (mode == LoginMode.Criar) {
                        AuthField(
                            label = "Nome",
                            value = nome,
                            onValueChange = { nome = it },
                            placeholder = "Como a Luna te chama",
                            enabled = !busy,
                            imeAction = ImeAction.Next,
                            onNext = { focus.moveFocus(FocusDirection.Down) },
                        )
                    }
                    AuthField(
                        label = "Email",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "voce@email.com",
                        enabled = !busy,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onNext = { focus.moveFocus(FocusDirection.Down) },
                    )
                    AuthField(
                        label = "Senha",
                        value = senha,
                        onValueChange = { senha = it },
                        placeholder = if (mode == LoginMode.Criar) "Mínimo 8 caracteres" else "Sua senha",
                        enabled = !busy,
                        isPassword = true,
                        passwordVisible = mostrarSenha,
                        onTogglePassword = { mostrarSenha = !mostrarSenha },
                        imeAction = ImeAction.Done,
                        onDone = {
                            if (mode == LoginMode.Criar) {
                                runAuth { AuthRepository.criarContaAura(nome, email, senha) }
                            } else {
                                runAuth { AuthRepository.entrarComAura(email, senha) }
                            }
                        },
                    )
                    PrimaryButton(
                        label = when {
                            busy && mode == LoginMode.Criar -> "Criando…"
                            busy -> "Entrando…"
                            mode == LoginMode.Criar -> "Criar Conta Aura"
                            else -> "Entrar com Conta Aura"
                        },
                        loading = busy,
                        enabled = !busy,
                        onClick = {
                            if (mode == LoginMode.Criar) {
                                runAuth { AuthRepository.criarContaAura(nome, email, senha) }
                            } else {
                                runAuth { AuthRepository.entrarComAura(email, senha) }
                            }
                        },
                    )

                    if (!erro.isNullOrBlank()) {
                        Text(
                            erro!!,
                            color = OrbitTokens.danger,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                DividerOu()
                Spacer(Modifier.height(18.dp))

                GoogleButton(
                    enabled = !busy && activity != null,
                    loading = busy,
                    onClick = {
                        val act = activity
                        if (act == null) {
                            erro = "Não foi possível abrir o Google Sign-In."
                            return@GoogleButton
                        }
                        runAuth { AuthRepository.entrarComGoogle(act) }
                    },
                )

                Spacer(Modifier.height(22.dp))

                when (mode) {
                    LoginMode.Entrar -> {
                        TextLink(
                            "Criar Conta Aura",
                            onClick = {
                                erro = null
                                mode = LoginMode.Criar
                            },
                        )
                    }
                    LoginMode.Criar -> {
                        TextLink(
                            "Já tenho Conta Aura",
                            onClick = {
                                erro = null
                                mode = LoginMode.Entrar
                            },
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    "Aura é a empresa por trás do Orbit e da Luna.\nConversas, imagens e vídeos sincronizam com a mesma nuvem do Orbit.",
                    color = OrbitTokens.textLow,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            color = OrbitTokens.textMid,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(FieldShape)
                .background(OrbitTokens.ink1)
                .border(1.dp, OrbitTokens.borderSoft, FieldShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = OrbitTokens.textHigh,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(AuraBlue),
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                    imeAction = imeAction,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { onNext?.invoke() },
                    onDone = { onDone?.invoke() },
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, color = OrbitTokens.textLow, fontSize = 15.sp)
                        }
                        inner()
                    }
                },
            )
            if (isPassword && onTogglePassword != null) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .orbitPressable(onClick = onTogglePassword),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                        tint = OrbitTokens.textMid,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(if (enabled) AuraBlue else AuraBlue.copy(alpha = 0.35f))
            .orbitPressable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GoogleButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(OrbitTokens.surface)
            .border(1.dp, OrbitTokens.border, shape)
            .orbitPressable(enabled = enabled && !loading, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(GoogleBlue),
            contentAlignment = Alignment.Center,
        ) {
            Text("G", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "Continuar com Google",
            color = OrbitTokens.textHigh,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DividerOu() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(OrbitTokens.borderSoft))
        Text(
            "ou",
            color = OrbitTokens.textLow,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(OrbitTokens.borderSoft))
    }
}

@Composable
private fun TextLink(
    label: String,
    muted: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        label,
        color = if (muted) OrbitTokens.textMid else AuraBlue,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.orbitPressable(onClick = onClick),
    )
}
