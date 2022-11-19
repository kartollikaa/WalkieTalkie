package com.kartollika.walkietalkie.audio.mic

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.os.Process
import android.util.Log
import com.kartollika.walkietalkie.bluetooth.BluetoothAction
import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSource
import com.kartollika.walkietalkie.bluetooth.SocketHolder.socket

class MicRecorder(private val dataSource: BluetoothActionsDataSource) : Runnable {

  var keepRecording = false

  override fun run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
    var bufferSize = AudioRecord.getMinBufferSize(
      SAMPLE_RATE,
      AudioFormat.CHANNEL_IN_MONO,
      ENCODING
    )
    Log.e("AUDIO", "buffersize = $bufferSize")
    if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
      bufferSize = SAMPLE_RATE * 2
    }
    try {
      val outputStream = socket!!.outputStream
      val audioBuffer = ByteArray(bufferSize)

      @SuppressLint("MissingPermission")

      val record = AudioRecord(
        AudioSource.MIC,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        ENCODING,
        bufferSize
      )
      if (record.state != AudioRecord.STATE_INITIALIZED) {
        Log.e("AUDIO", "Audio Record can't initialize!")
        return
      }

      record.startRecording()
      Log.e("AUDIO", "STARTED RECORDING")
      while (keepRecording) {
        val numberOfBytes = record.read(audioBuffer, 0, audioBuffer.size)

        if (numberOfBytes >= 0 && numberOfBytes <= audioBuffer.size) {
          outputStream.write(audioBuffer)
          outputStream.flush()
        } else {
          Log.w("AUDIO", "Unexpected length returned: $numberOfBytes");
        }
      }

      record.stop()
      record.release()
      Log.e("AUDIO", "Streaming stopped")
    } catch (e: Exception) {
      dataSource.sendAction(BluetoothAction.Error(e, "Output stream closed"))
      e.printStackTrace()
      keepRecording = false
    }
  }

  companion object {
    private const val SAMPLE_RATE = 8000
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
  }
}