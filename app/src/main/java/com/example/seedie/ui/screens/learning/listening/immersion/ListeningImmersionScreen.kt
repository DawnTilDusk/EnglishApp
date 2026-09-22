package com.example.seedie.ui.screens.learning.listening.immersion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.seedie.data.remote.SupabaseListeningMaterial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningImmersionRoute(
    onNavigateBack: () -> Unit,
    viewModel: ListeningImmersionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var grade by rememberSaveable { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.pause()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.pause()
        }
    }
    fun back() {
        if (showPlayer) showPlayer = false else { viewModel.pause(); onNavigateBack() }
    }
    BackHandler(enabled = showPlayer) { showPlayer = false }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (showPlayer) "正在听" else "磨耳朵") }, navigationIcon = {
                IconButton(onClick = ::back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            })
        },
        bottomBar = {
            if (showPlayer && state.current != null) {
                ImmersionPlayerControls(state, viewModel)
            } else if (state.current != null && state.hasHistory) {
                Surface(tonalElevation = 3.dp) {
                    Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showPlayer = true }, modifier = Modifier.weight(1f)) {
                            Text(state.current!!.title_zh ?: state.current!!.title.orEmpty())
                        }
                        IconButton(onClick = viewModel::togglePlayback) {
                            Icon(if (state.isPlaying || state.buffering) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (state.isPlaying || state.buffering) "暂停" else "继续播放")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError != null || state.materials.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.loadError ?: "暂时没有可收听的内容")
                Button(onClick = viewModel::reload) { Text("重新加载") }
            }
            showPlayer && state.current != null -> ImmersionArticle(state.current!!,
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("每天听一点", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("从熟悉的话题开始，让英语融入日常。", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.hasHistory && state.current != null) {
                    item {
                        Card(onClick = { viewModel.open(state.currentId!!); showPlayer = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("继续上次收听", style = MaterialTheme.typography.labelLarge)
                                Text(state.current!!.title_zh ?: state.current!!.title.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium)
                                Text("听到 ${audioTime(state.positionMs)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 7, 8, 9).forEach { value ->
                            FilterChip(selected = grade == value, onClick = { grade = value },
                                label = { Text(if (value == 0) "全部" else "${value}年级") })
                        }
                    }
                }
                val filtered = state.materials.filter { grade == 0 || materialGrade(it) == grade }
                if (filtered.isEmpty()) item { Text("这个年级暂时没有内容，试试其他分类。") }
                items(filtered, key = { it.material_id }) { material ->
                    Card(onClick = { viewModel.open(material.material_id); showPlayer = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(material.title_zh ?: material.title.orEmpty(),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (!material.title.isNullOrBlank()) Text(material.title,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("${gradeLabel(material)} · ${audioTime(material.estimated_seconds * 1000L)} · ${sourceLabel(material)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.Headphones, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImmersionArticle(material: SupabaseListeningMaterial, modifier: Modifier = Modifier) {
    var showEnglish by rememberSaveable { mutableStateOf(false) }
    var showChinese by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    LaunchedEffect(material.material_id) { scroll.scrollTo(0) }
    Column(modifier.verticalScroll(scroll).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(material.title_zh ?: material.title.orEmpty(), style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text(material.title.orEmpty(), style = MaterialTheme.typography.titleMedium)
        Text("${gradeLabel(material)} · ${sourceLabel(material)}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = showEnglish, onClick = { showEnglish = !showEnglish }, label = { Text("英文原文") })
            if (!material.transcript_zh.isNullOrBlank()) {
                FilterChip(selected = showChinese, onClick = { showChinese = !showChinese }, label = { Text("中文翻译") })
            }
        }
        if (!showEnglish && (!showChinese || material.transcript_zh.isNullOrBlank())) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .4f)) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Headphones, contentDescription = null,
                        modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("先听一听，再打开原文", style = MaterialTheme.typography.titleMedium)
                    Text("没听清时，可以后退 10 秒或放慢速度。", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (showEnglish) {
            Text("英文原文", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            material.transcript.orEmpty().split(Regex("""\n\s*\n""")).filter { it.isNotBlank() }.forEach {
                Text(it.trim(), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (showChinese && !material.transcript_zh.isNullOrBlank()) {
            Text("中文翻译", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Text(material.transcript_zh, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ImmersionPlayerControls(state: ImmersionUiState, viewModel: ListeningImmersionViewModel) {
    var dragging by remember(state.currentId) { mutableStateOf(false) }
    var seekPosition by remember(state.currentId) { mutableFloatStateOf(0f) }
    val index = state.materials.indexOfFirst { it.material_id == state.currentId }
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (state.playbackError != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.playbackError, modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { state.currentId?.let(viewModel::open) }) { Text("重试") }
                }
            } else if (state.buffering) {
                Text("正在缓冲…", style = MaterialTheme.typography.bodySmall)
            }
            Slider(
                value = if (dragging) seekPosition else state.positionMs.toFloat().coerceIn(0f, state.durationMs.coerceAtLeast(1).toFloat()),
                onValueChange = { dragging = true; seekPosition = it },
                onValueChangeFinished = { viewModel.seek(seekPosition.toLong()); dragging = false },
                valueRange = 0f..state.durationMs.coerceAtLeast(1).toFloat(), enabled = state.durationMs > 0
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(audioTime(if (dragging) seekPosition.toLong() else state.positionMs), style = MaterialTheme.typography.labelSmall)
                Text(audioTime(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::previous, enabled = index > 0) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一篇")
                }
                IconButton(onClick = { viewModel.skip(-10) }, enabled = state.durationMs > 0) {
                    Icon(Icons.Default.Replay10, contentDescription = "后退 10 秒")
                }
                FilledIconButton(onClick = viewModel::togglePlayback, modifier = Modifier.size(56.dp)) {
                    Icon(if (state.isPlaying || state.buffering) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying || state.buffering) "暂停" else "播放")
                }
                IconButton(onClick = { viewModel.skip(10) }, enabled = state.durationMs > 0) {
                    Icon(Icons.Default.Forward10, contentDescription = "前进 10 秒")
                }
                IconButton(onClick = viewModel::next, enabled = index >= 0 && index < state.materials.lastIndex) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一篇")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = viewModel::cycleSpeed) { Text("${state.speed}× 倍速") }
                TextButton(onClick = viewModel::toggleRepeat) { Text(if (state.repeatOne) "单篇循环" else "顺序播放") }
            }
        }
    }
}

private fun materialGrade(material: SupabaseListeningMaterial): Int? =
    material.grade_level?.takeIf { it in 13..18 }?.let { (it + 1) / 2 }

private fun gradeLabel(material: SupabaseListeningMaterial) = materialGrade(material)?.let { "${it}年级" } ?: "英语听读"
private fun sourceLabel(material: SupabaseListeningMaterial) = if (material.material_id.startsWith("original-")) "原创短文" else "听力短文"
internal fun audioTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
