package com.kartollika.walkietalkie

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioManager
import android.media.AudioTrack
import android.os.IBinder
import java.io.IOException
import java.io.InputStream

class AudioPlayService : Service() {

  private var audioTrack: AudioTrack? = null
  private var keepPlaying = true

  override fun onBind(intent: Intent): IBinder {
    error("Not supported")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startStreaming()
    return super.onStartCommand(intent, flags, startId)
  }

  private fun startStreaming() {
    val audioStreamingRunnable = Runnable {
      var bufferSize = AudioTrack.getMinBufferSize(16000, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT)
      if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
        bufferSize = 16000 * 2;
      }

      val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build()
        )
        .setBufferSizeInBytes(bufferSize)
        .setAudioFormat(
          AudioFormat.Builder()
            .setSampleRate(16000)
            .setChannelMask(CHANNEL_OUT_MONO)
            .setEncoding(ENCODING_PCM_16BIT)
            .build()
        )
        .build()
      this.audioTrack = audioTrack

      audioTrack.play()

      val buffer = ByteArray(bufferSize)

      try {
        val socket = SocketHolder.socket
        if (socket != null) {
          val inputStream: InputStream = socket.inputStream ?: return@Runnable
          var readBytes: Int = inputStream.read(buffer, 0, bufferSize)

          while (keepPlaying && readBytes != 1) {
            audioTrack.write(buffer, 0, buffer.size)
            readBytes = inputStream.read(buffer, 0, bufferSize)
          }
          inputStream.close()
        }

        audioTrack.release()
      } catch (e: IOException) {
        e.printStackTrace()
      } catch (e: java.lang.NullPointerException) {
        e.printStackTrace()
      }
    }
    Thread(audioStreamingRunnable).start()
  }

  override fun onDestroy() {
    super.onDestroy()
    keepPlaying = false
    if(audioTrack != null) {
      audioTrack?.release()
    }
  }
}