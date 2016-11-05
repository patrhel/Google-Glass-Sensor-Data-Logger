package com.example.dataloggerglass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

public class LogFileWriter {

	private BufferedWriter mBufferedWriter = null;
	private File mFile;
	private FileWriter mFileWriter = null;
	private final int BUFFER_SIZE = 4000;
	private String fileName;

	public LogFileWriter(String filename) {
		this.mFile = new File(filename);
		this.fileName = filename;
		if (mFile.exists()) {
			mFile.delete();
		}
		try {
			mFile.createNewFile();
			mFileWriter = new FileWriter(mFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mBufferedWriter = new BufferedWriter(mFileWriter, BUFFER_SIZE);
	}

	public void writeACCdata(Long timestamp, int accuracy, float xVal, float yVal, float zVal) {
		String logString = timestamp + "\t" + accuracy + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeRotationVectorData(Long timestamp, int accuracy, float xVal, float yVal, float zVal) {
		String logString = timestamp + "\t" + accuracy + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeQuaternionData(Long timestamp, int accuracy, float wVal, float xVal, float yVal, float zVal) {
		String logString = timestamp + "\t" + accuracy + "\t" + wVal + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeGyroscopeData(Long timestamp, int accuracy, float xVal, float yVal, float zVal) {
		String logString = timestamp + "\t" + accuracy + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeMagneticSensorData(Long timestamp, int accuracy, float xVal, float yVal, float zVal) {
		String logString = timestamp + "\t" + accuracy + "\t" + xVal + "\t" + yVal + "\t" + zVal + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeBluetoothData(Long timestamp, ArrayList<String> btDevices) {
		String logString = timestamp.toString();
		for (String device : btDevices) {
			logString = logString + "\t" + device;
		}
		logString = logString + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeLightSensorData(Long timestamp, int accuracy, float value) {
		String logString = timestamp + "\t" + accuracy + "\t" + value + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeIRSensorData(Long timestamp, float value) {
		String logString = timestamp + "\t" + value + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeBeaconData(Long timestamp, String uuid, double distance, int rssi) {
		String logString = timestamp + "\t" + uuid + "\t" + String.valueOf(distance) + "\t" + String.valueOf(rssi) + "\n";
		this.writeString(logString);
		logString = null;
	}

	public void writeLabel(String labelName) {
		Long timestamp = System.currentTimeMillis();
		String logString = timestamp + "\t" + labelName;
		logString = logString + "\n";
		writeString(logString);
		logString = null;
	}

	public void writeString(String logdata) {
		if (mBufferedWriter != null) {
			try {
				mBufferedWriter.write(logdata);
			} catch (IOException e) {
				Log.e("LogFileWriter", "Error while writing.");
				e.printStackTrace();

			}
		}
	}

	public void discreteFlush() {
		try {
			mBufferedWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void closeWriter() {
		Log.v("Log file writer", "Finished writing to " + fileName);
		try {
			mBufferedWriter.flush();
			mBufferedWriter.close();
			mBufferedWriter = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
