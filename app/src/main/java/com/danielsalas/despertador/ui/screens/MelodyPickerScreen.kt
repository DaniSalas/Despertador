package com.danielsalas.despertador.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.danielsalas.despertador.R

data class AudioItem(val name: String, val path: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MelodyPickerScreen(
    onMelodySelected: (name: String, path: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    
    var systemSounds by remember { mutableStateOf<List<AudioItem>>(emptyList()) }
    var localSounds by remember { mutableStateOf<List<AudioItem>>(emptyList()) }

    var previewingPath by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(Unit) {
        systemSounds = getSystemRingtones(context)
        localSounds = getLocalAudioFiles(context)
    }

    // Release player when screen is closed
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun playPreview(path: String) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(path))
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_explorer_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        mediaPlayer?.stop()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.system_sounds)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.local_files)) }
                )
            }

            val currentList = if (selectedTab == 0) systemSounds else localSounds

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(currentList) { audio ->
                    val isPreviewing = previewingPath == audio.path
                    ListItem(
                        headlineContent = { Text(audio.name) },
                        leadingContent = { 
                            Icon(
                                imageVector = if (isPreviewing) Icons.Filled.PlayArrow else Icons.AutoMirrored.Filled.List, 
                                contentDescription = null,
                                tint = if (isPreviewing) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isPreviewing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
                            .clickable {
                                if (isPreviewing) {
                                    mediaPlayer?.stop()
                                    onMelodySelected(audio.name, audio.path)
                                } else {
                                    previewingPath = audio.path
                                    playPreview(audio.path)
                                }
                            }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun getSystemRingtones(context: Context): List<AudioItem> {
    val list = mutableListOf<AudioItem>()
    try {
        val manager = RingtoneManager(context).apply {
            setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
        }
        val cursor = manager.cursor
        var position = 0
        while (cursor != null && cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(position)
            if (uri != null) {
                list.add(AudioItem(title ?: "Sound $position", uri.toString()))
            }
            position++
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

@SuppressLint("InlinedApi")
private fun getLocalAudioFiles(context: Context): List<AudioItem> {
    val list = mutableListOf<AudioItem>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media._ID)
    
    try {
        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val id = it.getLong(idColumn)
                val fileUri = Uri.withAppendedPath(uri, id.toString())
                list.add(AudioItem(name ?: "Audio $id", fileUri.toString()))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}
