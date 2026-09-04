package io.github.xchat.features.items.chat

import android.media.MediaPlayer
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Music_note
import com.composables.icons.materialsymbols.outlined.Play_circle
import com.composables.icons.materialsymbols.outlined.Send
import dev.ujhhgtg.comptime.This
import io.github.xchat.features.api.core.WeMessageApi
import io.github.xchat.features.api.ui.WeCurrentConversationApi
import io.github.xchat.features.core.ClickableFeature
import io.github.xchat.features.core.Feature
import io.github.xchat.preferences.WePrefs
import io.github.xchat.ui.content.AlertDialogContent
import io.github.xchat.ui.utils.showComposeDialog
import io.github.xchat.utils.AudioUtils
import io.github.xchat.utils.EdgeTtsClient
import io.github.xchat.utils.WeLogger
import io.github.xchat.utils.android.showToast
import io.github.xchat.utils.android.showToastSuspend
import io.github.xchat.utils.coerceToInt
import io.github.xchat.utils.fs.KnownPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div

private const val TAG = "VoiceWorkbench"

/**
 * 语音工作台 (整合 Wtonec 的语音工作流公开契约):
 *  1. 文字转语音: 基于免费的 Edge TTS, 支持音色 / 语速 / 音调 / 音量调节, 可试听与直接发送。
 *  2. 本地语音包: 将导入的音频文件集中存放, 可试听并作为语音消息发送。
 * 入口: 从模块设置页点击「语音工作台」打开面板。
 */
@Feature(
    name = "语音工作台",
    categories = ["聊天"],
    description = "整合文字转语音与本地语音包\n点击本入口打开面板\n1. 文字转语音 (Edge TTS, 免费, 可调语速/音调/音量, 可试听)\n2. 语音包 (导入 / 试听 / 发送)"
)
object VoiceWorkbench : ClickableFeature() {

    override val noSwitchWidget = true

    // Edge TTS 可选音色 (voice name -> 展示名称)。
    val TTS_VOICES = listOf(
        "zh-CN-XiaoxiaoNeural" to "晓晓 (女, 温柔)",
        "zh-CN-XiaoyiNeural" to "晓伊 (女, 活泼)",
        "zh-CN-YunxiNeural" to "云希 (男, 阳光)",
        "zh-CN-YunyangNeural" to "云扬 (男, 播报)",
        "zh-CN-YunjianNeural" to "云健 (男, 浑厚)",
        "zh-CN-YunxiaNeural" to "云夏 (男, 少年)",
        "zh-CN-liaoning-XiaobeiNeural" to "晓北 (女, 东北话)",
        "zh-CN-shaanxi-XiaoniNeural" to "晓妮 (女, 陕西话)",
        "zh-HK-HiuMaanNeural" to "曉曼 (女, 粤语)",
        "zh-HK-WanLungNeural" to "雲龍 (男, 粤语)",
        "zh-TW-HsiaoChenNeural" to "曉臻 (女, 台湾)",
        "zh-TW-YunJheNeural" to "雲哲 (男, 台湾)",
        "en-US-AriaNeural" to "Aria (女, 英语)",
        "en-US-GuyNeural" to "Guy (男, 英语)",
        "ja-JP-NanamiNeural" to "七海 (女, 日语)",
    )

    const val DEFAULT_TTS_VOICE = "zh-CN-XiaoxiaoNeural"

    var ttsVoice by WePrefs.prefOption("wb_tts_voice", DEFAULT_TTS_VOICE)
    var ttsRate by WePrefs.prefOption("wb_tts_rate", "+0%")
    var ttsPitch by WePrefs.prefOption("wb_tts_pitch", "+0Hz")
    var ttsVolume by WePrefs.prefOption("wb_tts_volume", "+0%")

    /** 语音包目录: /storage/emulated/0/Download/Xchat/voicepack */
    val voicePackDir: File by lazy {
        (KnownPaths.downloads / "voicepack").toFile().apply { mkdirs() }
    }
    override fun onClick(context: ComponentActivity) {
        showVoiceWorkbench(context)
    }
}

private fun showVoiceWorkbench(context: android.content.Context) {
    showComposeDialog(context) {
        FixedTabsPanel(onDismiss = onDismiss)
    }
}

@Composable
private fun FixedTabsPanel(onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(0) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabLabel("文字转语音", selected = tab == 0, onClick = { tab = 0 })
            TabLabel("语音包", selected = tab == 1, onClick = { tab = 1 })
        }
        Spacer(Modifier.padding(top = 8.dp))

        when (tab) {
            0 -> TtsTab(onDismiss = onDismiss)
            1 -> VoicePackTab()
        }
    }
}

@Composable
private fun TabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun TtsTab(onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var voice by remember { mutableStateOf(VoiceWorkbench.ttsVoice) }
    var rate by remember { mutableStateOf(0f) }      // -50% .. +50%
    var pitch by remember { mutableStateOf(0f) }     // -20Hz .. +20Hz
    var vol by remember { mutableStateOf(0f) }       // -50% .. +50%

    // 播放当前合成的预览
    var previewPath by remember { mutableStateOf<String?>(null) }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }

    val rateArg = "${rate.toInt().let { if (it >= 0) "+$it" else it.toString() }}%"
    val pitchArg = "${pitch.toInt().let { if (it >= 0) "+$it" else it.toString() }}Hz"
    val volArg = "${vol.toInt().let { if (it >= 0) "+$it" else it.toString() }}%"

    Column {
        Text("音色", style = MaterialTheme.typography.labelLarge)
        VoiceWorkbench.TTS_VOICES.forEach { (v, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { voice = v }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                RadioButton(selected = voice == v, onClick = { voice = v })
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.padding(top = 8.dp))
        Text("文本", style = MaterialTheme.typography.labelLarge)
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("输入要转换为语音的文字") }
        )

        Spacer(Modifier.padding(top = 8.dp))
        LabeledSlider("语速  $rateArg", rate) { rate = it }
        LabeledSlider("音调  $pitchArg", pitch) { pitch = it }
        LabeledSlider("音量  $volArg", vol) { vol = it }

        Spacer(Modifier.padding(top = 12.dp))
        Button(
            onClick = {
                player.value?.release()
                player.value = null
                previewPath = null
                onDismiss()
                VoiceWorkbench.ttsVoice = voice
                VoiceWorkbench.ttsRate = rateArg
                VoiceWorkbench.ttsPitch = pitchArg
                VoiceWorkbench.ttsVolume = volArg
                if (text.isBlank()) {
                    showToast("输入内容为空!")
                    return@Button
                }
                synthesizeAndSendVoice(text, voice, rateArg, volArg, pitchArg)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(MaterialSymbols.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("生成并发送")
        }

        Spacer(Modifier.padding(top = 8.dp))
        Row {
            TextButton(onClick = {
                if (text.isBlank()) {
                    showToast("输入内容为空!")
                    return@TextButton
                }
                previewMp3(text, voice, rateArg, volArg, pitchArg) { path ->
                    previewPath = path
                    player.value?.release()
                    player.value = MediaPlayer().apply {
                        setDataSource(path)
                        prepare()
                        start()
                    }
                }
            }) {
                Icon(MaterialSymbols.Outlined.Play_circle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("试听")
            }
            TextButton(onClick = {
                player.value?.release()
                player.value = null
                previewPath?.let { File(it).delete() }
                previewPath = null
            }) {
                Text("停止")
            }
        }

        previewPath?.let {
            Text("已合成: $it", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun VoicePackTab() {
    val files = remember { mutableStateOf(loadVoicePacks()) }

    Column {
        Text(
            "语音包目录: ${VoiceWorkbench.voicePackDir}",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.padding(top = 8.dp))

        if (files.value.isEmpty()) {
            Text(
                "暂无语音包\n请在手机存储 ${VoiceWorkbench.voicePackDir} 目录下放入 MP3 / AMR / SILK 音频文件",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            files.value.forEach { f ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Icon(
                        MaterialSymbols.Outlined.Music_note,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        f.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        previewLocalFile(f)
                    }) {
                        Icon(MaterialSymbols.Outlined.Play_circle, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("试听")
                    }
                    TextButton(onClick = {
                        val conv = WeCurrentConversationApi.value
                        if (conv.isBlank()) {
                            showToast("请先打开一个聊天")
                            return@TextButton
                        }
                        sendVoiceFile(f, conv)
                    }) {
                        Icon(MaterialSymbols.Outlined.Send, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("发送")
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = -50f..50f
    )
}

private fun loadVoicePacks(): List<File> {
    val dir = VoiceWorkbench.voicePackDir
    if (!dir.exists()) return emptyList()
    return dir.listFiles { f -> f.isFile }?.filter {
        val n = it.name.lowercase()
        n.endsWith(".mp3") || n.endsWith(".amr") || n.endsWith(".silk")
    }?.sortedBy { it.name } ?: emptyList()
}

private fun previewMp3(
    text: String,
    voice: String,
    rate: String,
    volume: String,
    pitch: String,
    onReady: (String) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        val mp3Path = KnownPaths.moduleCache / "wb_preview.mp3"
        mp3Path.deleteIfExists()
        EdgeTtsClient.synthesizeToMp3(text, mp3Path, voice = voice, rate = rate, volume = volume, pitch = pitch)
            .onSuccess { p ->
                withContext(Dispatchers.Main) { onReady(p.absolutePathString()) }
            }
            .onFailure {
                WeLogger.e(TAG, "preview synth failed", it)
                showToastSuspend("试听合成失败: ${it.message}")
            }
    }
}

private fun previewLocalFile(f: File) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            MediaPlayer().apply {
                setDataSource(f.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            showToast("无法播放: ${e.message}")
        }
    }
}

private fun sendVoiceFile(f: File, currentConv: String) {
    CoroutineScope(Dispatchers.IO).launch {
        showToastSuspend("正在发送 ${f.name}...")
        val durationMs = AudioUtils.getDurationMs(f.absolutePath).toInt()
        if (durationMs <= 0) {
            showToastSuspend("发送失败: 无法获取语音时长")
            return@launch
        }

        val isSilk = f.name.lowercase().endsWith(".silk") ||
                f.name.lowercase().endsWith(".amr")
        var success: Boolean
        if (isSilk) {
            success = WeMessageApi.sendVoice(currentConv, f.absolutePath, durationMs)
        } else {
            val silkPath = KnownPaths.moduleCache / "wb_conv_tmp"
            val conv = AudioUtils.mp3ToSilk(f.absolutePath, silkPath.absolutePathString())
            success = if (conv) {
                WeMessageApi.sendVoice(currentConv, silkPath.absolutePathString(), durationMs)
            } else {
                showToastSuspend("MP3 转 SILK 失败")
                return@launch
            }
            silkPath.deleteIfExists()
        }

        showToastSuspend("语音发送${if (success) "成功" else "失败!"}")
    }
}

private fun synthesizeAndSendVoice(
    text: String,
    voice: String,
    rate: String,
    volume: String,
    pitch: String,
) {
    CoroutineScope(Dispatchers.IO).launch {
        showToastSuspend("正在合成语音...")
        val mp3Path = KnownPaths.moduleCache / "wb_tts.mp3"
        val silkPath = KnownPaths.moduleCache / "wb_tts_conv.mp3"
        mp3Path.deleteIfExists()
        silkPath.deleteIfExists()

        EdgeTtsClient.synthesizeToMp3(text, mp3Path, voice = voice, rate = rate, volume = volume, pitch = pitch)
            .onFailure {
                WeLogger.e(TAG, "synthesize failed", it)
                showToastSuspend("语音合成失败: ${it.message}")
                return@launch
            }

        val durationMs = AudioUtils.getDurationMs(mp3Path.absolutePathString())
        showToastSuspend("合成成功, 正在转换并发送...")

        val conv = AudioUtils.mp3ToSilk(mp3Path.absolutePathString(), silkPath.absolutePathString())
        if (!conv) {
            showToastSuspend("MP3 转 SILK 失败")
            mp3Path.deleteIfExists()
            return@launch
        }

        val currentConv = WeCurrentConversationApi.value
        val success = if (currentConv.isBlank()) {
            showToastSuspend("请先打开一个聊天")
            false
        } else {
            WeMessageApi.sendVoice(currentConv, silkPath.absolutePathString(), durationMs.coerceToInt())
        }

        showToastSuspend("语音发送${if (success) "成功" else "失败!"}")
        mp3Path.deleteIfExists()
        silkPath.deleteIfExists()
    }
}
