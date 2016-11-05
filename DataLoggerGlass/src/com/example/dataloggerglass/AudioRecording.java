package com.example.dataloggerglass;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.app.IntentService;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecording extends IntentService {

	private String mLogSessionDirectoryPath;

	public AudioRecording() {
		super("");
	}

	public AudioRecording(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (intent != null) {
			mLogSessionDirectoryPath = intent.getStringExtra("logSessionDirectoryPath");
		}

		try {
			File file = new File(mLogSessionDirectoryPath, "audio.pcm");
			try {
				file.createNewFile();
				OutputStream outputStream = new FileOutputStream(file);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
				DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);

				int minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT);
				short[] audioData = new short[minBufferSize];
				AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
				audioRecord.startRecording();
				while (LoggerService.isLogging) {
					Log.v("AudioRecording", "Audio is being captured.");
					int numberOfShort = audioRecord.read(audioData, 0, minBufferSize);
					for (int i = 0; i < numberOfShort; i++)
						dataOutputStream.writeShort(audioData[i]);
				}
				audioRecord.stop();
				dataOutputStream.close();
			} catch (IOException e) {
				Log.e("AudioRecording", "Error while saving audio capture.");
			}

		} catch (Exception e) {
			Log.v("AUDIO", "Error while audio capturing.");
		}

	}

}
