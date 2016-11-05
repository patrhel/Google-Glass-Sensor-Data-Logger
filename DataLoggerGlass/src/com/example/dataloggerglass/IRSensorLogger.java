package com.example.dataloggerglass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.util.Log;

public class IRSensorLogger {

	// hint: only works if on-head detection is on (XE18)

	public float getIRSensorData() {
		try {
			Process process = Runtime.getRuntime().exec("cat /sys/bus/i2c/devices/4-0035/proxraw");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			int read;
			char[] buffer = new char[8];
			StringBuffer output = new StringBuffer();
			while ((read = reader.read(buffer)) > 0) {
				output.append(buffer, 0, read);
			}
			reader.close();
			float value = Float.valueOf(output.toString());
			process = null;
			reader = null;
			buffer = null;
			output = null;
			return value;
		} catch (IOException e) {
			// permission error
			Log.v("IRSensor", "Permission error!");
			return -1.0f;
		}
	}

	public boolean isInstallationFinished() {
		Float res = this.getIRSensorData();
		if (res == -1.0f) {
			return false;
		}
		return true;
	}
}
