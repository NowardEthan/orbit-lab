package com.ethan.orbitlab.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ethan.orbitlab.ui.theme.OrbitTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Foto ou vídeo — muda o que a câmera monta por baixo. */
enum class ModoCaptura { FOTO, VIDEO }

private fun arquivoCaptura(context: Context, extensao: String): File {
    val dir = File(context.cacheDir, "camera").also { it.mkdirs() }
    return File(dir, "capture_${System.currentTimeMillis()}.$extensao")
}

private fun uriDoArquivo(context: Context, arquivo: File): Uri? = runCatching {
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
}.getOrNull()

internal fun temPermissao(context: Context, permissao: String): Boolean =
    ContextCompat.checkSelfPermission(context, permissao) == PackageManager.PERMISSION_GRANTED

@Composable
private fun CameraConteudo(
    modoInicial: ModoCaptura,
    onFechar: () -> Unit,
    onCapturado: (ComposerAttachment) -> Unit,
) {
    val context = LocalContext.current
    val dono = LocalLifecycleOwner.current

    var modo by remember { mutableStateOf(modoInicial) }
    var frontal by remember { mutableStateOf(false) }
    var permitida by remember { mutableStateOf(temPermissao(context, Manifest.permission.CAMERA)) }
    var audioOk by remember { mutableStateOf(temPermissao(context, Manifest.permission.RECORD_AUDIO)) }
    var ocupado by remember { mutableStateOf(false) }
    var gravando by remember { mutableStateOf(false) }
    var segundos by remember { mutableStateOf(0) }
    var falha by remember { mutableStateOf<String?>(null) }

    val provedorRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val fotoRef = remember { mutableStateOf<ImageCapture?>(null) }
    val videoRef = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val gravacaoRef = remember { mutableStateOf<Recording?>(null) }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    val pedirCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { res ->
        permitida = res[Manifest.permission.CAMERA] ?: permitida
        audioOk = res[Manifest.permission.RECORD_AUDIO] ?: audioOk
    }

    LaunchedEffect(Unit) {
        val faltando = buildList {
            if (!temPermissao(context, Manifest.permission.CAMERA)) add(Manifest.permission.CAMERA)
            if (!temPermissao(context, Manifest.permission.RECORD_AUDIO)) add(Manifest.permission.RECORD_AUDIO)
        }
        if (faltando.isNotEmpty()) pedirCamera.launch(faltando.toTypedArray())
    }

    // Monta/remonta a câmera quando muda o modo ou a lente.
    LaunchedEffect(permitida, modo, frontal) {
        if (!permitida) return@LaunchedEffect
        val provedor = runCatching { provedorCamera(context) }.getOrNull()
        if (provedor == null) {
            falha = "Não consegui abrir a câmera."
            return@LaunchedEffect
        }
        provedorRef.value = provedor
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val seletor = CameraSelector.Builder()
            .requireLensFacing(
                if (frontal) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK,
            )
            .build()
        runCatching {
            provedor.unbindAll()
            if (modo == ModoCaptura.FOTO) {
                val captura = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provedor.bindToLifecycle(dono, seletor, preview, captura)
                fotoRef.value = captura
                videoRef.value = null
            } else {
                val gravador = Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.from(
                            Quality.HD,
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
                        ),
                    )
                    .build()
                val captura = VideoCapture.withOutput(gravador)
                provedor.bindToLifecycle(dono, seletor, preview, captura)
                videoRef.value = captura
                fotoRef.value = null
            }
            falha = null
        }.onFailure { falha = "Não consegui abrir a câmera." }
    }

    // Solta a câmera ao fechar — senão fica segurando memória (e o app é justo o que
    // estamos tentando manter vivo). O bind é no ciclo da Activity, que continua de pé.
    DisposableEffect(Unit) {
        onDispose {
            runCatching { gravacaoRef.value?.stop() }
            gravacaoRef.value = null
            runCatching { provedorRef.value?.unbindAll() }
            provedorRef.value = null
        }
    }

    LaunchedEffect(gravando) {
        segundos = 0
        while (gravando) {
            delay(1000)
            segundos += 1
        }
    }

    fun tirarFoto() {
        val captura = fotoRef.value ?: return
        if (ocupado) return
        ocupado = true
        val arquivo = arquivoCaptura(context, "jpg")
        val opcoes = ImageCapture.OutputFileOptions.Builder(arquivo)
            .setMetadata(ImageCapture.Metadata().apply { isReversedHorizontal = frontal })
            .build()
        captura.takePicture(
            opcoes,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(saida: ImageCapture.OutputFileResults) {
                    ocupado = false
                    val uri = uriDoArquivo(context, arquivo)
                    if (uri == null) {
                        falha = "Não consegui guardar a foto."
                        return
                    }
                    onCapturado(
                        ComposerAttachment(
                            id = "cam-${System.currentTimeMillis()}",
                            kind = AttachmentKind.IMAGE,
                            name = "Foto",
                            sizeLabel = tamanhoLegivel(arquivo.length()),
                            mime = "image/jpeg",
                            uri = uri,
                        ),
                    )
                    onFechar()
                }

                override fun onError(erro: ImageCaptureException) {
                    ocupado = false
                    falha = "A foto não saiu. Tenta de novo?"
                }
            },
        )
    }

    fun alternarGravacao() {
        val captura = videoRef.value ?: return
        val emCurso = gravacaoRef.value
        if (emCurso != null) {
            emCurso.stop()
            return
        }
        val arquivo = arquivoCaptura(context, "mp4")
        val opcoes = FileOutputOptions.Builder(arquivo).build()
        val pendente = captura.output.prepareRecording(context, opcoes)
            .apply { if (audioOk) runCatching { withAudioEnabled() } }
        gravacaoRef.value = pendente.start(ContextCompat.getMainExecutor(context)) { evento ->
            when (evento) {
                is VideoRecordEvent.Start -> gravando = true
                is VideoRecordEvent.Finalize -> {
                    gravando = false
                    gravacaoRef.value = null
                    if (evento.hasError()) {
                        falha = "O vídeo não gravou. Tenta de novo?"
                        return@start
                    }
                    val uri = uriDoArquivo(context, arquivo)
                    if (uri == null) {
                        falha = "Não consegui guardar o vídeo."
                        return@start
                    }
                    onCapturado(
                        ComposerAttachment(
                            id = "vid-${System.currentTimeMillis()}",
                            kind = AttachmentKind.VIDEO,
                            name = "Vídeo",
                            sizeLabel = tamanhoLegivel(arquivo.length()),
                            mime = "video/mp4",
                            uri = uri,
                        ),
                    )
                    onFechar()
                }
                else -> Unit
            }
        }
    }

    CameraPalco(
        previewView = previewView,
        permitida = permitida,
        modo = modo,
        gravando = gravando,
        ocupado = ocupado,
        segundos = segundos,
        falha = falha,
        onTrocarModo = { novo -> if (!gravando) modo = novo },
        onVirarLente = { if (!gravando) frontal = !frontal },
        onDisparar = { if (modo == ModoCaptura.FOTO) tirarFoto() else alternarGravacao() },
        onPedirPermissao = { pedirCamera.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) },
        onFechar = { if (gravando) gravacaoRef.value?.stop() else onFechar() },
    )
}

@Composable
private fun CameraPalco(
    previewView: PreviewView,
    permitida: Boolean,
    modo: ModoCaptura,
    gravando: Boolean,
    ocupado: Boolean,
    segundos: Int,
    falha: String?,
    onTrocarModo: (ModoCaptura) -> Unit,
    onVirarLente: () -> Unit,
    onDisparar: () -> Unit,
    onPedirPermissao: () -> Unit,
    onFechar: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (permitida) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "A câmera precisa da sua permissão",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ela abre aqui dentro do Orbit — assim o app não é fechado pelo sistema no meio.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(OrbitTokens.accent)
                        .clickable { onPedirPermissao() }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Text("Permitir", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotaoRedondo(icone = Icons.Rounded.Close, descricao = "Fechar", onClick = onFechar)
            Spacer(Modifier.weight(1f))
            if (gravando) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFFE5484D)))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "%d:%02d".format(segundos / 60, segundos % 60),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        if (falha != null) {
            Text(
                falha,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!gravando) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(4.dp),
                ) {
                    ModoPill("Foto", modo == ModoCaptura.FOTO) { onTrocarModo(ModoCaptura.FOTO) }
                    ModoPill("Vídeo", modo == ModoCaptura.VIDEO) { onTrocarModo(ModoCaptura.VIDEO) }
                }
                Spacer(Modifier.height(18.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Spacer(Modifier.size(46.dp))
                Disparador(modo = modo, gravando = gravando, ocupado = ocupado, onClick = onDisparar)
                if (gravando) {
                    Spacer(Modifier.size(46.dp))
                } else {
                    BotaoRedondo(
                        icone = Icons.Rounded.Cameraswitch,
                        descricao = "Virar câmera",
                        onClick = onVirarLente,
                    )
                }
            }
        }
    }
}

@Composable
private fun BotaoRedondo(icone: ImageVector, descricao: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icone, contentDescription = descricao, tint = Color.White, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun ModoPill(rotulo: String, ativo: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (ativo) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 7.dp),
    ) {
        Text(
            rotulo,
            color = if (ativo) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = if (ativo) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun Disparador(modo: ModoCaptura, gravando: Boolean, ocupado: Boolean, onClick: () -> Unit) {
    val miolo by animateColorAsState(
        targetValue = when {
            gravando -> Color(0xFFE5484D)
            modo == ModoCaptura.VIDEO -> Color(0xFFE5484D)
            else -> Color.White
        },
        animationSpec = tween(180),
        label = "miolo",
    )
    Box(
        Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(999.dp))
            .clickable(enabled = !ocupado) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(if (gravando) 28.dp else 60.dp)
                .clip(RoundedCornerShape(if (gravando) 8.dp else 999.dp))
                .background(miolo.copy(alpha = if (ocupado) 0.5f else 1f)),
        )
    }
}

private fun tamanhoLegivel(bytes: Long): String = when {
    bytes <= 0L -> "—"
    bytes < 1024L * 1024 -> "${(bytes / 1024f).toInt()} KB"
    else -> String.format("%.1f MB", bytes / (1024f * 1024f))
}

/**
 * Câmera **dentro** do app.
 *
 * A câmera do sistema é outro aplicativo: enquanto ela está aberta, o Android mata o Orbit
 * em segundo plano (num celular apertado de memória isso é quase certo) e o app reinicia ao
 * voltar — perdendo a foto. Com a câmera aqui, o Orbit nunca sai da frente.
 */
@Composable
fun CameraNoAppSheet(
    visivel: Boolean,
    modoInicial: ModoCaptura = ModoCaptura.FOTO,
    onFechar: () -> Unit,
    onCapturado: (ComposerAttachment) -> Unit,
) {
    if (!visivel) return
    Dialog(
        onDismissRequest = onFechar,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        CameraConteudo(modoInicial = modoInicial, onFechar = onFechar, onCapturado = onCapturado)
    }
}

/** Espera o provedor da câmera sem bloquear a thread. */
internal suspend fun provedorCamera(context: Context): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val futuro = ProcessCameraProvider.getInstance(context)
        futuro.addListener(
            {
                runCatching { futuro.get() }
                    .onSuccess { if (cont.isActive) cont.resume(it) }
                    .onFailure { if (cont.isActive) cont.resumeWithException(it) }
            },
            ContextCompat.getMainExecutor(context),
        )
    }
