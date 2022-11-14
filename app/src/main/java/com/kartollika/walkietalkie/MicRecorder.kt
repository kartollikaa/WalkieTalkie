package com.kartollika.walkietalkie

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import android.os.Process
import android.util.Log
import com.kartollika.walkietalkie.bluetooth.BluetoothAction
import com.kartollika.walkietalkie.bluetooth.BluetoothActionsDataSource
import com.kartollika.walkietalkie.bluetooth.SocketHolder.socket
import java.io.IOException

class MicRecorder(private val dataSource: BluetoothActionsDataSource) : Runnable {

  var keepRecording = false

  override fun run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
    var bufferSize = AudioRecord.getMinBufferSize(
      SAMPLE_RATE,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT
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
        AudioFormat.ENCODING_PCM_16BIT,
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
        val writeToOutputStream = Runnable {
          try {
            outputStream.write(audioBuffer)
            outputStream.flush()
          } catch (e: IOException) {
            dataSource.sendAction(BluetoothAction.Error(e, "Output stream closed"))
            e.printStackTrace()
            keepRecording = false
          }
        }
        val thread = Thread(writeToOutputStream)
        thread.start()
      }
      record.stop()
      record.release()
      Log.e("AUDIO", "Streaming stopped")
    } catch (e: IOException) {
      dataSource.sendAction(BluetoothAction.Error(e, "Output stream closed"))
      e.printStackTrace()
    }
  }

  companion object {
    private const val SAMPLE_RATE = 16000
  }
}