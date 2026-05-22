package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import java.io.File

class AudioRecorderPlayer(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordedFile: File? = null

    fun startRecording(): String? {
        return try {
            val fileName = "nota_voz_${System.currentTimeMillis()}.m4a"
            val file = File(context.filesDir, fileName)
            currentRecordedFile = file

            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            Log.d("AudioRecorderPlayer", "Started recording at: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e("AudioRecorderPlayer", "Failed to start recording", e)
            null
        }
    }

    fun stopRecording(): Int {
        var durationSecs = 0
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            // Estimate duration based on file length or capture metadata
            currentRecordedFile?.let { file ->
                if (file.exists()) {
                    val size = file.length()
                    // 1 second of m4a is roughly 8KB to 16KB depending on sample rate
                    durationSecs = (size / 16000).toInt().coerceAtLeast(1)
                }
            }
            Log.d("AudioRecorderPlayer", "Stopped recording. Estimated duration: $durationSecs seconds")
        } catch (e: Exception) {
            Log.e("AudioRecorderPlayer", "Error stopping recorder", e)
            mediaRecorder = null
        }
        return durationSecs
    }

    fun pauseRecording() {
        try {
            mediaRecorder?.pause()
            Log.d("AudioRecorderPlayer", "Recording paused")
        } catch (e: Exception) {
            Log.e("AudioRecorderPlayer", "Error pausing recorder", e)
        }
    }

    fun resumeRecording() {
        try {
            mediaRecorder?.resume()
            Log.d("AudioRecorderPlayer", "Recording resumed")
        } catch (e: Exception) {
            Log.e("AudioRecorderPlayer", "Error resuming recorder", e)
        }
    }

    fun startPlaying(filePath: String, onCompletion: () -> Unit) {
        stopPlaying()
        try {
            val player = MediaPlayer()
            player.setDataSource(filePath)
            player.prepare()
            player.start()
            player.setOnCompletionListener {
                onCompletion()
                stopPlaying()
            }
            mediaPlayer = player
            Log.d("AudioRecorderPlayer", "Playing audio: $filePath")
        } catch (e: Exception) {
            Log.e("AudioRecorderPlayer", "Failed to play audio", e)
            onCompletion()
        }
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    fun stopPlaying() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            Log.d("AudioRecorderPlayer", "Stopped playback")
        } catch (e: Exception) {
            Log.e("AudioRecorderPlayer", "Error stopping playback", e)
            mediaPlayer = null
        }
    }
}
